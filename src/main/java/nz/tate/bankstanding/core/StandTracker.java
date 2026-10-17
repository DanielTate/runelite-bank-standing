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
 * using the bank, for the start delay ({@link #START_DELAY_TICKS} until levels shorten it). From then on a drop is rolled every
 * {@link XpRoller#ROLL_INTERVAL_TICKS}. Walking, using the bank, leaving range or gaining
 * real XP ends the stand and the delay starts again from zero. Chatting, emotes, camera and
 * tab use never reach this class, so they never interrupt a stand.
 */
public final class StandTracker
{
	/** 30 seconds of standing still before the first drop. */
	public static final int START_DELAY_TICKS = 50;

	private int startDelayTicks = START_DELAY_TICKS;
	private int peekTicks;
	private int idleTicks;
	private int bankTicks;
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

		if (moved || !nearBank)
		{
			idleTicks = 0;
			bankTicks = 0;
			return false;
		}
		if (usingBank)
		{
			// Peeking: a short look in the bank pauses the stand instead of ending it.
			if (++bankTicks > peekTicks)
			{
				idleTicks = 0;
			}
			return false;
		}
		bankTicks = 0;

		idleTicks++;
		int standing = idleTicks - startDelayTicks;
		return standing > 0 && standing % XpRoller.ROLL_INTERVAL_TICKS == 0;
	}

	/** Gaining XP in any real skill ends the stand. */
	public void realXpGained()
	{
		idleTicks = 0;
	}

	/** Sets the level unlocks: the start delay and how long the bank may be open. */
	public void setUnlocks(int startDelayTicks, int peekTicks)
	{
		this.startDelayTicks = startDelayTicks;
		this.peekTicks = peekTicks;
	}

	/** Logging out, hopping or loading a new area ends the stand and forgets the position. */
	public void reset()
	{
		idleTicks = 0;
		bankTicks = 0;
		hasPosition = false;
	}

	/** While a peek is holding the stand open: ticks left before it ends, else 0. */
	public int peekTicksLeft()
	{
		return bankTicks > 0 && isStanding() ? peekTicks - bankTicks + 1 : 0;
	}

	/** True while standing still in range of a bank, including the wait before the first drop. */
	public boolean isIdleAtBank()
	{
		return idleTicks > 0;
	}

	/** True once the start delay has passed and drops are being rolled. */
	public boolean isStanding()
	{
		return idleTicks > startDelayTicks;
	}
}
