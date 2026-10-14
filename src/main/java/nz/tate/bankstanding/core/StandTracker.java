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
package nz.tate.bankstanding.core;

/**
 * Decides, tick by tick, whether the player is bank standing and when to roll a drop.
 *
 * <p>A stand begins once the player has been within range of a bank, without moving or
 * using the bank, for {@link #START_DELAY_TICKS}. From then on a drop is rolled every
 * {@link XpRoller#ROLL_INTERVAL_TICKS}. Walking, using the bank, leaving range or gaining
 * real XP ends the stand and the delay starts again from zero. Chatting, emotes, camera and
 * tab use never reach this class, so they never interrupt a stand.
 */
public final class StandTracker
{
	/** 30 seconds of standing still before the first drop. */
	public static final int START_DELAY_TICKS = 50;

	private int idleTicks;
	private boolean hasPosition;
	private int lastX;
	private int lastY;
	private int lastPlane;

	/**
	 * Advances one game tick.
	 *
	 * @param x          the player's world x
	 * @param y          the player's world y
	 * @param plane      the player's plane
	 * @param nearBank   whether a bank is within range
	 * @param usingBank  whether a bank or deposit box interface is open
	 * @return true when a drop should be rolled this tick
	 */
	public boolean tick(int x, int y, int plane, boolean nearBank, boolean usingBank)
	{
		boolean moved = hasPosition && (x != lastX || y != lastY || plane != lastPlane);
		hasPosition = true;
		lastX = x;
		lastY = y;
		lastPlane = plane;

		if (moved || !nearBank || usingBank)
		{
			idleTicks = 0;
			return false;
		}

		idleTicks++;
		int standing = idleTicks - START_DELAY_TICKS;
		return standing > 0 && standing % XpRoller.ROLL_INTERVAL_TICKS == 0;
	}

	/** Gaining XP in any real skill ends the stand. */
	public void realXpGained()
	{
		idleTicks = 0;
	}

	/** Logging out, hopping or loading a new area ends the stand and forgets the position. */
	public void reset()
	{
		idleTicks = 0;
		hasPosition = false;
	}

	/** True once the start delay has passed and drops are being rolled. */
	public boolean isStanding()
	{
		return idleTicks > START_DELAY_TICKS;
	}
}
