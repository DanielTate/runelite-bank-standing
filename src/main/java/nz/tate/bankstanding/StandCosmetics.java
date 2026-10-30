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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import nz.tate.bankstanding.core.Moments;
import nz.tate.bankstanding.core.Unlocks;

/**
 * The level unlocks you can see while standing: the rug, the seat, fidgeting, bankers
 * talking to you, and the cape emote. All of it is drawn in this client only; nothing is sent
 * to the server and nobody else sees it. Runs on the client thread.
 */
@Singleton
class StandCosmetics
{
	// Rug pieces: corner, side, middle. The standard rug is red with a gold trim. The game's
	// worn rug (objects 917 and 918) has only corners and sides, so its middle is the standard
	// rug's, recoloured to match.
	private static final int[] WORN_RUG = {1061, 1117, 1067};
	private static final int[] RUG = {1110, 1134, 1067};
	private static final short RUG_BODY = 926;
	private static final short RUG_TRIM = 9145;
	/** The standard rug middle's pattern. */
	private static final short RUG_PATTERN = 0;
	private static final short WORN_BODY = 7952;
	private static final short WORN_TRIM = 9114;
	private static final short BLUE_BODY = (short) 46249;
	private static final short BLUE_TRIM = (short) 47378;
	private static final short GOLD_BODY = 9000;
	private static final short GOLD_TRIM = 8666;

	private static final int STOOL_MODEL = 10089;
	private static final int STOOL_SIT = AnimationID.CHAIR_SIT_READY_STOOL_1;
	private static final int THRONE_MODEL = 12594;
	private static final int THRONE_SIT = AnimationID.CHAIR_SIT_READY_THRONE_4;

	/** At 99 the gold carpet comes with riches: coin piles on its corners and gold bars at the front and back. */
	private static final int COIN_PILE = 1004;
	private static final int GOLD_BAR = 2357;
	private static final int RICHES_SCALE = 192;

	private static final int[] FIDGETS = {AnimationID.EMOTE_THINK, AnimationID.EMOTE_SHRUG, AnimationID.EMOTE_YAWN};
	private static final int CAPE_EMOTE = AnimationID.MAX_CAPE_PLAYER_ANIM;
	private static final int CAPE_EMOTE_SPOTANIM = 1286;
	private static final int CAPE_EMOTE_KEY = 0x42414E50;
	/** Ticks an animation we played is ignored by the emote bonuses. */
	private static final int OWN_ANIMATION_TICKS = 8;

	private static final String[] BANKER_LINES = {
		"Back again?", "Nice day for it.", "Your usual spot is free.", "Still here?",
		"Don't mind me, just counting coins.", "I could set my watch by you.",
		"Busy day, isn't it?", "Take your time.",
	};
	private static final String[] REGULAR_LINES = {
		"Good to see a regular!", "We saved your spot.", "Standing like a pro.",
		"The bank wouldn't be the same without you.",
	};
	/** Said when a banker hands you tea, from level 85. */
	private static final String[] TEA_LINES = {
		"Here's your cuppa.", "Milk, no sugar, right?", "Fresh from the pot.", "Careful, it's hot.",
		"Tea's on the house.", "Thought you could use a brew.",
	};
	/**
	 * Said by the banker at the Fractured Archive: hints toward the phrases that work there,
	 * never the phrases themselves.
	 */
	private static final String[] ARCHIVE_LINES = {
		"Last chance to deposit before the cliff face.", "Everyone calls this place by a nickname from the trailer.",
		"The builders' note ends with a prayer to their god.", "They left this place for something more important.",
		"The Guthixians named that stone after a hand.", "Juna says these rocks once cried.",
		"He sent the gods away by decree, you know.", "That megarare hammer has two ways to swing.",
		"Everyone's begging for the megarare.", "A Mahjarrat took the stone that opened this place.",
		"An assassin once told you it wasn't personal.", "Bork brought friends this time.",
		"Stop pinging your inventory at people.", "The general plans at a board out west.",
		"Looking for a team? There's a board east of me.", "A new prayer takes more than a bit of reading.",
	};
	/** At the Archive the banker sits on the east chest and calls across to the west one. */
	private static final int ARCHIVE_BANKER_TILES = 20;
	private static final int BANKER_TILES = 10;
	/** The drink animation you do when the tea arrives, if you aren't sitting. */
	private static final int DRINK = AnimationID.HUMAN_EAT;
	private static final String[] MASTER_LINES = {
		"Good day, Master.", "An honour, Master.", "Master of the bank floor!",
		"Can I get you anything, Master?",
	};

	private final Client client;
	private final Random random = new Random();

	private final List<RuneLiteObject> rugPieces = new ArrayList<>();
	private Unlocks.Rug rugShown = Unlocks.Rug.NONE;
	private RuneLiteObject seatObject;
	private final List<RuneLiteObject> riches = new ArrayList<>();
	private final TeaDelivery teaDelivery;
	private final Pets pets;
	/** Whether tea has come this stand: one cup a stand. */
	private boolean teaServed;
	private Unlocks.Seat seatShown = Unlocks.Seat.NONE;
	private int sitAnimation = -1;
	private int standingIdlePose = -1;
	private boolean wasStanding;
	private int nextFidgetTick;
	private int nextBankerTick;
	private int ownAnimation = -1;
	private int ownAnimationUntilTick;

	@Inject
	StandCosmetics(Client client, TeaDelivery teaDelivery, Pets pets)
	{
		this.client = client;
		this.teaDelivery = teaDelivery;
		this.pets = pets;
	}

	/**
	 * Called every game tick with whether a stand is under way, what's switched on, the bank
	 * you're at and the banks you've stood at.
	 */
	void tick(boolean standing, int level, BankStandingConfig config, boolean moneybag, boolean rat, boolean butterfly,
		String bankKey, Set<String> visited)
	{
		Player player = client.getLocalPlayer();
		if (player == null || !standing || !onLand(player))
		{
			clear();
			return;
		}
		int now = client.getTickCount();
		boolean started = !wasStanding;
		wasStanding = true;

		Unlocks.Rug rug = config.showRug() ? Unlocks.rug(level) : Unlocks.Rug.NONE;
		Unlocks.Seat seat = config.showSeat() ? Unlocks.seat(level) : Unlocks.Seat.NONE;
		if (started || rug != rugShown)
		{
			placeRug(player, rug);
		}
		if (started || seat != seatShown)
		{
			placeSeat(player, seat);
		}
		keepSitting(player);
		pets.tick(player, moneybag, rat, butterfly);

		if (started)
		{
			nextFidgetTick = now + 20 + random.nextInt(40);
			nextBankerTick = now + 3 + random.nextInt(5);
		}
		if (config.fidgets() && Unlocks.fidgets(level) && now >= nextFidgetTick)
		{
			nextFidgetTick = now + 40 + random.nextInt(60);
			// The fidgets are standing emotes, so they'd pull you half out of your seat.
			if (player.getAnimation() == -1 && sitAnimation == -1)
			{
				play(player, FIDGETS[random.nextInt(FIDGETS.length)]);
			}
		}
		if (config.bankerChat() && Unlocks.bankersNotice(level) && now >= nextBankerTick)
		{
			nextBankerTick = now + 60 + random.nextInt(120);
			bankerSays(player, level, config.teaDelivery(), bankKey, visited);
		}
	}

	/** Takes everything away and puts your normal pose back. */
	void clear()
	{
		wasStanding = false;
		removeRug();
		rugShown = Unlocks.Rug.NONE;
		if (seatObject != null)
		{
			seatObject.setActive(false);
			seatObject = null;
		}
		seatShown = Unlocks.Seat.NONE;
		pets.clear();
		teaDelivery.clear();
		teaServed = false;
		standUp(client.getLocalPlayer());
	}

	/**
	 * An item's model as it looks on the ground, with the item's own colours: a gold bar is the
	 * bronze bar model recoloured, for one. A copy, safe to scale.
	 */
	static ModelData itemModel(Client client, int item)
	{
		ItemComposition composition = client.getItemDefinition(item);
		int id = composition.getInventoryModel();
		ModelData data = id < 0 ? null : client.loadModelData(id);
		if (data == null)
		{
			return null;
		}
		ModelData model = data.cloneVertices().cloneColors();
		short[] from = composition.getColorToReplace();
		short[] to = composition.getColorToReplaceWith();
		if (from != null && to != null)
		{
			for (int i = 0; i < Math.min(from.length, to.length); i++)
			{
				model.recolor(from[i], to[i]);
			}
		}
		return model;
	}

	/** The 99 cape emote: the max cape animation, played in this client only. */
	void capeEmote()
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			play(player, CAPE_EMOTE);
			player.createSpotAnim(CAPE_EMOTE_KEY, CAPE_EMOTE_SPOTANIM, 0, 0);
		}
	}

	/** True for an animation this class just played, so it doesn't count as the player's emote. */
	boolean isOwnAnimation(int animation)
	{
		return animation == ownAnimation && client.getTickCount() <= ownAnimationUntilTick;
	}

	private void play(Player player, int animation)
	{
		ownAnimation = animation;
		ownAnimationUntilTick = client.getTickCount() + OWN_ANIMATION_TICKS;
		player.setAnimation(animation);
		player.setAnimationFrame(0);
	}

	/** Boats move under you, so the props are only laid out on solid ground. */
	private boolean onLand(Player player)
	{
		WorldView view = player.getWorldView();
		return view != null && view.isTopLevel();
	}

	private void placeRug(Player player, Unlocks.Rug rug)
	{
		removeRug();
		rugShown = rug;
		if (rug == Unlocks.Rug.NONE)
		{
			return;
		}
		int[] pieces = rug == Unlocks.Rug.WORN ? WORN_RUG : RUG;
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				int edges = Math.abs(dx) + Math.abs(dy);
				ModelData base = client.loadModelData(pieces[edges == 2 ? 0 : edges == 1 ? 1 : 2]);
				if (base == null)
				{
					removeRug();
					rugShown = Unlocks.Rug.NONE;
					return;
				}
				ModelData piece = base.cloneColors();
				if (rug == Unlocks.Rug.WORN && edges == 0)
				{
					piece.recolor(RUG_BODY, WORN_BODY).recolor(RUG_PATTERN, WORN_TRIM);
				}
				else if (rug == Unlocks.Rug.BLUE)
				{
					piece.recolor(RUG_BODY, BLUE_BODY).recolor(RUG_TRIM, BLUE_TRIM);
				}
				else if (rug == Unlocks.Rug.GOLD)
				{
					piece.recolor(RUG_BODY, GOLD_BODY).recolor(RUG_TRIM, GOLD_TRIM);
				}
				int turns = edges == 0 ? 0 : turnsToFaceMiddle(base, dx, dy);
				RuneLiteObject object = spawn(player, piece, dx, dy, turns * 512);
				if (object != null)
				{
					rugPieces.add(object);
				}
			}
		}
		if (rug == Unlocks.Rug.GOLD)
		{
			placeRiches(player);
		}
	}

	/** Coin piles on the gold carpet's corners and gold bars front and back: you've made it. */
	private void placeRiches(Player player)
	{
		int[][] corners = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
		for (int[] corner : corners)
		{
			addRiches(player, COIN_PILE, corner[0], corner[1]);
		}
		addRiches(player, GOLD_BAR, 0, -1);
		addRiches(player, GOLD_BAR, 0, 1);
	}

	private void addRiches(Player player, int item, int dx, int dy)
	{
		ModelData data = itemModel(client, item);
		if (data == null)
		{
			return;
		}
		RuneLiteObject object = spawn(player, data.scale(RICHES_SCALE, RICHES_SCALE, RICHES_SCALE), dx, dy,
			random.nextInt(2048));
		if (object != null)
		{
			riches.add(object);
		}
	}

	private void removeRug()
	{
		for (RuneLiteObject piece : rugPieces)
		{
			piece.setActive(false);
		}
		rugPieces.clear();
		for (RuneLiteObject piece : riches)
		{
			piece.setActive(false);
		}
		riches.clear();
	}

	/**
	 * How many quarter turns make a rug edge piece face the middle. The game's edge pieces only
	 * cover the inner part of their tile (the rug's edge falls mid-tile), so the piece is turned
	 * until the area it covers points back towards the player's tile. A quarter turn moves a
	 * point (x, z) to (z, -x), the same way the renderer turns an object.
	 */
	private static int turnsToFaceMiddle(ModelData model, int dx, int dy)
	{
		double[] centre = centre(model);
		double x = centre[0];
		double z = centre[1];
		int best = 0;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (int turns = 0; turns < 4; turns++)
		{
			double score = -(x * dx + z * dy);
			if (score > bestScore)
			{
				bestScore = score;
				best = turns;
			}
			double turned = z;
			z = -x;
			x = turned;
		}
		return best;
	}

	/** The average x and z of the model's vertices. */
	private static double[] centre(ModelData model)
	{
		float[] xs = model.getVerticesX();
		float[] zs = model.getVerticesZ();
		int n = model.getVerticesCount();
		double x = 0;
		double z = 0;
		for (int i = 0; i < n; i++)
		{
			x += xs[i];
			z += zs[i];
		}
		return n == 0 ? new double[]{0, 0} : new double[]{x / n, z / n};
	}

	private void placeSeat(Player player, Unlocks.Seat seat)
	{
		if (seatObject != null)
		{
			seatObject.setActive(false);
			seatObject = null;
		}
		standUp(player);
		seatShown = seat;
		if (seat == Unlocks.Seat.NONE)
		{
			return;
		}
		ModelData model = client.loadModelData(seat == Unlocks.Seat.THRONE ? THRONE_MODEL : STOOL_MODEL);
		if (model == null)
		{
			seatShown = Unlocks.Seat.NONE;
			return;
		}
		seatObject = spawn(player, model, 0, 0, player.getOrientation());
		sitAnimation = seat == Unlocks.Seat.THRONE ? THRONE_SIT : STOOL_SIT;
	}

	/** The game can reset your idle pose at any time, so the sitting pose is put back every tick. */
	private void keepSitting(Player player)
	{
		if (sitAnimation == -1)
		{
			return;
		}
		int idle = player.getIdlePoseAnimation();
		if (idle != sitAnimation)
		{
			standingIdlePose = idle;
			player.setIdlePoseAnimation(sitAnimation);
		}
		if (player.getPoseAnimation() == standingIdlePose)
		{
			player.setPoseAnimation(sitAnimation);
		}
		if (seatObject != null)
		{
			seatObject.setOrientation(player.getOrientation());
		}
	}

	private void standUp(Player player)
	{
		if (sitAnimation != -1 && player != null && standingIdlePose != -1)
		{
			if (player.getIdlePoseAnimation() == sitAnimation)
			{
				player.setIdlePoseAnimation(standingIdlePose);
			}
			if (player.getPoseAnimation() == sitAnimation)
			{
				player.setPoseAnimation(standingIdlePose);
			}
		}
		sitAnimation = -1;
		standingIdlePose = -1;
	}

	/** Places a model on the tile (dx, dy) away from the player. */
	private RuneLiteObject spawn(Player player, ModelData model, int dx, int dy, int orientation)
	{
		LocalPoint here = LocalPoint.fromWorld(player.getWorldView(), player.getWorldLocation().dx(dx).dy(dy));
		if (here == null)
		{
			return null;
		}
		RuneLiteObject object = client.createRuneLiteObject();
		object.setModel(model.light());
		object.setLocation(here, player.getWorldLocation().getPlane());
		object.setOrientation(orientation);
		object.setActive(true);
		return object;
	}

	private void bankerSays(Player player, int level, boolean tea, String bankKey, Set<String> visited)
	{
		WorldPoint here = player.getWorldLocation();
		boolean archive = Moments.atArchive(bankKey);
		int range = archive ? ARCHIVE_BANKER_TILES : BANKER_TILES;
		List<NPC> bankers = new ArrayList<>();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			NPCComposition composition = npc.getTransformedComposition();
			if (composition != null && hasBankAction(composition.getActions())
				&& npc.getWorldLocation().distanceTo2D(here) <= range)
			{
				bankers.add(npc);
			}
		}
		NPC banker = bankers.isEmpty() ? null : bankers.get(random.nextInt(bankers.size()));
		if (tea && Unlocks.tea(level) && !teaServed && !teaDelivery.isBusy() && random.nextInt(3) == 0)
		{
			// One cup a stand, carried over by a banker.
			teaServed = true;
			teaDelivery.start(player, banker, bankKey, visited, TEA_LINES[random.nextInt(TEA_LINES.length)],
				() -> drinkTea(player));
			return;
		}
		if (banker == null)
		{
			return;
		}
		String[] lines = archive && random.nextBoolean() ? ARCHIVE_LINES
			: level >= Unlocks.MAX ? MASTER_LINES : level >= 70 && random.nextBoolean() ? REGULAR_LINES : BANKER_LINES;
		banker.setOverheadText(lines[random.nextInt(lines.length)]);
		banker.setOverheadCycle(150);
	}

	/** Sends a banker over with tea now, for the dev panel. */
	void devTea(String bankKey, Set<String> visited, String banker)
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			teaDelivery.clear();
			teaServed = false;
			nextBankerTick = 0;
			NPC local = null;
			for (NPC npc : client.getTopLevelWorldView().npcs())
			{
				NPCComposition composition = npc.getTransformedComposition();
				if (composition != null && hasBankAction(composition.getActions())
					&& npc.getWorldLocation().distanceTo2D(player.getWorldLocation()) <= ARCHIVE_BANKER_TILES)
				{
					local = npc;
				}
			}
			teaServed = true;
			teaDelivery.start(player, local, bankKey, visited, TEA_LINES[random.nextInt(TEA_LINES.length)],
				() -> drinkTea(player), banker);
		}
	}

	/** You take a sip when the tea arrives, unless you're sitting, where a standing animation looks wrong. */
	private void drinkTea(Player player)
	{
		if (sitAnimation == -1 && player.getAnimation() == -1)
		{
			play(player, DRINK);
		}
	}

	private static boolean hasBankAction(String[] actions)
	{
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if ("Bank".equalsIgnoreCase(action))
			{
				return true;
			}
		}
		return false;
	}
}
