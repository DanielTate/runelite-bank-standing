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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.coords.LocalPoint;
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
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarPlayerID;
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
import nz.tate.bankstanding.core.Companions;
import nz.tate.bankstanding.core.Experience;
import nz.tate.bankstanding.core.GearSets;
import nz.tate.bankstanding.core.Keywords;
import nz.tate.bankstanding.core.Moments;
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
	private final Moments.Watch watch = new Moments.Watch();
	/** Players beside you: x, y and ticks still, by name. */
	private Map<String, long[]> neighbours = new HashMap<>();
	private static final int NEIGHBOUR_TILES = 3;
	private boolean wearingPoison;

	/** An emote's effect lasts this long after it starts. */
	private static final int EMOTE_TICKS = 100;

	private Set<Integer> worn = Collections.emptySet();
	private int[] inventory = new int[0];
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
		for (GameWindow window : windows())
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
			ItemContainer carried = client.getItemContainer(InventoryID.INV);
			if (carried != null)
			{
				readInventory(carried);
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
		for (GameWindow window : windows())
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
		inventory = new int[0];
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
			watch.loggedOut();
			realXp.clear();
			progress = null;
			capeDisplay.forget();
			cosmetics.clear();
			BankStandingPanel shown = panel;
			SwingUtilities.invokeLater(shown::showLoggedOut);
			for (GameWindow window : windows())
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
		if (event.getContainerId() == InventoryID.INV)
		{
			readInventory(event.getItemContainer());
		}
		if (event.getContainerId() != InventoryID.WORN)
		{
			return;
		}
		readEquipment(event.getItemContainer());
	}

	/** Poisoned weapons and ammunition, and the toxic and serpentine items. */
	static boolean isPoisonous(String name)
	{
		String lower = name == null ? "" : name.toLowerCase(Locale.ENGLISH);
		return lower.contains("(p)") || lower.contains("(p+)") || lower.contains("(p++)") || lower.contains("(kp)")
			|| lower.startsWith("toxic ") || lower.startsWith("serpentine helm") || lower.startsWith("tanzanite helm")
			|| lower.startsWith("magma helm");
	}

	private void readInventory(ItemContainer container)
	{
		int[] slots = new int[container.size()];
		for (int i = 0; i < slots.length; i++)
		{
			Item item = container.getItem(i);
			slots[i] = item == null ? -1 : item.getId();
		}
		inventory = slots;
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
		wearingPoison = false;
		for (int id : ids)
		{
			wearingPoison |= isPoisonous(itemManager.getItemComposition(id).getName());
		}
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
		if (animation == AnimationID.EMOTE_SIT && progress != null && tracker.isStanding())
		{
			sitDown((Player) event.getActor());
		}
	}

	/** The Sit emote during a stand: a moment for the log, and a boosted roll once an hour. */
	private void sitDown(Player player)
	{
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		if (Moments.sat(progress, System.currentTimeMillis(), odd))
		{
			WorldPoint location = mapLocation(player);
			String bankKey = BankNames.keyFor(location.getX(), location.getY());
			BankRecord bank = progress.bank(bankKey, location.getX(), location.getY());
			int level = progress.level();
			drop(roller.boostedRoll(level), bank, level, null);
		}
		dirty = true;
		announce(Collections.emptySet(), odd);
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

		WorldPoint location = mapLocation(player);
		String bankKey = BankNames.keyFor(location.getX(), location.getY());
		ClanSettings clan = client.getClanSettings();
		Keywords.Kind phrase = keywords.claim(progress, Text.removeTags(event.getMessage()),
			System.currentTimeMillis(), today(), bankKey, worn, clan == null ? null : clan.getName());
		if (phrase != null)
		{
			BankRecord bank = progress.bank(bankKey, location.getX(), location.getY());
			int level = progress.level();
			Modifiers.Effect effect = evaluate(bank, location);
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
			Moments.backfill(progress, keywords);
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
			watch.standEnded();
			return;
		}

		location = mapLocation(player);
		String bankKey = BankNames.keyFor(location.getX(), location.getY());
		boolean newBank = !progress.banks.containsKey(bankKey) && BankNames.find(location.getX(), location.getY()) != null;
		BankRecord bank = progress.bank(bankKey, location.getX(), location.getY());
		if (newBank)
		{
			collected("New bank: ", BankNames.displayName(bankKey, bank), -1, false);
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
		noteMoments(bankKey, location, bank);

		if (roll)
		{
			int level = progress.level();
			Modifiers.Effect effect = evaluate(bank, location);
			drop(roller.roll(level, effect.boost, effect.bonusHit), bank, level, null);
			progress.rolls++;
			CollectionLog.Drop found = CollectionLog.roll(collectionRandom, today());
			if (found != null)
			{
				obtain(found);
			}
			for (GearSets.Tier tier : GearSets.collect(progress, bankKey, worn, inventory, System.currentTimeMillis()))
			{
				collected("New item: ", tier.name, tier.icon, false);
				dirty = true;
			}
			NPC follower = client.getFollower();
			Companions.Kind companion = Companions.collect(progress,
				follower == null || follower.getName() == null ? null : Text.removeTags(follower.getName()),
				System.currentTimeMillis());
			if (companion != null)
			{
				collected("New companion: ", companion.name, companion.icon, false);
				dirty = true;
			}
		}
	}

	private GameWindow[] windows()
	{
		return new GameWindow[]{skillGuideOverlay, collectionLogOverlay};
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
		if (drop.rare())
		{
			Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
			Moments.rareDrop(progress, watch, System.currentTimeMillis(), odd);
			announce(Collections.emptySet(), odd);
		}
		if (CollectionLog.collect(progress, drop, System.currentTimeMillis()))
		{
			collected("New item: ", drop.name, drop.icon, drop.gold);
		}
		else
		{
			chat("Bank Standing drop: <col=ef1020>" + drop.name + "</col>");
		}
		dirty = true;
		save();
		refreshPanel();
	}

	/**
	 * The chat line and the popup for something new in the log. Both say Bank Standing, so
	 * they can't be mistaken for the game's own collection log.
	 */
	private void collected(String what, String name, int icon, boolean gold)
	{
		chat("Bank Standing collection log - " + what + "<col=ef1020>" + name + "</col>");
		collectionPopup.show(what + name, icon, gold);
	}

	private void chat(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	/**
	 * Where the player is on the real map: inside an instance, like a clan hall, the game's
	 * own coordinates are made up, so the instance's template is used to name the bank.
	 */
	private WorldPoint mapLocation(Player player)
	{
		LocalPoint local = player.getLocalLocation();
		if (local != null && client.getTopLevelWorldView().isInstance())
		{
			return WorldPoint.fromLocalInstance(client, local);
		}
		return player.getWorldLocation();
	}

	/** The modifiers for one roll; anything that counts for the first time goes in the log. */
	private Modifiers.Effect evaluate(BankRecord bank, WorldPoint location)
	{
		Modifiers.Effect effect = Modifiers.evaluate(context(bank, location));
		for (String key : effect.found)
		{
			if (Moments.record(progress, key, System.currentTimeMillis()))
			{
				collected("Dressed for it: ", key.startsWith("diary:") ? "diary item" : key.startsWith("haul:") ? "full inventory" : "outfit", -1, false);
				dirty = true;
			}
		}
		return effect;
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
		context.inventory = inventory;
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
		boolean maxHit = normalMax > 1 && hit == normalMax;
		dropFeed.add(xp, maxHit, showRare || hit > normalMax, phrase, previewSprite());

		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		Moments.Splat splat = Moments.drop(progress, watch, hit, maxHit, hit > normalMax, phrase, normalMax,
			today(), System.currentTimeMillis(), odd);
		if (splat != null)
		{
			collected("New hitsplat: ", splat.title, -1, false);
		}
		announce(Collections.emptySet(), odd);
	}

	/** Checks the holidays and odd moments for one standing tick. */
	private void noteMoments(String bankKey, WorldPoint location, BankRecord bank)
	{
		Moments.Tick tick = new Moments.Tick();
		tick.now = System.currentTimeMillis();
		tick.utcDay = today();
		tick.local = LocalDateTime.now();
		tick.yaw = client.getCameraYaw();
		tick.pitch = client.getCameraPitch();
		tick.prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		tick.namedBank = BankNames.find(location.getX(), location.getY()) != null ? bankKey : null;
		tick.world = client.getWorld();
		tick.standTicks = progress.records.standTicks();
		tick.bankTicks = bank.ticks;
		EnumSet<WorldType> types = client.getWorldType();
		tick.pvpWorld = types.contains(WorldType.PVP);
		tick.skillTotalWorld = types.contains(WorldType.SKILL_TOTAL);
		tick.poisoned = client.getVarpValue(VarPlayerID.POISON) > 0;
		tick.wearingPoison = wearingPoison;
		watchNeighbours(tick);
		Set<Moments.HolidayEntry> holidays = EnumSet.noneOf(Moments.HolidayEntry.class);
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		Moments.standingTick(progress, watch, tick, holidays, odd);
		announce(holidays, odd);
	}

	/**
	 * Counts the other players in the scene, and how long the stillest one standing within a
	 * few tiles has stayed put without animating.
	 */
	private void watchNeighbours(Moments.Tick tick)
	{
		Player me = client.getLocalPlayer();
		WorldPoint here = me.getWorldLocation();
		Map<String, long[]> seen = new HashMap<>();
		int around = 0;
		long stillest = 0;
		for (Player other : client.getTopLevelWorldView().players())
		{
			if (other == null || other == me || other.getName() == null)
			{
				continue;
			}
			around++;
			WorldPoint at = other.getWorldLocation();
			if (at.getPlane() != here.getPlane() || at.distanceTo2D(here) > NEIGHBOUR_TILES)
			{
				continue;
			}
			long[] before = neighbours.get(other.getName());
			boolean still = other.getAnimation() == -1 && before != null && before[0] == at.getX() && before[1] == at.getY();
			long ticks = still ? before[2] + 1 : 0;
			seen.put(other.getName(), new long[]{at.getX(), at.getY(), ticks});
			stillest = Math.max(stillest, ticks);
		}
		neighbours = seen;
		tick.playersAround = around;
		tick.stillestNeighbourTicks = stillest;
	}

	private void announce(Set<Moments.HolidayEntry> holidays, Set<Moments.Odd> odd)
	{
		for (Moments.HolidayEntry holiday : holidays)
		{
			collected("New holiday: ", holiday.title, -1, false);
		}
		for (Moments.Odd moment : odd)
		{
			collected("New moment: ", moment.title, -1, false);
		}
		if (!holidays.isEmpty() || !odd.isEmpty())
		{
			dirty = true;
			save();
		}
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
		clientThread.invoke(() -> collected("New item: ", "Golden booth key", 989, true));
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
