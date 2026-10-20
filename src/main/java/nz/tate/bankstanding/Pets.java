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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.ModelData;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * The collection log's pets, beside you while you stand: the moneybag, which hops around you
 * now and then, and the bank rat, which scurries off for a look around and comes back. Drawn
 * in this client only. Client thread.
 */
@Singleton
class Pets
{
	/** The moneybag is the coin pouch item's own model, made bigger. */
	private static final int MONEYBAG_ITEM = 22533;
	private static final int MONEYBAG_SCALE = 256;
	/**
	 * The bank rat is the crystalline rat, turned to gold, with its own animations: NPC, stand,
	 * walk. (Shrunk models, like Scurry, the Scurrius pet, stretch out of shape when animated.)
	 */
	private static final int[] RAT = {9026, 4932, 4931};
	/** Gold, as the game writes colours: hue 8, saturation 6, with each face keeping its own lightness. */
	private static final int GOLD_HUE_SATURATION = 8 << 10 | 6 << 7;
	/**
	 * Rats the dev panel can try instead, all turned gold: name, NPC, stand and walk animations. Only ones the
	 * game doesn't shrink, since shrinking stretches their animations out of shape.
	 */
	static final Object[][] DEV_RATS = {
		{"Crystalline rat (gold)", 9026, 4932, 4931},
		{"Rat", 2854, 2704, 2703},
		{"Rat (Shield of Arrav)", 2492, 2704, 2703},
		{"Crypt rat", 1679, 242, 239},
		{"Giant rat", 2856, 4932, 4931},
		{"Giant rat (grey)", 2859, 4932, 4931},
		{"Giant rat (grey 2)", 2860, 4932, 4931},
		{"Giant rat (grey 3)", 2861, 4932, 4931},
		{"Dungeon rat", 2865, 4932, 4931},
		{"Giant rat (Scurrius)", 7223, 4932, 4931},
		{"Corrupted rat", 9040, 4932, 4931},
	};
	/** Dev panel only: the rat to show, as {NPC, stand, walk}, or null for the usual one. */
	int[] devRat;

	/** One hop from tile to tile, and how high it goes, in local units. */
	private static final long HOP_MILLIS = 350;
	private static final int HOP_HEIGHT = 60;
	/** The rat runs two tiles a game tick, in local units per millisecond. */
	private static final double RAT_SPEED = 256 / 600.0;
	/** The gold butterfly is the game's butterfly, turned gold, flapping as it flies. */
	private static final int BUTTERFLY_NPC = 234;
	private static final int BUTTERFLY_FLAP = 362;
	/** One lap round you, how far out, how far it drifts in and out, and its height, in local units. */
	private static final long BUTTERFLY_LAP_MILLIS = 9_000;
	private static final int BUTTERFLY_RADIUS = 170;
	private static final int BUTTERFLY_DRIFT = 40;
	private static final int BUTTERFLY_HEIGHT = 130;
	private static final int BUTTERFLY_BOB = 20;

	/** A single coin: the rat drops them as it runs, the moneybag throws them as it hops. */
	private static final int COIN = 995;
	/** What else falls out of the rat now and then: gold and jewelled rings and necklaces. */
	private static final int[] JEWELLERY = {1635, 1637, 1641, 1643, 1654, 1658, 1662, 1692};
	/** One drop in this many from the rat is jewellery rather than a coin. */
	private static final int JEWELLERY_CHANCE = 4;
	/** How far the rat runs from you, in tiles: past the rug's edge. */
	private static final int RAT_RANGE = 4;
	/** How long a coin lies there, and the most there can be at once. */
	private static final long COIN_MILLIS = 15_000;
	private static final int MAX_COINS = 60;
	/** A thrown coin's flight, and how high it goes, in local units. */
	private static final long THROW_MILLIS = 600;
	private static final int THROW_HEIGHT = 90;
	/** The rat drops something every this many local units it runs. */
	private static final int TRAIL_SPACING = 48;

	/** The tiles around you, clockwise from the east, that the moneybag hops round. */
	private static final int[][] RING = {{1, 0}, {1, -1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}};

	/** One pet: its object, its home tile beside you, and where it's going. */
	private static final class Pet
	{
		RuneLiteObject object;
		int homeDx;
		int homeDy;
		/** Points to move through, in local units, and how far along. */
		final List<int[]> route = new ArrayList<>();
		int step;
		double x;
		double y;
		/** When the current hop began. */
		long legStarted;
		long nextOuting;
		long pauseUntil;
		int stand;
		int walk;
		/** Whether this hop's coin has been thrown. */
		boolean thrown;
	}

	/** A coin on the ground or in the air. */
	private static final class Coin
	{
		RuneLiteObject object;
		double fromX;
		double fromY;
		double toX;
		double toY;
		long thrownAt;
		/** 0 for a coin just dropped, not thrown. */
		long flight;
		long goneAt;
	}

	private final Client client;
	private final Random random = new Random();
	private final List<Coin> coins = new ArrayList<>();
	/** How far the rat has run since its last coin. */
	private double sinceCoin;
	private Pet moneybag;
	private Pet rat;
	private Pet butterfly;
	private int plane;
	private long lastFrame;

	@Inject
	Pets(Client client)
	{
		this.client = client;
	}

	/** Called every game tick with which pets to show. */
	void tick(Player player, boolean showMoneybag, boolean showRat, boolean showButterfly)
	{
		plane = player.getWorldLocation().getPlane();
		if (showMoneybag && moneybag == null)
		{
			moneybag = placeMoneybag(player);
		}
		else if (!showMoneybag)
		{
			moneybag = remove(moneybag);
		}
		if (showButterfly && butterfly == null)
		{
			butterfly = placeButterfly(player);
		}
		else if (!showButterfly)
		{
			butterfly = remove(butterfly);
		}
		if (showRat && rat == null)
		{
			rat = placeRat(player);
		}
		else if (!showRat)
		{
			rat = remove(rat);
		}
		if (moneybag != null && moneybag.route.isEmpty())
		{
			moneybag.object.setOrientation(player.getOrientation());
		}
	}

	private Pet placeMoneybag(Player player)
	{
		ModelData data = StandCosmetics.itemModel(client, MONEYBAG_ITEM);
		if (data == null)
		{
			return null;
		}
		return place(player, data.scale(MONEYBAG_SCALE, MONEYBAG_SCALE, MONEYBAG_SCALE), 1, 0);
	}

	private Pet placeRat(Player player)
	{
		int[] rat = devRat != null ? devRat : RAT;
		ModelData model = npcModel(rat[0]);
		if (model == null)
		{
			return null;
		}
		Pet pet = place(player, gilded(model), -1, 0);
		if (pet != null)
		{
			pet.stand = rat[1];
			pet.walk = rat[2];
			animate(pet, pet.stand);
		}
		return pet;
	}

	private Pet placeButterfly(Player player)
	{
		ModelData model = npcModel(BUTTERFLY_NPC);
		if (model == null)
		{
			return null;
		}
		Pet pet = place(player, gilded(model), 0, 1);
		if (pet != null)
		{
			animate(pet, BUTTERFLY_FLAP);
		}
		return pet;
	}

	/** An NPC's model with its own colours, or null if it won't load. */
	private ModelData npcModel(int npc)
	{
		NPCComposition composition = client.getNpcDefinition(npc);
		int[] ids = composition == null ? null : composition.getModels();
		if (ids == null || ids.length == 0)
		{
			return null;
		}
		ModelData[] parts = new ModelData[ids.length];
		for (int i = 0; i < ids.length; i++)
		{
			parts[i] = client.loadModelData(ids[i]);
			if (parts[i] == null)
			{
				return null;
			}
		}
		ModelData model = client.mergeModels(parts);
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
		return model;
	}

	/** The model in solid gold: every face gold at its own lightness, with textures taken off. */
	private static ModelData gilded(ModelData model)
	{
		// Models without textures have no texture list to copy.
		ModelData gold = model.getFaceTextures() == null ? model.cloneColors() : model.cloneColors().cloneTextures();
		short[] colours = gold.getFaceColors();
		short[] textures = gold.getFaceTextures();
		for (int i = 0; i < colours.length; i++)
		{
			colours[i] = (short) (GOLD_HUE_SATURATION | colours[i] & 127);
			if (textures != null)
			{
				textures[i] = -1;
			}
		}
		return gold;
	}

	private Pet place(Player player, ModelData model, int dx, int dy)
	{
		LocalPoint me = player.getLocalLocation();
		if (me == null)
		{
			return null;
		}
		Pet pet = new Pet();
		pet.homeDx = dx;
		pet.homeDy = dy;
		pet.x = me.getX() + dx * 128;
		pet.y = me.getY() + dy * 128;
		pet.object = client.createRuneLiteObject();
		pet.object.setModel(model.light());
		pet.object.setOrientation(player.getOrientation());
		pet.nextOuting = System.currentTimeMillis() + 5_000 + random.nextInt(15_000);
		move(pet, 0);
		pet.object.setActive(true);
		return pet;
	}

	private Pet remove(Pet pet)
	{
		if (pet != null)
		{
			pet.object.setActive(false);
		}
		return null;
	}

	void clear()
	{
		moneybag = remove(moneybag);
		rat = remove(rat);
		butterfly = remove(butterfly);
		for (Coin coin : coins)
		{
			coin.object.setActive(false);
		}
		coins.clear();
	}

	/** Dev panel only: shows a different rat, swapping it in straight away. */
	void devRat(int[] rat)
	{
		devRat = rat;
		this.rat = remove(this.rat);
	}

	/** Moves the pets on; call every client tick. */
	void frame()
	{
		Player player = client.getLocalPlayer();
		LocalPoint me = player == null ? null : player.getLocalLocation();
		long now = System.currentTimeMillis();
		long elapsed = lastFrame == 0 ? 0 : now - lastFrame;
		lastFrame = now;
		if (me == null)
		{
			return;
		}
		if (moneybag != null)
		{
			hop(moneybag, me, now);
		}
		if (rat != null)
		{
			scurry(rat, me, now, elapsed);
		}
		if (butterfly != null)
		{
			flutter(butterfly, me, now);
		}
		moveCoins(now);
	}

	/** Now and then the moneybag hops once round you, tile by tile, and settles back in its spot. */
	private void hop(Pet pet, LocalPoint me, long now)
	{
		if (pet.route.isEmpty())
		{
			if (now < pet.nextOuting)
			{
				return;
			}
			int start = 0;
			for (int i = 0; i <= RING.length; i++)
			{
				int[] tile = RING[(start + i) % RING.length];
				pet.route.add(new int[]{me.getX() + tile[0] * 128, me.getY() + tile[1] * 128});
			}
			pet.step = 1;
			pet.legStarted = now;
		}
		int[] from = pet.route.get(pet.step - 1);
		int[] to = pet.route.get(pet.step);
		double t = Math.min(1, (now - pet.legStarted) / (double) HOP_MILLIS);
		pet.x = from[0] + (to[0] - from[0]) * t;
		pet.y = from[1] + (to[1] - from[1]) * t;
		pet.object.setOrientation(TeaDelivery.facing(to[0] - from[0], to[1] - from[1]));
		move(pet, (int) Math.round(Math.sin(Math.PI * t) * HOP_HEIGHT));
		if (t >= 0.5 && !pet.thrown)
		{
			// A coin flung out at the top of each hop, landing a tile or two away.
			pet.thrown = true;
			double angle = random.nextDouble() * 2 * Math.PI;
			double reach = 128 + random.nextInt(160);
			throwCoin(pet.x, pet.y, me.getX() + Math.cos(angle) * reach, me.getY() + Math.sin(angle) * reach, COIN, now);
		}
		if (t >= 1)
		{
			pet.thrown = false;
			pet.step++;
			pet.legStarted = now;
			if (pet.step >= pet.route.size())
			{
				pet.route.clear();
				pet.nextOuting = now + 20_000 + random.nextInt(40_000);
			}
		}
	}

	/** Now and then the rat runs off to a nearby tile, has a look round, and runs back. */
	private void scurry(Pet pet, LocalPoint me, long now, long elapsed)
	{
		if (pet.route.isEmpty())
		{
			if (now < pet.nextOuting)
			{
				return;
			}
			int dx;
			int dy;
			do
			{
				dx = random.nextInt(RAT_RANGE * 2 + 1) - RAT_RANGE;
				dy = random.nextInt(RAT_RANGE * 2 + 1) - RAT_RANGE;
			}
			while (dx == 0 && dy == 0 || dx == pet.homeDx && dy == pet.homeDy);
			int homeX = me.getX() + pet.homeDx * 128;
			int homeY = me.getY() + pet.homeDy * 128;
			pet.route.add(new int[]{homeX, homeY});
			pet.route.add(new int[]{me.getX() + dx * 128, me.getY() + dy * 128});
			pet.route.add(new int[]{homeX, homeY});
			pet.step = 1;
			animate(pet, pet.walk);
		}
		if (now < pet.pauseUntil)
		{
			return;
		}
		int[] to = pet.route.get(pet.step);
		double dx = to[0] - pet.x;
		double dy = to[1] - pet.y;
		double left = Math.hypot(dx, dy);
		double distance = elapsed * RAT_SPEED;
		if (left > 0)
		{
			pet.object.setOrientation(TeaDelivery.facing(dx, dy));
		}
		if (left <= distance)
		{
			pet.x = to[0];
			pet.y = to[1];
			pet.step++;
			if (pet.step >= pet.route.size())
			{
				pet.route.clear();
				animate(pet, pet.stand);
				pet.nextOuting = now + 15_000 + random.nextInt(30_000);
			}
			else
			{
				// A sniff round before heading back.
				animate(pet, pet.stand);
				pet.pauseUntil = now + 1_000 + random.nextInt(1_500);
			}
		}
		else
		{
			if (pet.pauseUntil != 0)
			{
				// Done looking round: off again.
				animate(pet, pet.walk);
				pet.pauseUntil = 0;
			}
			pet.x += dx / left * distance;
			pet.y += dy / left * distance;
			// A trail of coins behind it as it runs.
			sinceCoin += distance;
			if (sinceCoin >= TRAIL_SPACING)
			{
				sinceCoin = 0;
				dropCoin(pet.x, pet.y, random.nextInt(JEWELLERY_CHANCE) == 0
					? JEWELLERY[random.nextInt(JEWELLERY.length)] : COIN, now);
			}
		}
		move(pet, 0);
	}

	private void dropCoin(double x, double y, int item, long now)
	{
		if (throwCoin(x, y, x, y, item, now))
		{
			coins.get(coins.size() - 1).flight = 0;
		}
	}

	/** Throws an item from one spot to another; false if its model won't load. */
	private boolean throwCoin(double fromX, double fromY, double toX, double toY, int item, long now)
	{
		ModelData model = StandCosmetics.itemModel(client, item);
		if (model == null)
		{
			return false;
		}
		if (coins.size() >= MAX_COINS)
		{
			coins.remove(0).object.setActive(false);
		}
		Coin coin = new Coin();
		coin.object = client.createRuneLiteObject();
		coin.object.setModel(model.light());
		coin.object.setOrientation(random.nextInt(2048));
		coin.fromX = fromX;
		coin.fromY = fromY;
		coin.toX = toX;
		coin.toY = toY;
		coin.thrownAt = now;
		coin.flight = THROW_MILLIS;
		coin.goneAt = now + COIN_MILLIS;
		coins.add(coin);
		place(coin, fromX, fromY, 0);
		coin.object.setActive(true);
		return true;
	}

	/** Flies thrown coins along their arc, and takes away ones that have lain long enough. */
	private void moveCoins(long now)
	{
		for (Iterator<Coin> it = coins.iterator(); it.hasNext(); )
		{
			Coin coin = it.next();
			if (now >= coin.goneAt)
			{
				coin.object.setActive(false);
				it.remove();
				continue;
			}
			if (coin.flight > 0)
			{
				double t = Math.min(1, (now - coin.thrownAt) / (double) coin.flight);
				place(coin, coin.fromX + (coin.toX - coin.fromX) * t, coin.fromY + (coin.toY - coin.fromY) * t,
					(int) Math.round(Math.sin(Math.PI * t) * THROW_HEIGHT));
				if (t >= 1)
				{
					coin.flight = 0;
				}
			}
		}
	}

	private void place(Coin coin, double x, double y, int lift)
	{
		WorldView view = client.getTopLevelWorldView();
		coin.object.setLocation(new LocalPoint((int) Math.round(x), (int) Math.round(y), view), plane);
		if (lift != 0)
		{
			coin.object.setZ(coin.object.getZ() - lift);
		}
	}

	/** The butterfly circles you all the time, drifting in and out and bobbing up and down. */
	private void flutter(Pet pet, LocalPoint me, long now)
	{
		double angle = now * 2 * Math.PI / BUTTERFLY_LAP_MILLIS;
		double radius = BUTTERFLY_RADIUS + Math.sin(now / 900.0) * BUTTERFLY_DRIFT;
		pet.x = me.getX() + Math.cos(angle) * radius;
		pet.y = me.getY() + Math.sin(angle) * radius;
		// Facing along the circle, anticlockwise.
		pet.object.setOrientation(TeaDelivery.facing(-Math.sin(angle), Math.cos(angle)));
		move(pet, BUTTERFLY_HEIGHT + (int) Math.round(Math.sin(now / 250.0) * BUTTERFLY_BOB));
	}

	/** Puts the pet at its position, {@code lift} units off the ground. */
	private void move(Pet pet, int lift)
	{
		WorldView view = client.getTopLevelWorldView();
		pet.object.setLocation(new LocalPoint((int) Math.round(pet.x), (int) Math.round(pet.y), view), plane);
		if (lift != 0)
		{
			pet.object.setZ(pet.object.getZ() - lift);
		}
	}

	private void animate(Pet pet, int animation)
	{
		AnimationController controller = new AnimationController(client, client.loadAnimation(animation));
		controller.setOnFinished(AnimationController::loop);
		pet.object.setAnimationController(controller);
	}
}
