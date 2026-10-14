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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import nz.tate.bankstanding.core.Unlocks;

/**
 * The level unlocks you can see while standing: the rug, the seat, fidgeting, bankers
 * talking to you, and the cape emote. All of it is drawn in this client only; nothing is sent
 * to the server and nobody else sees it. Runs on the client thread.
 */
@Singleton
class StandCosmetics
{
	private static final int TILE = 128;

	// Rug pieces: corner, side, middle. The standard rug is red with a gold trim.
	private static final int[] WORN_RUG = {1061, 1117, 1113};
	private static final int[] RUG = {1110, 1134, 1067};
	private static final short RUG_BODY = 926;
	private static final short RUG_TRIM = 9145;
	private static final short BLUE_BODY = (short) 46249;
	private static final short BLUE_TRIM = (short) 47378;
	private static final short GOLD_BODY = 9000;
	private static final short GOLD_TRIM = 8666;

	private static final int STOOL_MODEL = 10089;
	private static final int STOOL_SIT = AnimationID.CHAIR_SIT_READY_STOOL_1;
	private static final int THRONE_MODEL = 12594;
	private static final int THRONE_SIT = AnimationID.CHAIR_SIT_READY_THRONE_4;

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
	private static final String[] MASTER_LINES = {
		"Good day, Master.", "An honour, Master.", "Master of the bank floor!",
		"Can I get you anything, Master?",
	};

	private final Client client;
	private final Random random = new Random();

	private RuneLiteObject rugObject;
	private Unlocks.Rug rugShown = Unlocks.Rug.NONE;
	private RuneLiteObject seatObject;
	private Unlocks.Seat seatShown = Unlocks.Seat.NONE;
	private int sitAnimation = -1;
	private int standingIdlePose = -1;
	private boolean wasStanding;
	private int nextFidgetTick;
	private int nextBankerTick;
	private int ownAnimation = -1;
	private int ownAnimationUntilTick;

	@Inject
	StandCosmetics(Client client)
	{
		this.client = client;
	}

	/** Called every game tick with whether a stand is under way and what's switched on. */
	void tick(boolean standing, int level, BankStandingConfig config)
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

		if (started)
		{
			nextFidgetTick = now + 20 + random.nextInt(40);
			nextBankerTick = now + 3 + random.nextInt(5);
		}
		if (config.fidgets() && Unlocks.fidgets(level) && now >= nextFidgetTick)
		{
			nextFidgetTick = now + 40 + random.nextInt(60);
			if (player.getAnimation() == -1)
			{
				play(player, FIDGETS[random.nextInt(FIDGETS.length)]);
			}
		}
		if (config.bankerChat() && Unlocks.bankersNotice(level) && now >= nextBankerTick)
		{
			nextBankerTick = now + 60 + random.nextInt(120);
			bankerSays(player, level);
		}
	}

	/** Takes everything away and puts your normal pose back. */
	void clear()
	{
		wasStanding = false;
		if (rugObject != null)
		{
			rugObject.setActive(false);
			rugObject = null;
		}
		rugShown = Unlocks.Rug.NONE;
		if (seatObject != null)
		{
			seatObject.setActive(false);
			seatObject = null;
		}
		seatShown = Unlocks.Seat.NONE;
		standUp(client.getLocalPlayer());
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
		if (rugObject != null)
		{
			rugObject.setActive(false);
			rugObject = null;
		}
		rugShown = rug;
		if (rug == Unlocks.Rug.NONE)
		{
			return;
		}
		ModelData model = buildRug(rug == Unlocks.Rug.WORN ? WORN_RUG : RUG);
		if (model == null)
		{
			rugShown = Unlocks.Rug.NONE;
			return;
		}
		if (rug == Unlocks.Rug.BLUE)
		{
			model.recolor(RUG_BODY, BLUE_BODY).recolor(RUG_TRIM, BLUE_TRIM);
		}
		else if (rug == Unlocks.Rug.GOLD)
		{
			model.recolor(RUG_BODY, GOLD_BODY).recolor(RUG_TRIM, GOLD_TRIM);
		}
		rugObject = spawn(player, model, 0);
	}

	/** Lays out a 3 by 3 rug centred on your tile, turning each edge piece to face outwards. */
	private ModelData buildRug(int[] pieces)
	{
		List<ModelData> parts = new ArrayList<>();
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				int edges = Math.abs(dx) + Math.abs(dy);
				ModelData base = client.loadModelData(pieces[edges == 2 ? 0 : edges == 1 ? 1 : 2]);
				if (base == null)
				{
					return null;
				}
				ModelData piece = edges == 0 ? base.cloneVertices() : facing(base, dx, dy);
				parts.add(piece.translate(dx * TILE, 0, dy * TILE));
			}
		}
		return client.mergeModels(parts.toArray(new ModelData[0])).cloneColors();
	}

	/**
	 * Turns a rug edge piece so its border points towards (dx, dy). The border is found from the
	 * model itself: the faces that aren't the rug's main colour.
	 */
	private static ModelData facing(ModelData base, int dx, int dy)
	{
		ModelData best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (int turns = 0; turns < 4; turns++)
		{
			ModelData turned = base.cloneVertices();
			if (turns == 1)
			{
				turned.rotateY90Ccw();
			}
			else if (turns == 2)
			{
				turned.rotateY180Ccw();
			}
			else if (turns == 3)
			{
				turned.rotateY270Ccw();
			}
			double[] border = borderCentre(turned);
			double score = border[0] * dx + border[1] * dy;
			if (score > bestScore)
			{
				bestScore = score;
				best = turned;
			}
		}
		return best;
	}

	/** The average x and z of the faces that aren't the most common colour. */
	private static double[] borderCentre(ModelData model)
	{
		short[] colours = model.getFaceColors();
		Map<Short, Integer> counts = new HashMap<>();
		for (short colour : colours)
		{
			counts.merge(colour, 1, Integer::sum);
		}
		short main = colours.length == 0 ? 0 : colours[0];
		for (Map.Entry<Short, Integer> entry : counts.entrySet())
		{
			if (entry.getValue() > counts.get(main))
			{
				main = entry.getKey();
			}
		}
		float[] xs = model.getVerticesX();
		float[] zs = model.getVerticesZ();
		int[][] faces = {model.getFaceIndices1(), model.getFaceIndices2(), model.getFaceIndices3()};
		double x = 0;
		double z = 0;
		int n = 0;
		for (int face = 0; face < model.getFaceCount(); face++)
		{
			if (counts.size() > 1 && colours[face] == main)
			{
				continue;
			}
			for (int[] corner : faces)
			{
				x += xs[corner[face]];
				z += zs[corner[face]];
				n++;
			}
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
		seatObject = spawn(player, model.cloneVertices(), player.getOrientation());
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

	private RuneLiteObject spawn(Player player, ModelData model, int orientation)
	{
		LocalPoint here = LocalPoint.fromWorld(player.getWorldView(), player.getWorldLocation());
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

	private void bankerSays(Player player, int level)
	{
		WorldPoint here = player.getWorldLocation();
		List<NPC> bankers = new ArrayList<>();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			NPCComposition composition = npc.getTransformedComposition();
			if (composition != null && hasBankAction(composition.getActions())
				&& npc.getWorldLocation().distanceTo2D(here) <= 10)
			{
				bankers.add(npc);
			}
		}
		if (bankers.isEmpty())
		{
			return;
		}
		String[] lines = level >= Unlocks.MAX ? MASTER_LINES : level >= 70 && random.nextBoolean() ? REGULAR_LINES : BANKER_LINES;
		NPC banker = bankers.get(random.nextInt(bankers.size()));
		banker.setOverheadText(lines[random.nextInt(lines.length)]);
		banker.setOverheadCycle(150);
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
