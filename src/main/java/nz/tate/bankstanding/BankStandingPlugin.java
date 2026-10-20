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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MessageNode;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.WorldType;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ClientShutdown;
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
import nz.tate.bankstanding.core.ChatCommands;
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
import nz.tate.bankstanding.core.Spellbook;
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
	private JournalOverlay journal;

	@Inject
	private CollectionPopupOverlay collectionPopup;

	@Inject
	private StandCosmetics cosmetics;

	@Inject
	private TeaDelivery teaDelivery;

	@Inject
	private Pets pets;

	@Inject
	private ConfigManager configManager;

	/** The version whose changelog was last opened from the side panel. */
	private static final String CHANGELOG_SEEN = "changelogSeen";

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

	@Inject
	private PeekTimer peekTimer;

	@Inject
	private ModifierWidget modifierWidget;

	@Inject
	private ChatCommandManager chatCommandManager;

	private BankStandingPanel panel;
	private NavigationButton navButton;
	private NavigationButton devButton;

	private final StandTracker tracker = new StandTracker();
	private Session session = new Session();
	private final XpRoller roller = new XpRoller(new Random());
	private final Random collectionRandom = new Random();

	/** Our own slot in the player's spot animations, so the fireworks never replace the game's. */
	private static final int FIREWORKS_KEY = 0x42414E4B;

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

	/** Eating and drinking, which spoil healing from 1 hitpoint. */
	private static final Set<Integer> EATING = new HashSet<>(Arrays.asList(
		AnimationID.HUMAN_EAT, AnimationID.HUMAN_EAT_BANANA, AnimationID.HUMAN_EAT_WITHSOUND));
	/** Fires anyone can light from logs, in every colour. */
	private static final Set<Integer> FIRES = new HashSet<>(Arrays.asList(
		ObjectID.FIRE, ObjectID.RED_FIRE, ObjectID.GREEN_FIRE, ObjectID.BLUE_FIRE));
	/** How close a fire must be to a bank booth or chest to count. */
	private static final int FIRE_TILES = 2;
	/** Drop party balloons, before they're popped. */
	private static final int FIRST_BALLOON = ObjectID.BALLOON_YELLOW;
	private static final int LAST_BALLOON = ObjectID.BALLOON3;
	private final Set<GameObject> balloons = new HashSet<>();
	/** Random event NPCs that have come to you during this stand. */
	private final Set<NPC> randomEvents = new HashSet<>();
	/** The tick you were hit on during a stand, or -1. */
	private int hitOnTick = -1;
	/** When you last died, for the Archive's sad ghost; 0 if not this session. */
	private long diedAt;

	private Set<Integer> worn = Collections.emptySet();
	private int[] inventory = new int[0];
	private boolean maxCash;
	private int clueTiers;
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
		for (GameWindow window : windows())
		{
			overlayManager.add(window);
			mouseManager.registerMouseListener(window.mouse);
			mouseManager.registerMouseWheelListener(window.wheel);
		}
		for (GameWindow window : windows())
		{
			// One window at a time, like the game's own.
			window.setOnOpen(() ->
			{
				for (GameWindow other : windows())
				{
					if (other != window)
					{
						other.close();
					}
				}
			});
		}
		overlayManager.add(collectionPopup);
		skillTileOverlay.addMenuEntry(MenuAction.RUNELITE_OVERLAY, "View", "Collection log", e -> openCollectionLog());
		overlayManager.add(xpDropOverlay);
		overlayManager.add(hitsplatOverlay);
		overlayManager.add(peekTimer);
		overlayManager.add(modifierWidget);
		overlayManager.add(teaDelivery);
		// Only the half that runs after the message is sent: the message is never changed
		// on its way out, only your own copy of it is redrawn.
		chatCommandManager.registerCommand(ChatCommands.SHORT, this::onChatCommand);
		chatCommandManager.registerCommand(ChatCommands.LONG, this::onChatCommand);

		panel = new BankStandingPanel(icons, this::capeEmote, this::openCollectionLog,
			!BankStandingPanel.VERSION.equals(configManager.getConfiguration(BankStandingConfig.GROUP, CHANGELOG_SEEN)),
			() -> configManager.setConfiguration(BankStandingConfig.GROUP, CHANGELOG_SEEN, BankStandingPanel.VERSION));
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
		// Progress belongs to the client thread, so it's saved and dropped there.
		clientThread.invoke(() ->
		{
			save();
			capeDisplay.restore(wornCape);
			cosmetics.clear();
			teaDelivery.stopTuning();
			realXp.clear();
			progress = null;
			worn = Collections.emptySet();
			inventory = new int[0];
			wornValue = 0;
			wornCape = -1;
			emote = Emote.NONE;
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
		overlayManager.remove(peekTimer);
		peekTimer.ticksLeft(0);
		overlayManager.remove(modifierWidget);
		overlayManager.remove(teaDelivery);
		chatCommandManager.unregisterCommand(ChatCommands.SHORT);
		chatCommandManager.unregisterCommand(ChatCommands.LONG);
		modifierWidget.show(Collections.emptyList());
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
	}

	/** Closing RuneLite while logged in: save on the client thread before it goes. */
	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		CompletableFuture<Void> saved = new CompletableFuture<>();
		clientThread.invoke(() ->
		{
			save();
			saved.complete(null);
		});
		event.waitFor(saved);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOADING)
		{
			bankLocator.clear();
			balloons.clear();
			randomEvents.clear();
			cosmetics.clear();
		}
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			save();
			tracker.reset();
			peekTimer.ticksLeft(0);
			realXp.clear();
			capeDisplay.forget();
			cosmetics.clear();
			if (state == GameState.HOPPING)
			{
				// Still the same account and login, so the session and progress carry on.
				watch.standEnded();
			}
			else
			{
				watch.loggedOut();
				progress = null;
				BankStandingPanel shown = panel;
				SwingUtilities.invokeLater(shown::showLoggedOut);
			}
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
		int id = event.getGameObject().getId();
		if (id >= FIRST_BALLOON && id <= LAST_BALLOON)
		{
			balloons.add(event.getGameObject());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		bankLocator.objectDespawned(event.getGameObject());
		balloons.remove(event.getGameObject());
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() == client.getLocalPlayer() && event.getHitsplat().getAmount() > 0 && tracker.isStanding())
		{
			hitOnTick = client.getTickCount();
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			diedAt = System.currentTimeMillis();
		}
	}

	/** A random event that came to you and gave up while you kept standing. */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (randomEvents.remove(event.getNpc()) && progress != null && tracker.isStanding())
		{
			Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
			Moments.notice(progress, Moments.Odd.NOT_NOW, System.currentTimeMillis(), odd);
			announce(Collections.emptySet(), odd);
		}
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
		boolean fullStack = false;
		Set<String> tiers = new HashSet<>();
		for (int i = 0; i < slots.length; i++)
		{
			Item item = container.getItem(i);
			slots[i] = item == null ? -1 : item.getId();
			if (item == null || item.getId() <= 0)
			{
				continue;
			}
			if (item.getId() == ItemID.COINS && item.getQuantity() == Integer.MAX_VALUE)
			{
				fullStack = true;
			}
			String tier = clueTier(itemManager.getItemComposition(item.getId()).getName());
			if (tier != null)
			{
				tiers.add(tier);
			}
		}
		inventory = slots;
		maxCash = fullStack;
		clueTiers = tiers.size();
	}

	/** The tier of a clue scroll, like "easy", or null for anything else (boxes and caskets too). */
	static String clueTier(String name)
	{
		String lower = name == null ? "" : name.toLowerCase(Locale.ENGLISH);
		if (lower.startsWith("clue scroll (") && lower.endsWith(")"))
		{
			return lower.substring("clue scroll (".length(), lower.length() - 1);
		}
		return null;
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
		if ((animation == AnimationID.EMOTE_SIT || animation == AnimationID.EMOTE_SIT_LOOP) && progress != null && tracker.isStanding())
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
			|| !tracker.isIdleAtBank() || player.getName() == null
			|| !Text.sanitize(event.getName()).equals(Text.sanitize(player.getName())))
		{
			return;
		}

		WorldPoint location = mapLocation(player);
		String bankKey = BankNames.keyFor(location.getX(), location.getY());
		ClanSettings clan = client.getClanSettings();
		Map<Keywords.Group, int[]> before = keywords.foundByGroup(progress);
		long now = System.currentTimeMillis();
		Keywords.Kind phrase = keywords.claim(progress, Text.removeTags(event.getMessage()),
			now, today(), bankKey, worn, clan == null ? null : clan.getName(), spellbook());
		if (phrase != null)
		{
			if (keywords.paidOnlyHere(progress, now, bankKey))
			{
				Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
				Moments.archivePhrase(progress, bankKey, now, odd);
				announce(Collections.emptySet(), odd);
			}
			BankRecord bank = progress.bank(bankKey, location.getX(), location.getY());
			int level = progress.level();
			Modifiers.Effect effect = evaluate(bank, location);
			drop(roller.keywordRoll(level, effect.bonusHit), bank, level, phrase);
			newPhrase(before);
		}
	}

	/**
	 * {@code !bs} in chat: your own copy of the message is redrawn with the answer. Other
	 * players, and other people's {@code !bs}, see the message as typed.
	 */
	private void onChatCommand(ChatMessage chatMessage, String message)
	{
		Player player = client.getLocalPlayer();
		MessageNode node = chatMessage.getMessageNode();
		if (progress == null || player == null || player.getName() == null || node == null)
		{
			return;
		}
		boolean mine = chatMessage.getType() == ChatMessageType.PRIVATECHATOUT
			|| Text.sanitize(chatMessage.getName()).equals(Text.sanitize(player.getName()));
		if (!mine)
		{
			return;
		}
		ChatCommands.Facts facts = new ChatCommands.Facts();
		facts.progress = progress;
		facts.session = session;
		facts.today = today();
		if (tracker.isIdleAtBank())
		{
			WorldPoint location = mapLocation(player);
			facts.hereKey = BankNames.keyFor(location.getX(), location.getY());
		}
		facts.log = collectionLogOverlay.total(progress);
		facts.secrets = secretsFound();
		String answer = ChatCommands.answer(Text.removeTags(message), facts);
		if (answer != null)
		{
			node.setValue(answer);
			client.refreshChat();
		}
	}

	/** Every secret found, of every kind, as one number: never which. */
	private int secretsFound()
	{
		int found = 0;
		for (int[] count : keywords.foundByGroup(progress).values())
		{
			found += count[0];
		}
		for (int[] count : Modifiers.foundByGroup(progress.moments).values())
		{
			found += count[0];
		}
		for (Spellbook book : Spellbook.values())
		{
			found += progress.moments.containsKey(book.momentKey()) ? 1 : 0;
		}
		for (Moments.Odd odd : Moments.Odd.values())
		{
			found += odd.hidden && progress.moments.containsKey(odd.key) ? 1 : 0;
		}
		for (CollectionLog.Drop drop : CollectionLog.DROPS)
		{
			found += drop.hidden && progress.collection.containsKey(drop.key) ? 1 : 0;
		}
		return found;
	}

	/** The first time a phrase pays out, it's new in the log: say which kind, never which one. */
	private void newPhrase(Map<Keywords.Group, int[]> before)
	{
		for (Map.Entry<Keywords.Group, int[]> after : keywords.foundByGroup(progress).entrySet())
		{
			int[] count = after.getValue();
			if (count[0] > before.get(after.getKey())[0])
			{
				collected("Secret phrase: ", after.getKey().title.toLowerCase(Locale.ENGLISH)
					+ " (" + count[0] + "/" + count[1] + ")", -1, false);
				dirty = true;
			}
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
		teaDelivery.frame();
		pets.frame();
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
			if (progress.journalSince == 0)
			{
				progress.journalSince = System.currentTimeMillis();
			}
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
		peekTimer.ticksLeft(tracker.peekTicksLeft());

		// The cape and the other unlocks only show while bank standing; the dev options show
		// them anywhere.
		boolean standing = tracker.isStanding() || dev.pretendStanding;
		boolean moneybag = config.showPet() && (CollectionLog.petOut(progress, CollectionLog.MONEYBAG) || dev.previewPet);
		boolean rat = config.showPet() && (CollectionLog.petOut(progress, CollectionLog.RAT) || dev.previewPet);
		boolean butterfly = config.showPet() && (CollectionLog.petOut(progress, CollectionLog.BUTTERFLY) || dev.previewPet);
		WorldPoint here = mapLocation(player);
		cosmetics.tick(standing, currentLevel, config, moneybag, rat, butterfly, BankNames.keyFor(here.getX(), here.getY()),
			progress.banks.keySet());
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
			showModifiers(Collections.emptyList());
			progress.records.standEnded();
			watch.standEnded();
			randomEvents.clear();
			hitOnTick = -1;
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
		if (bank.ticks == 0 && bank.firstAt == 0)
		{
			bank.firstAt = System.currentTimeMillis();
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
		if (progress.records.standTicks() == 1)
		{
			bank.visits++;
		}
		noteMoments(bankKey, location, bank);
		standingOnFire(player, bank);
		showModifiers(config.showModifiers() ? activeModifiers(bankKey, bank, location) : Collections.emptyList());

		if (roll)
		{
			int level = progress.level();
			Modifiers.Effect effect = evaluate(bank, location);
			drop(roller.roll(level, effect.boost, effect.bonusHit), bank, level, null);
			progress.rolls++;
			Set<Moments.Odd> changed = EnumSet.noneOf(Moments.Odd.class);
			Set<String> outfits = new HashSet<>();
			for (String key : effect.found)
			{
				if (key.startsWith("outfit:"))
				{
					outfits.add(key);
				}
			}
			Moments.outfitsWorn(progress, today(), outfits, System.currentTimeMillis(), changed);
			announce(Collections.emptySet(), changed);
			CollectionLog.Drop found = CollectionLog.roll(collectionRandom, today(), bankKey);
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

	/** The spellbook you're on, or null if the game hasn't said. Client thread. */
	private Spellbook spellbook()
	{
		return Spellbook.of(client.getVarbitValue(VarbitID.SPELLBOOK));
	}

	private GameWindow[] windows()
	{
		// The log first: it can open over the skill tile, and a press on it there is the log's.
		return new GameWindow[]{journal, collectionLogOverlay, skillGuideOverlay};
	}

	/** Opens one of the journal's books on one tab, from the collection log. */
	void openJournal(int book, int tab)
	{
		journal.show(book, tab);
	}

	/** Back from the journal to its section of the collection log. */
	void openJournalSectionInLog(int book)
	{
		collectionLogOverlay.showSecrets(book == JournalOverlay.DRESSED);
		collectionLogOverlay.open();
	}

	/** Puts a pet away, or brings it back out, from its slot in the collection log. */
	void togglePet(String key)
	{
		clientThread.invoke(() ->
		{
			if (progress == null || !progress.collection.containsKey(key))
			{
				return;
			}
			if (!progress.hiddenPets.remove(key))
			{
				progress.hiddenPets.add(key);
			}
			dirty = true;
			save();
		});
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
		if (drop.rare() && !drop.hidden)
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
				Spellbook book = Spellbook.forMomentKey(key);
				if (book != null)
				{
					collected("New spellbook: ", book.title, book.icon, false);
				}
				else
				{
					collected("Dressed for it: ", dressedFor(key), -1, false);
				}
				dirty = true;
			}
		}
		return effect;
	}

	/** Dressed-for-it entries by moment key, for the modifier widget. */
	private static final Map<String, Modifiers.Dressed> DRESSED = new HashMap<>();

	static
	{
		for (Modifiers.Dressed dressed : Modifiers.dressed())
		{
			DRESSED.put(dressed.key, dressed);
		}
	}

	/**
	 * What the modifier widget shows this tick: the emote you did, while it lasts, and each
	 * full set that counts here. A set only shows once it's in the log, so the widget never
	 * gives away one you haven't found.
	 */
	private List<ModifierWidget.Shown> activeModifiers(String bankKey, BankRecord bank, WorldPoint location)
	{
		List<ModifierWidget.Shown> shown = new ArrayList<>();
		int ticksLeft = emoteUntilTick - client.getTickCount();
		if (emote != Emote.NONE && ticksLeft > 0)
		{
			shown.add(emoteShown(emote, System.currentTimeMillis() + ticksLeft * 600L));
		}
		for (String key : Modifiers.evaluate(context(bank, location)).found)
		{
			if (!progress.moments.containsKey(key))
			{
				continue;
			}
			Spellbook book = Spellbook.forMomentKey(key);
			if (book != null)
			{
				shown.add(ModifierWidget.Shown.buff(book.title, "+1 max hit", book.icon, -1, 0, 1, false));
				continue;
			}
			Modifiers.Dressed dressed = DRESSED.get(key);
			if (dressed == null)
			{
				continue;
			}
			String here = BankNames.displayName(bankKey, bank);
			String what = dressedFor(key);
			String name = Character.toUpperCase(what.charAt(0)) + what.substring(1) + " at " + here;
			boolean pet = dressed.pet != null;
			shown.add(ModifierWidget.Shown.buff(name, pet ? "Slightly better rolls" : "+1 max hit", dressed.icon, -1, 0,
				pet ? 0 : 1, pet));
		}
		cooldowns(shown, System.currentTimeMillis());
		return shown;
	}

	/** Sit, and standing on a fire once found, each pay out once an hour: show the wait. */
	private void cooldowns(List<ModifierWidget.Shown> shown, long now)
	{
		long sitReady = progress.lastSitRoll + Moments.SIT_COOLDOWN_MILLIS;
		if (progress.lastSitRoll != 0 && now < sitReady)
		{
			shown.add(ModifierWidget.Shown.cooldown("Sit", "A boosted drop once an hour", ItemID.POH_DINING_CHAIRS_1, -1, sitReady));
		}
		long fireReady = progress.lastFireRoll + Moments.SIT_COOLDOWN_MILLIS;
		if (progress.lastFireRoll != 0 && now < fireReady)
		{
			shown.add(ModifierWidget.Shown.cooldown("Hot seat", "A boosted drop once an hour", ItemID.TINDERBOX, -1, fireReady));
		}
	}

	/** Every kind of box at once, for the dev panel; the emote restarts each minute. */
	private List<ModifierWidget.Shown> previewModifiers()
	{
		long now = System.currentTimeMillis();
		long minute = 60_000L;
		long emoteEnds = (now / minute + 1) * minute;
		List<ModifierWidget.Shown> shown = new ArrayList<>();
		shown.add(emoteShown(Emote.FLEX, emoteEnds));
		shown.add(ModifierWidget.Shown.buff("Outfit (preview)", "+1 max hit", ItemID.SPIRIT_ANGLER_HAT, -1, 0, 1, false));
		shown.add(ModifierWidget.Shown.buff("Pet at home (preview)", "Slightly better rolls", -1, -1, 0, 0, true));
		shown.add(ModifierWidget.Shown.buff(Spellbook.LUNAR.title, "+1 max hit", Spellbook.LUNAR.icon, -1, 0, 1, false));
		shown.add(ModifierWidget.Shown.cooldown("Sit", "A boosted drop once an hour", ItemID.POH_DINING_CHAIRS_1, -1, now + 42 * minute));
		shown.add(ModifierWidget.Shown.cooldown("Hot seat", "A boosted drop once an hour", ItemID.TINDERBOX, -1, emoteEnds + 15_000));
		return shown;
	}

	/** What the widget shows this tick, unless the dev panel is previewing it. */
	private void showModifiers(List<ModifierWidget.Shown> shown)
	{
		modifierWidget.show(dev.previewModifiers ? previewModifiers() : shown);
	}

	private static ModifierWidget.Shown emoteShown(Emote emote, long endsAt)
	{
		switch (emote)
		{
			case LEAN:
				return ModifierWidget.Shown.buff("Lean", "Better rolls", -1, SpriteID.Emotes.LEAN, endsAt, 0, true);
			case YAWN:
				return ModifierWidget.Shown.buff("Yawn", "Slightly better rolls", -1, SpriteID.Emotes.YAWN, endsAt, 0, true);
			case FLEX:
				return ModifierWidget.Shown.buff("Flex", "+1 max hit", -1, -1, endsAt, 1, false);
			default:
				return ModifierWidget.Shown.buff("Skillcape emote", "+1 max hit", -1, SpriteID.Emotes.SKILLCAPE, endsAt, 1, false);
		}
	}

	/** What kind of thing counted, without saying which. */
	private static String dressedFor(String key)
	{
		if (key.startsWith("diary:"))
		{
			return "diary item";
		}
		if (key.startsWith("haul:"))
		{
			return "full inventory";
		}
		return key.startsWith("skilling-pet:") ? "pet at home" : "outfit";
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
		NPC follower = client.getFollower();
		context.follower = follower != null;
		context.followerName = follower == null || follower.getName() == null ? null
			: Text.removeTags(follower.getName()).replace('\u00A0', ' ').trim().toLowerCase(Locale.ENGLISH);
		Player player = client.getLocalPlayer();
		context.skulled = player != null && player.getSkullIcon() != -1;
		context.streakDays = DailyStreak.current(progress, context.today);
		context.hoursAtBank = bank.hours();
		context.inventory = inventory;
		context.spellbook = spellbook();
		context.zealUnlocked = client.getVarbitValue(VarbitID.PRAYER_ZEAL_UNLOCKED) == 1;
		return context;
	}

	private static LocalDate today()
	{
		return LocalDate.now();
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
		tick.local = LocalDateTime.now();
		tick.day = tick.local.toLocalDate();
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
		Player me = client.getLocalPlayer();
		tick.hitpoints = client.getBoostedSkillLevel(Skill.HITPOINTS);
		tick.maxHitpoints = client.getRealSkillLevel(Skill.HITPOINTS);
		tick.eating = EATING.contains(me.getAnimation()) && !cosmetics.isOwnAnimation(me.getAnimation());
		tick.balloons = !balloons.isEmpty();
		tick.maxCash = maxCash;
		tick.clueTiers = clueTiers;
		tick.wornValue = wornValue;
		tick.spellbook = spellbook();
		tick.inventoryEmpty = Arrays.stream(inventory).allMatch(id -> id <= 0);
		tick.emptyHanded = worn.isEmpty() && tick.inventoryEmpty;
		tick.diedAt = diedAt;
		watchNeighbours(tick);
		watchRandomEvents(me);
		Set<Moments.HolidayEntry> holidays = EnumSet.noneOf(Moments.HolidayEntry.class);
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		if (hitOnTick >= 0 && client.getTickCount() > hitOnTick)
		{
			// Hit last tick, and still standing.
			Moments.notice(progress, Moments.Odd.NOT_BUDGING, tick.now, odd);
			hitOnTick = -1;
		}
		Moments.standingTick(progress, watch, tick, holidays, odd);
		announce(holidays, odd);
		if (tick.healed)
		{
			obtain(CollectionLog.find(CollectionLog.BANDAGES));
		}
	}

	/** Random event NPCs come with a Dismiss option; note any that are here for you. */
	private void watchRandomEvents(Player me)
	{
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc == null || npc.getInteracting() != me)
			{
				continue;
			}
			NPCComposition composition = npc.getComposition();
			String[] actions = composition == null ? null : composition.getActions();
			if (actions != null && Arrays.asList(actions).contains("Dismiss"))
			{
				randomEvents.add(npc);
			}
		}
	}

	/**
	 * Standing on a lit fire close to a bank booth or chest: a moment for the log, and a
	 * boosted roll the first time and then at most once an hour.
	 */
	private void standingOnFire(Player player, BankRecord bank)
	{
		LocalPoint local = player.getLocalLocation();
		if (local == null || !bankLocator.isBoothOrChestNear(player.getWorldLocation(), FIRE_TILES))
		{
			return;
		}
		Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
		int plane = client.getTopLevelWorldView().getPlane();
		Tile tile = tiles[plane][local.getSceneX()][local.getSceneY()];
		if (tile == null)
		{
			return;
		}
		for (GameObject object : tile.getGameObjects())
		{
			if (object != null && FIRES.contains(object.getId()))
			{
				Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
				if (Moments.onFire(progress, System.currentTimeMillis(), odd))
				{
					int level = progress.level();
					drop(roller.boostedRoll(level), bank, level, null);
				}
				dirty = true;
				announce(Collections.emptySet(), odd);
				return;
			}
		}
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
		boolean sharedTile = false;
		for (Player other : client.getTopLevelWorldView().players())
		{
			if (other == null || other == me || other.getName() == null)
			{
				continue;
			}
			around++;
			WorldPoint at = other.getWorldLocation();
			sharedTile |= at.equals(here);
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
		tick.sharedTile = sharedTile;
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

	/** Sends tea now: {@code banker} is a bank whose banker comes, one of TeaDelivery's choices, or null for any. */
	void devTea(String banker)
	{
		clientThread.invoke(() ->
		{
			Player player = client.getLocalPlayer();
			if (player != null && progress != null)
			{
				WorldPoint here = mapLocation(player);
				cosmetics.devTea(BankNames.keyFor(here.getX(), here.getY()), progress.banks.keySet(), banker);
			}
		});
	}

	void devTune(String banker, int[] cup)
	{
		clientThread.invoke(() -> teaDelivery.tune(banker, cup));
	}

	void devTurnTuner()
	{
		clientThread.invoke(teaDelivery::turnTuner);
	}

	void devStopTuning()
	{
		clientThread.invoke(teaDelivery::stopTuning);
	}

	void devRat(int[] rat)
	{
		clientThread.invoke(() -> pets.devRat(rat));
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
