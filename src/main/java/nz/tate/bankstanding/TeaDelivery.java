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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * From level 85, a banker walks over with a cup of tea, hands it to you and walks off again.
 * The banker is a copy drawn in this client only: the real one stays behind the counter. Now
 * and then it's a banker from another bank you've stood at, come to visit.
 */
@Singleton
class TeaDelivery extends Overlay
{
	/** A banker model, how it moves (stand and walk animations), and where the cup sits on its head. */
	private static final class Banker
	{
		final int npc;
		final int stand;
		final int walk;
		/** Where the cup sits, in model units: height (up is negative), forward (negative) and sideways. */
		final int[] cup;

		/** With the cup placed for this banker's head: height, forward and sideways. */
		Banker(int npc, int stand, int walk, int... cup)
		{
			this.npc = npc;
			this.stand = stand;
			this.walk = walk;
			this.cup = cup.length == 3 ? cup : new int[]{CUP_HEIGHT, CUP_FORWARD, 0};
		}
	}

	private static final int HUMAN_STAND = AnimationID.HUMAN_READY;
	private static final int HUMAN_WALK = AnimationID.HUMAN_WALK_F;
	/** The classic banker, for banks whose own banker can't walk over. */
	private static final Banker GENERIC = new Banker(2897, HUMAN_STAND, HUMAN_WALK);
	/** Bankers that come visiting, by the bank they work at. */
	private static final Map<String, Banker> VISITORS = new HashMap<>();
	/** Banks whose own bankers can't walk (dwarves and gnomes stand still), so the classic banker comes. */
	private static final Set<String> STILL_BANKERS = Set.of("keldagrim", "lovakengj", "lovakengj-mine",
		"gnome-stronghold-north", "gnome-stronghold-south");

	static
	{
		// Cup positions (height, forward, sideways) were set by eye with the dev panel's tuner.
		human("lletya", 1480, -192, -9, 10);
		human("warriors-guild", 2472);
		human("canifis", 2633);
		human("port-phasmatys", 3003);
		human("shilo", 3093);
		human("rogues-den", 3194);
		human("prifddinas-north", 9127, -195, -2, 0);
		human("prifddinas-south", 9127, -195, -2, 0);
		human("burgh-de-rott", 4471, -206, -7, 0);
		human("mage-arena", 1600);
		human("ferox", 10389, -196, -11, 0);
		human("shayzien", 6859);
		human("hosidius", 6939, -194, -13, 4);
		human("al-kharid", 3090);
		human("farming-guild", 8589);
		VISITORS.put("dorgesh-kaan", new Banker(2293, 6040, 6041, -162, -43, 1));
		// The fairy flies, so her head is higher up.
		VISITORS.put("zanaris", new Banker(3092, 112, 112, -207, -7, 0));
		VISITORS.put("tzhaar", new Banker(7677, 2603, 2601, -171, 15, 1));
		VISITORS.put("mor-ul-rek", new Banker(7677, 2603, 2601, -171, 15, 1));
		// Ape Atoll's own banker is a monkey too small to carry a cup, so a gorilla brings it.
		VISITORS.put("ape-atoll", new Banker(1464, 1401, 1399, -150, -49, -8));
		VISITORS.put("darkmeyer", new Banker(8322, 4702, 4699, -197, -9, 0));
	}

	private static void human(String bank, int npc, int... cup)
	{
		VISITORS.put(bank, new Banker(npc, HUMAN_STAND, HUMAN_WALK, cup));
	}

	/** For the dev panel: the banker standing nearby, or the classic banker. */
	static final String LOCAL = "local";
	static final String CLASSIC = "classic";

	/** The banks with a visiting banker, for the dev panel. */
	static List<String> visitorBanks()
	{
		List<String> banks = new ArrayList<>(VISITORS.keySet());
		Collections.sort(banks);
		return banks;
	}

	/** One time in this many, a banker from somewhere else you've stood brings the tea. */
	private static final int VISITOR_CHANCE = 4;
	/** How far away the banker sets off from, in steps. */
	private static final int START_STEPS = 7;
	/** Walking speed: one tile per game tick, in local units per millisecond. */
	private static final double SPEED = 128 / 600.0;
	private static final long HAND_OVER_MILLIS = 3000;
	private static final long SPEECH_MILLIS = 3000;
	/** Said while setting off with the tea. */
	private static final String[] SETTING_OFF = {
		"Tea's up!", "One cup of tea, coming through!", "Mind your backs, hot tea!", "Brew's ready!",
	};
	/** Said walking away again. */
	private static final String[] LEAVING = {"Enjoy your brew!", "Don't let it go cold.", "Sip slowly."};
	/** The cup, carried balanced on the banker's head, shrunk from its inventory size. */
	private static final int CUP_ITEM = 4245;
	private static final int CUP_SCALE = 64;
	/** The top of a human banker's head, in model units (up is negative). */
	private static final int CUP_HEIGHT = -192;
	/** Nudges the cup forward over the banker's face; forward is negative. */
	private static final int CUP_FORWARD = -7;
	private static final Color SPEECH = Color.YELLOW;

	private enum State
	{
		NONE,
		ARRIVING,
		HANDING_OVER,
		LEAVING
	}

	private final Client client;
	private final Random random = new Random();
	private State state = State.NONE;
	private RuneLiteObject walker;
	/** The banker without the cup, once it's handed over. */
	private Model emptyHanded;
	private Banker banker;
	/** Tile centres to walk through, in local units. */
	private final List<int[]> path = new ArrayList<>();
	private int step;
	private double x;
	private double y;
	private int plane;
	private long lastFrame;
	private long stateStarted;
	private String speech;
	private String handOverLine;
	private long speechUntil;
	private Runnable onServed;

	@Inject
	TeaDelivery(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	boolean isBusy()
	{
		return state != State.NONE;
	}

	/**
	 * Sends a banker over with tea. {@code local} is a banker standing nearby, or null.
	 * {@code onServed} runs when the cup is handed over. Client thread.
	 */
	void start(Player player, NPC local, String bankKey, Set<String> visited, String line, Runnable onServed)
	{
		start(player, local, bankKey, visited, line, onServed, null);
	}

	/** As above; {@code chosen} picks the banker for the dev panel: a bank, LOCAL, CLASSIC, or null for any. */
	void start(Player player, NPC local, String bankKey, Set<String> visited, String line, Runnable onServed,
		String chosen)
	{
		if (state != State.NONE)
		{
			return;
		}
		Banker who = chosen == null ? pick(bankKey, visited) : LOCAL.equals(chosen) ? null
			: CLASSIC.equals(chosen) ? GENERIC : VISITORS.get(chosen);
		NPCComposition composition = who != null ? client.getNpcDefinition(who.npc)
			: local != null ? local.getTransformedComposition() : null;
		if (composition == null)
		{
			who = GENERIC;
			composition = client.getNpcDefinition(GENERIC.npc);
		}
		if (who == null)
		{
			who = new Banker(composition.getId(), HUMAN_STAND, HUMAN_WALK);
		}
		ModelData carrying = model(composition, who.cup);
		ModelData empty = model(composition, null);
		if (carrying == null || empty == null || !plan(player, local))
		{
			return;
		}
		banker = who;
		emptyHanded = empty.light();
		walker = client.createRuneLiteObject();
		walker.setModel(carrying.light());
		int[] first = path.get(0);
		x = first[0];
		y = first[1];
		plane = player.getWorldLocation().getPlane();
		step = 1;
		place();
		animate(banker.walk);
		walker.setActive(true);
		say(SETTING_OFF[random.nextInt(SETTING_OFF.length)], System.currentTimeMillis());
		handOverLine = line;
		this.onServed = onServed;
		lastFrame = System.currentTimeMillis();
		state = State.ARRIVING;
	}

	/** Which banker comes: now and then a visitor from a bank you've stood at, else this bank's own. */
	private Banker pick(String bankKey, Set<String> visited)
	{
		if (random.nextInt(VISITOR_CHANCE) == 0)
		{
			List<String> known = new ArrayList<>();
			for (String bank : visited)
			{
				if (VISITORS.containsKey(bank) && !bank.equals(bankKey))
				{
					known.add(bank);
				}
			}
			if (!known.isEmpty())
			{
				Collections.sort(known);
				return VISITORS.get(known.get(random.nextInt(known.size())));
			}
		}
		if (STILL_BANKERS.contains(bankKey))
		{
			return GENERIC;
		}
		return VISITORS.get(bankKey);
	}

	/** The banker's model, with a cup of tea on its head at {@code cup} (height, forward, sideways), or none if null. */
	private ModelData model(NPCComposition composition, int[] cup)
	{
		int[] ids = composition.getModels();
		if (ids == null || ids.length == 0)
		{
			return null;
		}
		List<ModelData> parts = new ArrayList<>();
		for (int id : ids)
		{
			ModelData part = client.loadModelData(id);
			if (part == null)
			{
				return null;
			}
			parts.add(part);
		}
		int cupModel = client.getItemDefinition(CUP_ITEM).getInventoryModel();
		ModelData cupData = cupModel < 0 || cup == null ? null : client.loadModelData(cupModel);
		if (cupData != null)
		{
			parts.add(cupData.cloneVertices().scale(CUP_SCALE, CUP_SCALE, CUP_SCALE).translate(cup[2], cup[0], cup[1]));
		}
		ModelData model = client.mergeModels(parts.toArray(new ModelData[0]));
		short[] from = composition.getColorToReplace();
		short[] to = composition.getColorToReplaceWith();
		if (from != null && to != null)
		{
			model = model.cloneColors();
			for (int i = 0; i < Math.min(from.length, to.length); i++)
			{
				model.recolor(from[i], to[i]);
			}
		}
		int width = composition.getWidthScale();
		int height = composition.getHeightScale();
		if (width != 128 || height != 128)
		{
			model = model.cloneVertices().scale(width, height, width);
		}
		return model;
	}

	/**
	 * Finds a way in: from a tile a few steps away (on the side of the real banker, if there
	 * is one) to the tile beside you. False if there's no room to walk.
	 */
	private boolean plan(Player player, NPC local)
	{
		path.clear();
		WorldView view = player.getWorldView();
		LocalPoint me = player.getLocalLocation();
		if (view == null || !view.isTopLevel() || me == null)
		{
			return false;
		}
		CollisionData[] maps = view.getCollisionMaps();
		int plane = view.getPlane();
		if (maps == null || maps[plane] == null)
		{
			return false;
		}
		int[][] flags = maps[plane].getFlags();
		int size = flags.length;
		int sx = me.getSceneX();
		int sy = me.getSceneY();

		// Steps from the player, over walkable tiles.
		int[][] distance = new int[size][size];
		for (int[] row : distance)
		{
			Arrays.fill(row, -1);
		}
		int[][] towards = new int[size][size];
		Deque<int[]> queue = new ArrayDeque<>();
		distance[sx][sy] = 0;
		queue.add(new int[]{sx, sy});
		List<int[]> far = new ArrayList<>();
		int[][] moves = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
		while (!queue.isEmpty())
		{
			int[] at = queue.poll();
			int d = distance[at[0]][at[1]];
			if (d == START_STEPS)
			{
				far.add(at);
				continue;
			}
			for (int m = 0; m < moves.length; m++)
			{
				int nx = at[0] + moves[m][0];
				int ny = at[1] + moves[m][1];
				if (nx < 1 || ny < 1 || nx >= size - 1 || ny >= size - 1 || distance[nx][ny] != -1
					|| !canStep(flags, at[0], at[1], moves[m][0], moves[m][1]))
				{
					continue;
				}
				distance[nx][ny] = d + 1;
				towards[nx][ny] = m;
				queue.add(new int[]{nx, ny});
			}
		}
		if (far.isEmpty())
		{
			return false;
		}

		// Set off from the far tile nearest the real banker, so it looks like they came from the counter.
		int[] start = far.get(random.nextInt(far.size()));
		LocalPoint from = local == null ? null : local.getLocalLocation();
		if (from != null)
		{
			int best = Integer.MAX_VALUE;
			for (int[] tile : far)
			{
				int dx = tile[0] - from.getSceneX();
				int dy = tile[1] - from.getSceneY();
				if (dx * dx + dy * dy < best)
				{
					best = dx * dx + dy * dy;
					start = tile;
				}
			}
		}

		// Walk back along the search from the start to the tile next to the player.
		List<int[]> tiles = new ArrayList<>();
		int cx = start[0];
		int cy = start[1];
		while (distance[cx][cy] > 1)
		{
			tiles.add(new int[]{cx, cy});
			int[] move = moves[towards[cx][cy]];
			cx -= move[0];
			cy -= move[1];
		}
		tiles.add(new int[]{cx, cy});
		for (int[] tile : tiles)
		{
			path.add(new int[]{tile[0] * 128 + 64, tile[1] * 128 + 64});
		}
		return path.size() >= 2;
	}

	/** Whether one step from (x, y) in direction (dx, dy) is open. Diagonals need both sides open too. */
	private static boolean canStep(int[][] flags, int x, int y, int dx, int dy)
	{
		if (dx != 0 && dy != 0)
		{
			return canStep(flags, x, y, dx, 0) && canStep(flags, x, y, 0, dy)
				&& canStep(flags, x + dx, y, 0, dy) && canStep(flags, x, y + dy, dx, 0)
				&& !blocked(flags[x + dx][y + dy]);
		}
		int to = flags[x + dx][y + dy];
		if (blocked(to))
		{
			return false;
		}
		int from = flags[x][y];
		if (dy == 1)
		{
			return (from & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0 && (to & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
		}
		if (dy == -1)
		{
			return (from & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0 && (to & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
		}
		if (dx == 1)
		{
			return (from & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0 && (to & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
		}
		return (from & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0 && (to & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
	}

	private static boolean blocked(int flag)
	{
		return (flag & (CollisionDataFlag.BLOCK_MOVEMENT_FULL | CollisionDataFlag.BLOCK_MOVEMENT_OBJECT
			| CollisionDataFlag.BLOCK_MOVEMENT_FLOOR | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION)) != 0;
	}

	/** Moves the banker on; call every client tick. Client thread. */
	void frame()
	{
		if (state == State.NONE)
		{
			return;
		}
		long now = System.currentTimeMillis();
		double distance = (now - lastFrame) * SPEED;
		lastFrame = now;
		switch (state)
		{
			case ARRIVING:
			case LEAVING:
				if (walk(distance))
				{
					arrived(now);
				}
				break;
			case HANDING_OVER:
				if (now - stateStarted >= HAND_OVER_MILLIS)
				{
					Collections.reverse(path);
					step = 1;
					animate(banker.walk);
					say(LEAVING[random.nextInt(LEAVING.length)], now);
					state = State.LEAVING;
				}
				break;
			default:
				break;
		}
	}

	/** Walks along the path; true at the end of it. */
	private boolean walk(double distance)
	{
		while (distance > 0 && step < path.size())
		{
			int[] next = path.get(step);
			double dx = next[0] - x;
			double dy = next[1] - y;
			double left = Math.hypot(dx, dy);
			if (left > 0)
			{
				walker.setOrientation(facing(dx, dy));
			}
			if (left <= distance)
			{
				x = next[0];
				y = next[1];
				distance -= left;
				step++;
			}
			else
			{
				x += dx / left * distance;
				y += dy / left * distance;
				distance = 0;
			}
		}
		place();
		return step >= path.size();
	}

	private void arrived(long now)
	{
		if (state == State.ARRIVING)
		{
			Player player = client.getLocalPlayer();
			if (player != null && player.getLocalLocation() != null)
			{
				LocalPoint me = player.getLocalLocation();
				walker.setOrientation(facing(me.getX() - x, me.getY() - y));
			}
			animate(banker.stand);
			walker.setModel(emptyHanded);
			say(handOverLine, now);
			stateStarted = now;
			state = State.HANDING_OVER;
			if (onServed != null)
			{
				onServed.run();
			}
			return;
		}
		clear();
	}

	private void say(String line, long now)
	{
		speech = line;
		speechUntil = now + SPEECH_MILLIS;
	}

	/** Facing the way (dx, dy) points: 0 is south, 512 west, 1024 north, 1536 east. */
	static int facing(double dx, double dy)
	{
		return (int) Math.round(Math.atan2(-dx, -dy) / (2 * Math.PI) * 2048) & 2047;
	}

	private void place()
	{
		WorldView view = client.getTopLevelWorldView();
		walker.setLocation(new LocalPoint((int) Math.round(x), (int) Math.round(y), view), plane);
	}

	private void animate(int animation)
	{
		AnimationController controller = new AnimationController(client, client.loadAnimation(animation));
		controller.setOnFinished(AnimationController::loop);
		walker.setAnimationController(controller);
	}

	// The dev panel's cup tuner: a banker turning slowly beside you with the cup where you put it.
	private RuneLiteObject tuner;
	/** Which way the tuner's banker faces; it starts facing you. */
	private int tunerFacing = 1024;

	/** The tuner's banker choices: the classic banker, then each visitor by bank. */
	static List<String> tunerChoices()
	{
		List<String> choices = new ArrayList<>();
		choices.add(CLASSIC);
		choices.addAll(visitorBanks());
		return choices;
	}

	/** Where the cup sits on this banker now: {height, forward, sideways}. */
	static int[] cupOf(String choice)
	{
		Banker banker = CLASSIC.equals(choice) ? GENERIC : VISITORS.get(choice);
		return banker == null ? new int[]{CUP_HEIGHT, CUP_FORWARD, 0} : banker.cup.clone();
	}

	/** Shows or updates the tuner: {@code choice}'s banker two tiles south of you with the cup at {@code cup}. Client thread. */
	void tune(String choice, int[] cup)
	{
		Banker banker = CLASSIC.equals(choice) ? GENERIC : VISITORS.get(choice);
		Player player = client.getLocalPlayer();
		NPCComposition composition = banker == null ? null : client.getNpcDefinition(banker.npc);
		ModelData model = composition == null ? null : model(composition, cup);
		if (player == null || player.getLocalLocation() == null || model == null)
		{
			return;
		}
		if (tuner == null)
		{
			tuner = client.createRuneLiteObject();
		}
		tuner.setOrientation(tunerFacing);
		tuner.setModel(model.light());
		AnimationController controller = new AnimationController(client, client.loadAnimation(banker.stand));
		controller.setOnFinished(AnimationController::loop);
		tuner.setAnimationController(controller);
		LocalPoint me = player.getLocalLocation();
		tuner.setLocation(new LocalPoint(me.getX(), me.getY() - 256, client.getTopLevelWorldView()),
			player.getWorldLocation().getPlane());
		tuner.setActive(true);
	}

	/** Turns the tuner's banker a quarter turn, to see the cup from the side. */
	void turnTuner()
	{
		tunerFacing = (tunerFacing + 512) & 2047;
		if (tuner != null)
		{
			tuner.setOrientation(tunerFacing);
		}
	}

	void stopTuning()
	{
		if (tuner != null)
		{
			tuner.setActive(false);
			tuner = null;
		}
	}

	/** Sends the banker away at once, like when the stand ends. */
	void clear()
	{
		if (walker != null)
		{
			walker.setActive(false);
			walker = null;
		}
		state = State.NONE;
		speech = null;
		path.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (walker == null || speech == null || System.currentTimeMillis() > speechUntil)
		{
			return null;
		}
		LocalPoint at = new LocalPoint((int) Math.round(x), (int) Math.round(y), client.getTopLevelWorldView());
		graphics.setFont(FontManager.getRunescapeBoldFont());
		Point text = Perspective.getCanvasTextLocation(client, graphics, at, speech, 230);
		if (text != null)
		{
			OverlayUtil.renderTextLocation(graphics, text, speech, SPEECH);
		}
		return null;
	}
}
