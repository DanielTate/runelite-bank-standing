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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
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
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import nz.tate.bankstanding.core.BankNames;
import nz.tate.bankstanding.core.BankRecord;
import nz.tate.bankstanding.core.DailyStreak;
import nz.tate.bankstanding.core.Emote;
import nz.tate.bankstanding.core.Experience;
import nz.tate.bankstanding.core.Keywords;
import nz.tate.bankstanding.core.Modifiers;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.Session;
import nz.tate.bankstanding.core.StandContext;
import nz.tate.bankstanding.core.StandTracker;
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
	private ClientThread clientThread;

	private BankStandingPanel panel;
	private NavigationButton navButton;

	private final StandTracker tracker = new StandTracker();
	private Session session = new Session();
	private final XpRoller roller = new XpRoller(new Random());

	/** Development only: `./gradlew run -PxpMultiplier=N` scales every drop. Always 1 for players. */
	/** Our own slot in the player's spot animations, so the fireworks never replace the game's. */
	private static final int FIREWORKS_KEY = 0x42414E4B;

	private static final int DEV_XP_MULTIPLIER = Math.max(1, Integer.getInteger("bankstanding.xpMultiplier", 1));

	/** Development only: `./gradlew run -PsetXp=N` overwrites the saved XP on the first login. */
	private static final Integer DEV_SET_XP = Integer.getInteger("bankstanding.setXp");
	/** Development only: `./gradlew run -PresetProgress` wipes the save on the first login. */
	private static final boolean DEV_RESET = Boolean.getBoolean("bankstanding.resetProgress");
	private static boolean devOverridesApplied;

	/**
	 * Development only: `./gradlew run -PpreviewEffects=3047,1414` plays each spot animation on
	 * the player in turn, naming it in chat, to choose the 99 effect. Empty for players.
	 */
	private static final int[] DEV_PREVIEW_EFFECTS = parseIds(System.getProperty("bankstanding.previewEffects", ""));
	private static final int DEV_PREVIEW_TICKS = 8;
	private static final int DEV_PREVIEW_KEY = 0x42414E4C;
	private int devPreviewTick;

	/** Development only: `./gradlew run -PpreviewCapes=9748,9769` cycles the 99 cape's base item. */
	private static final int[] DEV_PREVIEW_CAPES = parseIds(System.getProperty("bankstanding.previewCapes", ""));
	private int devCapeTick;

	/** Development only: `./gradlew run -PrareEvery=N` shows every Nth drop with the special splat. */
	private static final int DEV_RARE_EVERY = Integer.getInteger("bankstanding.rareEvery", 0);
	private int devDrops;
	private final Map<Skill, Integer> realXp = new EnumMap<>(Skill.class);
	private final Keywords keywords = new Keywords(Keywords.HASHES);

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
		tracker.reset();
		overlayManager.add(skillTileOverlay);
		overlayManager.add(xpDropOverlay);
		overlayManager.add(hitsplatOverlay);

		panel = new BankStandingPanel(icons);
		navButton = NavigationButton.builder()
			.tooltip("Bank Standing")
			.icon(icons.small())
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

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
		int realCape = wornCape;
		clientThread.invoke(() -> capeDisplay.restore(realCape));
		overlayManager.remove(skillTileOverlay);
		overlayManager.remove(xpDropOverlay);
		overlayManager.remove(hitsplatOverlay);
		dropFeed.clear();
		clientToolbar.removeNavigation(navButton);
		navButton = null;
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
		}
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			tracker.reset();
			realXp.clear();
			progress = null;
			capeDisplay.forget();
			BankStandingPanel shown = panel;
			SwingUtilities.invokeLater(shown::showLoggedOut);
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
		Emote started = Emote.fromAnimation(event.getActor().getAnimation());
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

		if (keywords.claim(progress, Text.removeTags(event.getMessage()), System.currentTimeMillis(), today()))
		{
			WorldPoint location = player.getWorldLocation();
			BankRecord bank = progress.bank(BankNames.keyFor(location.getX(), location.getY()),
				location.getX(), location.getY());
			int level = progress.level();
			Modifiers.Effect effect = Modifiers.evaluate(context(bank, location));
			drop(roller.keywordRoll(level, effect.bonusHit), bank, level, effect);
		}
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
			session = new Session();
			refreshPanel();
		}

		previewEffect(player);

		if (config.showCape() && progress.level() >= Experience.MAX_LEVEL)
		{
			previewCape();
			capeDisplay.apply();
		}

		WorldPoint location = player.getWorldLocation();
		boolean roll = tracker.tick(location.getX(), location.getY(), location.getPlane(),
			bankLocator.isNearBank(location), isUsingBank());

		if (!tracker.isStanding())
		{
			return;
		}

		BankRecord bank = progress.bank(BankNames.keyFor(location.getX(), location.getY()),
			location.getX(), location.getY());
		bank.ticks++;
		session.ticks++;
		DailyStreak.standingTick(progress, today());

		if (roll)
		{
			int level = progress.level();
			Modifiers.Effect effect = Modifiers.evaluate(context(bank, location));
			drop(roller.roll(level, effect.boost, effect.bonusHit), bank, level, effect);
		}
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

	/** Awards one rolled hit and shows it. A hit above the normal max for the level is boosted. */
	private void drop(int hit, BankRecord bank, int level, Modifiers.Effect effect)
	{
		int xp = hit * DEV_XP_MULTIPLIER;
		award(xp, bank, level);
		boolean showRare = DEV_RARE_EVERY > 0 && ++devDrops % DEV_RARE_EVERY == 0;
		dropFeed.add(xp, hit == XpRoller.maxHit(level, effect.bonusHit), showRare || hit > XpRoller.baseMaxHit(level));
	}

	private void award(int drop, BankRecord bank, int levelBefore)
	{
		int before = progress.xp;
		progress.xp = Experience.addXp(progress.xp, drop);
		bank.xp += progress.xp - before;
		session.xp += progress.xp - before;
		store.save(progress);
		refreshPanel();

		int levelAfter = progress.level();
		if (levelAfter > levelBefore)
		{
			announceLevel(levelAfter);
		}
	}

	private void refreshPanel()
	{
		StatsView view = new StatsView(progress, session, today().toString());
		BankStandingPanel shown = panel;
		SwingUtilities.invokeLater(() -> shown.show(view));
	}

	private void applyDevOverrides()
	{
		if (devOverridesApplied || (!DEV_RESET && DEV_SET_XP == null))
		{
			return;
		}
		devOverridesApplied = true;
		if (DEV_RESET)
		{
			progress = new Progress();
		}
		if (DEV_SET_XP != null)
		{
			progress.xp = Math.max(0, Math.min(Experience.MAX_XP, DEV_SET_XP));
		}
		store.save(progress);
	}

	private void previewEffect(Player player)
	{
		if (DEV_PREVIEW_EFFECTS.length == 0 || devPreviewTick++ % DEV_PREVIEW_TICKS != 0)
		{
			return;
		}
		int effect = DEV_PREVIEW_EFFECTS[(devPreviewTick / DEV_PREVIEW_TICKS) % DEV_PREVIEW_EFFECTS.length];
		player.createSpotAnim(DEV_PREVIEW_KEY, effect, 0, 0);
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage("Effect preview: " + effect)
			.build());
	}

	private void previewCape()
	{
		if (DEV_PREVIEW_CAPES.length == 0 || devCapeTick++ % DEV_PREVIEW_TICKS != 0)
		{
			return;
		}
		int cape = DEV_PREVIEW_CAPES[(devCapeTick / DEV_PREVIEW_TICKS) % DEV_PREVIEW_CAPES.length];
		capeDisplay.setCapeItem(cape);
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage("Cape preview: " + itemManager.getItemComposition(cape).getName() + " (" + cape + ")")
			.build());
	}

	private static int[] parseIds(String list)
	{
		return Arrays.stream(list.split(","))
			.map(String::trim)
			.filter(id -> !id.isEmpty())
			.mapToInt(Integer::parseInt)
			.toArray();
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
		if (player != null)
		{
			int fireworks = level >= Experience.MAX_LEVEL ? SpotanimID.LEVELUP_99_ANIM : SpotanimID.LEVELUP_ANIM;
			player.createSpotAnim(FIREWORKS_KEY, fireworks, 0, 0);
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
