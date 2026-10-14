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
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.SpotanimID;

/**
 * The 99 celebration: fireworks, a meteor shower around the player, then a ring of
 * explosions. Built from the game's own effects as projectiles on this client only; nobody
 * else sees it. Driven by {@link #tick()} every client tick (20ms).
 */
@Singleton
class Celebration
{
	private static final int[] METEORS = {SpotanimID.WILD_FALLOFF_METEOR_FLYING, SpotanimID.INFERNO_ZUK_PROJECTILE};
	private static final int[] BLASTS = {SpotanimID.WILD_FALLOFF_METEOR_BLAST, SpotanimID.WILD_FALLOFF_METEOR_BLAST_RED};
	private static final int RING_BLAST = SpotanimID.OLM_CRYSTAL_EXPLODE;
	private static final int FIREWORKS_KEY = 0x42414E4D;

	/** Client cycles are 20ms. */
	private static final int SHOWER_CYCLES = 300;
	private static final int METEOR_EVERY = 12;
	private static final int FALL_CYCLES = 55;
	private static final int BLAST_CYCLES = 60;
	private static final int FINALE_AT = SHOWER_CYCLES + FALL_CYCLES;
	private static final int LENGTH = FINALE_AT + 120;
	private static final int SKY_HEIGHT = 1800;
	private static final int SPREAD = 5;
	private static final int RING_RADIUS = 4;

	private final Client client;
	private final Random random = new Random();
	private final List<int[]> pendingBlasts = new ArrayList<>();
	private int startCycle = -1;
	private int lastMeteor;
	private boolean finaleDone;

	@Inject
	Celebration(Client client)
	{
		this.client = client;
	}

	void start()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		startCycle = client.getGameCycle();
		lastMeteor = startCycle - METEOR_EVERY;
		finaleDone = false;
		pendingBlasts.clear();
		player.createSpotAnim(FIREWORKS_KEY, SpotanimID.LEVELUP_99_ANIM, 0, 0);
	}

	boolean isRunning()
	{
		return startCycle >= 0;
	}

	void stop()
	{
		startCycle = -1;
		pendingBlasts.clear();
	}

	void tick()
	{
		Player player = client.getLocalPlayer();
		if (startCycle < 0 || player == null)
		{
			return;
		}
		int now = client.getGameCycle();
		int elapsed = now - startCycle;
		WorldPoint centre = player.getWorldLocation();

		if (elapsed < SHOWER_CYCLES && now - lastMeteor >= METEOR_EVERY)
		{
			lastMeteor = now;
			meteor(centre, now);
		}

		for (Iterator<int[]> it = pendingBlasts.iterator(); it.hasNext(); )
		{
			int[] blast = it.next();
			if (now >= blast[0])
			{
				it.remove();
				stationary(blast[3], new WorldPoint(blast[1], blast[2], centre.getPlane()), now, BLAST_CYCLES);
			}
		}

		if (!finaleDone && elapsed >= FINALE_AT)
		{
			finaleDone = true;
			player.createSpotAnim(FIREWORKS_KEY, SpotanimID.LEVELUP_MAX, 0, 0);
			for (int i = 0; i < 12; i++)
			{
				double angle = Math.PI * 2 * i / 12;
				WorldPoint at = centre.dx((int) Math.round(Math.cos(angle) * RING_RADIUS))
					.dy((int) Math.round(Math.sin(angle) * RING_RADIUS));
				stationary(RING_BLAST, at, now + i * 3, BLAST_CYCLES);
			}
		}

		if (elapsed >= LENGTH)
		{
			stop();
		}
	}

	private void meteor(WorldPoint centre, int now)
	{
		int dx = random.nextInt(SPREAD * 2 + 1) - SPREAD;
		int dy = random.nextInt(SPREAD * 2 + 1) - SPREAD;
		if (dx == 0 && dy == 0)
		{
			dx = 1;
		}
		WorldPoint land = centre.dx(dx).dy(dy);
		WorldPoint sky = land.dx(random.nextInt(5) - 2).dy(random.nextInt(5) - 2);
		int pick = random.nextInt(METEORS.length);
		spawn(client.createProjectile(METEORS[pick], sky, SKY_HEIGHT, null, land, 0, null,
			now, now + FALL_CYCLES, 0, 0));
		pendingBlasts.add(new int[]{now + FALL_CYCLES, land.getX(), land.getY(), BLASTS[pick]});
	}

	/** A projectile that starts and ends on the same tile just plays its effect there. */
	private void stationary(int id, WorldPoint at, int start, int length)
	{
		spawn(client.createProjectile(id, at, 0, null, at, 0, null, start, start + length, 0, 0));
	}

	private void spawn(Projectile projectile)
	{
		if (projectile == null)
		{
			return;
		}
		for (Projectile existing : client.getProjectiles())
		{
			if (existing == projectile)
			{
				return;
			}
		}
		client.getProjectiles().addLast(projectile);
	}
}
