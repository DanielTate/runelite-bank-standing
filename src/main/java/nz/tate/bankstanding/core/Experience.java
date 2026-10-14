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
 * The real OSRS experience table. Bank Standing levels exactly like a real skill: 99 at
 * 13,034,431 XP, and XP keeps counting past 99 up to the 200m cap.
 */
public final class Experience
{
	public static final int MAX_LEVEL = 99;
	public static final int MAX_XP = 200_000_000;

	/** XP_FOR_LEVEL[n] is the XP needed to reach level n; index 0 is unused. */
	private static final int[] XP_FOR_LEVEL = new int[MAX_LEVEL + 1];

	static
	{
		int points = 0;
		XP_FOR_LEVEL[1] = 0;
		for (int level = 1; level < MAX_LEVEL; level++)
		{
			points += (int) Math.floor(level + 300 * Math.pow(2, level / 7.0));
			XP_FOR_LEVEL[level + 1] = points / 4;
		}
	}

	private Experience()
	{
	}

	public static int xpForLevel(int level)
	{
		if (level < 1 || level > MAX_LEVEL)
		{
			throw new IllegalArgumentException("level " + level);
		}
		return XP_FOR_LEVEL[level];
	}

	public static int levelForXp(long xp)
	{
		int level = 1;
		while (level < MAX_LEVEL && xp >= XP_FOR_LEVEL[level + 1])
		{
			level++;
		}
		return level;
	}

	/** Adds a drop without passing the 200m cap. */
	public static int addXp(int xp, int drop)
	{
		return (int) Math.min(MAX_XP, (long) xp + drop);
	}
}
