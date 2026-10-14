/*
 * Copyright (c) 2026, dan <dan@tate.nz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nz.tate.bankstanding;

import com.google.inject.Provides;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import nz.tate.bankstanding.core.BankNames;
import nz.tate.bankstanding.core.BankRecord;
import nz.tate.bankstanding.core.CollectionLog;
import nz.tate.bankstanding.core.DailyStreak;
import nz.tate.bankstanding.core.Emote;
import nz.tate.bankstanding.core.Experience;
import nz.tate.bankstanding.core.Keywords;
import nz.tate.bankstanding.core.Modifiers;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.Session;
import nz.tate.bankstanding.core.StandContext;
import nz.tate.bankstanding.core.StandTracker;
import nz.tate.bankstanding.core.Unlocks;
import nz.tate.bankstanding.core.StatsView;
import nz.tate.bankstanding.core.XpRoller;

/**
 * Wires the plugin together: each game tick it asks the stand tracker whether a drop is
 * due, rolls it, and saves the result against the account and the bank.
 */
@Slf4j
@PluginDescriptor(
	name = "Bank Standing",
	description = "Train the Bank Standing skill by idling near a bank, all the way to 99",
	tags = {"bank", "idle", "skill", "meme", "afk"}
)
public class BankStandingPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private BankLocator bankLocator;

	@Inject
	private ProgressStore store;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DropFeed dropFeed;

	@Inject
	private SkillTileOverlay skillTileOverlay;

	@Inject
	private SkillGuideOverlay skillGuideOverlay;

	@Inject
	private CollectionLogOverlay collectionLogOverlay;

	@Inject
	private CollectionPopupOverlay collectionPopup;

	@Inject
	private StandCosmetics cosmetics;

	@Inject
	private ConfigManager configManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private XpDropOverlay xpDropOverlay;

	@Inject
	private HitsplatOverlay hitsplatOverlay;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Icons icons;

	@Inject
	private ItemManager itemManager;

	@Inject
	private BankStandingConfig config;

	@Inject
	private CapeDisplay capeDisplay;

	@Inject
	private Celebration celebration;

	@Inject
	private DevOptions dev;

	@Inject
	private ClientThread clientThread;

	private BankStandingPanel panel;
	private NavigationButton navButton;
	private NavigationButton devButton;

	private final StandTracker tracker = new StandTracker();
	private Session session = new Session();
	private final XpRoller roller = new XpRoller(new Random());
	private final Random collectionRandom = new Random();

	/** Our own slot in the player's spot animations, so the fireworks never replace the game's. */
	private static final int FIREWORKS_KEY = 0x42414E4B;
	/** Client-wide config key: when the plugin first started, in epoch milliseconds. */
	private static final String INSTALLED_KEY = "installedAt";

	/** Unsaved changes are written at most this often (a minute), and on logout. */
	private static final int SAVE_EVERY_TICKS = 100;
	private boolean dirty;
	private int ticksSinceSave;

	private boolean devOverridesApplied;
	private int devDrops;
	private long devNextCelebration;

	private final Map<Skill, Integer> realXp = new EnumMap<>(Skill.class);
	private final Keywords keywords = new Keywords(Keywords.PHRASES);

	/** An emote's effect lasts this long after it starts. */
	private static final int EMOTE_TICKS = 100;

	private Set<Integer> worn = Collections.emptySet();
	private long wornValue;
	private int wornCape = -1;
	private Emote emote = Emote.NONE;
	private int emoteUntilTick;

	private Progress progress;

	@Provides
	BankStandingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankStandingConfig.class);
	}

	@Override
	protected void startUp()
	{
		// RuneLite doesn't record when a plugin was installed, so note the first start.
		if (configManager.getConfiguration(BankStandingConfig.GROUP, INSTALLED_KEY) == null)
		{
			configManager.setConfiguration(BankStandingConfig.GROUP, INSTALLED_KEY, System.currentTimeMillis());
		}
		tracker.reset();
		overlayManager.add(skillTileOverlay);
		for (ParchmentWindow window : windows())
		{
			overlayManager.add(window);
			mouseManager.registerMouseListener(window.mouse);
			mouseManager.registerMouseWheelListener(window.wheel);
		}
		skillGuideOverlay.setOnOpen(collectionLogOverlay::close);
		collectionLogOverlay.setOnOpen(skillGuideOverlay::close);
		overlayManager.add(collectionPopup);
		skillTileOverlay.addMenuEntry(MenuAction.RUNELITE_OVERLAY, "View", "Collection log", e -> openCollectionLog());
		overlayManager.add(xpDropOverlay);
		overlayManager.add(hitsplatOverlay);

		panel = new BankStandingPanel(icons, this::capeEmote, this::openCollectionLog);
		navButton = NavigationButton.builder()
			.tooltip("Bank Standing")
			.icon(icons.small())
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		if (dev.developerMode)
		{
			devButton = NavigationButton.builder()
				.tooltip("Bank Standing (dev)")
				.icon(icons.small())
				.priority(11)
				.panel(new DevPanel(this, dev))
				.build();
			clientToolbar.addNavigation(devButton);
		}

		clientThread.invoke(() ->
		{
			ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
			if (equipment != null)
			{
				readEquipment(equipment);
			}
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				bankLocator.scan(client.getTopLevelWorldView().getScene());
			}
		});
	}

	@Override
	protected void shutDown()
	{
		save();
		int realCape = wornCape;
		clientThread.invoke(() ->
		{
			capeDisplay.restore(realCape);
			cosmetics.clear();
		});
		overlayManager.remove(skillTileOverlay);
		for (ParchmentWindow window : windows())
		{
			mouseManager.unregisterMouseListener(window.mouse);
			mouseManager.unregisterMouseWheelListener(window.wheel);
			overlayManager.remove(window);
			window.close();
		}
		skillTileOverlay.removeMenuEntry(MenuAction.RUNELITE_OVERLAY, "View", "Collection log");
		overlayManager.remove(collectionPopup);
		collectionPopup.clear();
		overlayManager.remove(xpDropOverlay);
		overlayManager.remove(hitsplatOverlay);
		dropFeed.clear();
		celebration.stop();
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		if (devButton != null)
		{
			clientToolbar.removeNavigation(devButton);
			devButton = null;
		}
		panel = null;
		bankLocator.clear();
		realXp.clear();
		progress = null;
		worn = Collections.emptySet();
		wornValue = 0;
		wornCape = -1;
		emote = Emote.NONE;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOADING)
		{
			bankLocator.clear();
			cosmetics.clear();
		}
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			save();
			tracker.reset();
			realXp.clear();
			progress = null;
			capeDisplay.forget();
			cosmetics.clear();
			BankStandingPanel shown = panel;
			SwingUtilities.invokeLater(shown::showLoggedOut);
			for (ParchmentWindow window : windows())
			{
				window.close();
			}
			collectionPopup.clear();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		bankLocator.objectSpawned(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		bankLocator.objectDespawned(event.getGameObject());
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Integer previous = realXp.put(event.getSkill(), event.getXp());
		if (previous != null && event.getXp() > previous)
		{
			tracker.realXpGained();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.WORN)
		{
			return;
		}
		readEquipment(event.getItemContainer());
	}

	private void readEquipment(ItemContainer container)
	{
		Set<Integer> ids = new HashSet<>();
		long value = 0;
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				ids.add(item.getId());
				value += (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
			}
		}
		worn = ids;
		wornValue = value;
		Item cape = container.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
		wornCape = cape == null ? -1 : cape.getId();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (BankStandingConfig.GROUP.equals(event.getGroup()) && "showCape".equals(event.getKey()) && !config.showCape())
		{
			clientThread.invoke(() -> capeDisplay.restore(wornCape));
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		int animation = event.getActor().getAnimation();
		if (cosmetics.isOwnAnimation(animation))
		{
			return;
		}
		Emote started = Emote.fromAnimation(animation);
		if (started != Emote.NONE)
		{
			emote = started;
			emoteUntilTick = client.getTickCount() + EMOTE_TICKS;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		Player player = client.getLocalPlayer();
		if (event.getType() != ChatMessageType.PUBLICCHAT || player == null || progress == null
			|| !tracker.isStanding() || player.getName() == null
			|| !Text.sanitize(event.getName()).equals(Text.sanitize(player.getName())))
		{
			return;
		}

		WorldPoint location = player.getWorldLocation();
		String bankKey = BankNames.keyFor(location.getX(), location.getY());
		Keywords.Kind phrase = keywords.claim(progress, Text.removeTags(event.getMessage()),
			System.currentTimeMillis(), today(), bankKey, worn);
		if (phrase != null)
		{
			BankRecord bank = progress.bank(bankKey, location.getX(), location.getY());
			int level = progress.level();
			Modifiers.Effect effect = Modifiers.evaluate(context(bank, location));
			drop(roller.keywordRoll(level, effect.bonusHit), bank, level, phrase);
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (dev.celebrateEvery > 0 && !celebration.isRunning() && client.getLocalPlayer() != null
			&& System.currentTimeMillis() >= devNextCelebration)
		{
			devNextCelebration = System.currentTimeMillis() + dev.celebrateEvery * 1000L;
			celebration.start();
		}
		celebration.tick();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		if (progress == null)
		{
			progress = store.load();
			if (progress == null)
			{
				return;
			}
			progress.renameBanks();
			applyDevOverrides();
			progress.records.begin(progress, System.currentTimeMillis());
			session = new Session();
			refreshPanel();
		}

		WorldPoint location = player.getWorldLocation();
		int currentLevel = progress.level();
		tracker.setUnlocks(Unlocks.startDelayTicks(currentLevel), Unlocks.peekTicks(currentLevel));
		boolean roll = tracker.tick(location.getX(), location.getY(), location.getPlane(),
			bankLocator.isNearBank(location, Unlocks.bankRange(currentLevel)), isUsingBank());

		// The cape and the other unlocks only show while bank standing; the dev options show
		// them anywhere.
		boolean standing = tracker.isStanding() || dev.pretendStanding;
		boolean pet = config.showPet() && (CollectionLog.hasPet(progress) || dev.previewPet);
		cosmetics.tick(standing, currentLevel, config, pet);
		if (dev.previewCape
			|| config.showCape() && currentLevel >= Experience.MAX_LEVEL && standing)
		{
			capeDisplay.apply();
		}
		else
		{
			capeDisplay.restore(wornCape);
		}

		if (!tracker.isStanding())
		{
			progress.records.standEnded();
			return;
		}

		String bankKey = BankNames.keyFor(location.getX(), location.getY());
		boolean newBank = !progress.banks.containsKey(bankKey) && BankNames.find(location.getX(), location.getY()) != null;
		BankRecord bank = progress.bank(bankKey, location.getX(), location.getY());
		if (newBank)
		{
			collected("New bank added to your collection log: ", BankNames.displayName(bankKey, bank), -1, false);
		}
		bank.ticks++;
		session.ticks++;
		dirty = true;
		if (++ticksSinceSave >= SAVE_EVERY_TICKS)
		{
			save();
		}
		DailyStreak.standingTick(progress, today());
		progress.records.standingTick(progress, session, System.currentTimeMillis());

		if (roll)
		{
			int level = progress.level();
			Modifiers.Effect effect = Modifiers.evaluate(context(bank, location));
			drop(roller.roll(level, effect.boost, effect.bonusHit), bank, level, null);
			progress.rolls++;
			CollectionLog.Drop found = CollectionLog.roll(collectionRandom);
			if (found != null)
			{
				obtain(found);
			}
		}
	}

	private ParchmentWindow[] windows()
	{
		return new ParchmentWindow[]{skillGuideOverlay, collectionLogOverlay};
	}

	/** Opens the collection log, from the side panel or the skill tile's menu. */
	void openCollectionLog()
	{
		collectionLogOverlay.showTab(CollectionLogOverlay.DROPS);
		collectionLogOverlay.open();
	}

	/** Adds a drop to the log: the popup the first time, a chat line every time. Client thread. */
	private void obtain(CollectionLog.Drop drop)
	{
		if (CollectionLog.collect(progress, drop, System.currentTimeMillis()))
		{
			collected("New item added to your collection log: ", drop.name, drop.icon, drop.gold);
		}
		else
		{
			chat("Bank Standing drop: <col=ef1020>" + drop.name + "</col>");
		}
		dirty = true;
		save();
		refreshPanel();
	}

	/** The chat line in the log's red, and the popup. */
	private void collected(String message, String name, int icon, boolean gold)
	{
		chat(message + "<col=ef1020>" + name + "</col>");
		collectionPopup.show((icon > 0 ? "New item: " : "New bank: ") + name, icon, gold);
	}

	private void chat(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	private StandContext context(BankRecord bank, WorldPoint location)
	{
		StandContext context = new StandContext();
		context.worn = worn;
		context.wornValue = wornValue;
		context.hitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
		context.bankKey = BankNames.keyFor(location.getX(), location.getY());
		context.today = today();
		context.emote = client.getTickCount() < emoteUntilTick ? emote : Emote.NONE;
		context.follower = client.getFollower() != null;
		context.streakDays = DailyStreak.current(progress, context.today);
		context.hoursAtBank = bank.hours();
		return context;
	}

	private static LocalDate today()
	{
		return LocalDate.now(ZoneOffset.UTC);
	}

	/**
	 * Awards one rolled hit and shows it: a max hit is exactly the normal max for the level,
	 * and anything above that (only reachable with a gear bonus) is boosted.
	 */
	private void drop(int hit, BankRecord bank, int level, Keywords.Kind phrase)
	{
		int xp = hit * dev.xpMultiplier;
		award(xp, bank, level);
		devDrops++;
		boolean showRare = dev.rareEvery > 0 && devDrops % dev.rareEvery == 0;
		int normalMax = XpRoller.baseMaxHit(level);
		dropFeed.add(xp, normalMax > 1 && hit == normalMax, showRare || hit > normalMax, phrase, previewSprite());
	}

	/** Development only: the next sprite from {@code -PpreviewSplats}, named in chat. */
	private int previewSprite()
	{
		if (dev.previewSplats.length == 0)
		{
			return 0;
		}
		int sprite = dev.previewSplats[devDrops % dev.previewSplats.length];
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage("Splat preview: " + sprite)
			.build());
		return sprite;
	}

	private void award(int drop, BankRecord bank, int levelBefore)
	{
		int before = progress.xp;
		progress.xp = Experience.addXp(progress.xp, drop);
		progress.records.gainedXp(before, progress.xp, System.currentTimeMillis());
		if (dev.xpMultiplier > 1)
		{
			progress.records.edited = true;
		}
		bank.xp += progress.xp - before;
		session.xp += progress.xp - before;
		refreshPanel();

		int levelAfter = progress.level();
		if (levelAfter > levelBefore)
		{
			save();
			announceLevel(levelAfter);
		}
	}

	private void refreshPanel()
	{
		StatsView view = new StatsView(progress, session, today().toString());
		BankStandingPanel shown = panel;
		SwingUtilities.invokeLater(() -> shown.show(view));
	}

	/** Writes progress if anything changed since the last save. */
	private void save()
	{
		ticksSinceSave = 0;
		if (dirty && progress != null)
		{
			store.save(progress);
			dirty = false;
		}
	}

	/** The 99 cape emote, from the side panel. Called on the Swing thread. */
	void capeEmote()
	{
		clientThread.invoke(() ->
		{
			if (progress != null && (progress.level() >= Experience.MAX_LEVEL || dev.developerMode))
			{
				cosmetics.capeEmote();
			}
		});
	}

	// Dev panel actions. Called on the Swing thread; game work is moved to the client thread.

	void devSplat(boolean maxHit, boolean boosted, Keywords.Kind phrase)
	{
		dropFeed.add(maxHit ? 25 : boosted ? 30 : 7, maxHit, boosted, phrase, 0);
	}

	void devSprite(int spriteId)
	{
		dropFeed.add(7, false, false, null, spriteId);
	}

	void devFireworks()
	{
		clientThread.invoke(() ->
		{
			Player player = client.getLocalPlayer();
			if (player != null)
			{
				player.createSpotAnim(FIREWORKS_KEY, SpotanimID.LEVELUP_ANIM, 0, 0);
			}
		});
	}

	void devCelebrate()
	{
		clientThread.invoke(celebration::start);
	}

	void devPreviewCape(boolean on)
	{
		dev.previewCape = on;
	}

	/** Replays the milestone history and says in chat whether it adds up. */
	void devCheckRecords()
	{
		clientThread.invoke(() ->
		{
			if (progress == null)
			{
				return;
			}
			List<String> problems = progress.records.check(progress, System.currentTimeMillis());
			String text = problems.isEmpty()
				? "Records check: consistent (" + progress.records.milestones.size() + " milestones)"
				: "Records check: " + String.join("; ", problems);
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(text)
				.build());
		});
	}

	void devGiveDrop(String key)
	{
		clientThread.invoke(() ->
		{
			CollectionLog.Drop drop = CollectionLog.find(key);
			if (progress != null && drop != null)
			{
				obtain(drop);
			}
		});
	}

	/** Gives the first drop not yet in the log. */
	void devGiveNextDrop()
	{
		clientThread.invoke(() ->
		{
			if (progress == null)
			{
				return;
			}
			for (CollectionLog.Drop drop : CollectionLog.DROPS)
			{
				if (!progress.collection.containsKey(drop.key))
				{
					obtain(drop);
					return;
				}
			}
			chat("Collection log: you have every drop.");
		});
	}

	void devTestPopup()
	{
		clientThread.invoke(() -> collected("New item added to your collection log: ", "Golden booth key", 989, true));
	}

	void devResetCollection()
	{
		clientThread.invoke(() ->
		{
			if (progress != null)
			{
				progress.collection.clear();
				progress.rolls = 0;
				dirty = true;
				save();
				refreshPanel();
			}
		});
	}

	void devPreviewPet(boolean on)
	{
		dev.previewPet = on;
	}

	void devSetLevel(int level)
	{
		devSetXp(Experience.xpForLevel(level));
	}

	void devSetXp(int xp)
	{
		clientThread.invoke(() ->
		{
			if (progress != null)
			{
				progress.xp = xp;
				progress.records.edited = true;
				dirty = true;
				save();
				refreshPanel();
			}
		});
	}

	void devReset()
	{
		clientThread.invoke(() ->
		{
			if (progress != null)
			{
				progress = new Progress();
				progress.records.begin(progress, System.currentTimeMillis());
				session = new Session();
				dirty = true;
				save();
				refreshPanel();
			}
		});
	}

	private void applyDevOverrides()
	{
		if (devOverridesApplied || (!dev.resetProgress && dev.setXp == null))
		{
			return;
		}
		devOverridesApplied = true;
		if (dev.resetProgress)
		{
			progress = new Progress();
		}
		if (dev.setXp != null)
		{
			progress.xp = Math.max(0, Math.min(Experience.MAX_XP, dev.setXp));
			progress.records.edited = true;
		}
		store.save(progress);
	}

	private void announceLevel(int level)
	{
		String message = new ChatMessageBuilder()
			.append("Congratulations, you've just advanced your Bank Standing level. You are now level " + level + ".")
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());

		Player player = client.getLocalPlayer();
		if (level >= Experience.MAX_LEVEL)
		{
			celebration.start();
		}
		else if (player != null)
		{
			player.createSpotAnim(FIREWORKS_KEY, SpotanimID.LEVELUP_ANIM, 0, 0);
		}
	}

	private boolean isUsingBank()
	{
		return isOpen(InterfaceID.BANKMAIN) || isOpen(InterfaceID.BANK_DEPOSITBOX);
	}

	private boolean isOpen(int group)
	{
		Widget root = client.getWidget(group, 0);
		return root != null && !root.isHidden();
	}

	Progress getProgress()
	{
		return progress;
	}
}
