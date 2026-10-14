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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What each level unlocks, for the skill guide. The main unlock is reach: how far from a bank
 * you can stand and still be bank standing.
 */
public final class Unlocks
{
	/** One tile at level 1, then one more every ten levels from 20 to 80, and 9 at 99. */
	public static int bankRange(int level)
	{
		if (level >= Experience.MAX_LEVEL)
		{
			return 9;
		}
		return Math.max(1, Math.min(8, level / 10));
	}

	/** One line of the skill guide. */
	public static final class Unlock
	{
		public final int level;
		public final String text;

		Unlock(int level, String text)
		{
			this.level = level;
			this.text = text;
		}
	}

	public static final List<Unlock> ALL;

	static
	{
		List<Unlock> all = new ArrayList<>();
		all.add(new Unlock(1, "Bank stand within 1 tile of a bank"));
		for (int level = 20; level <= 80; level += 10)
		{
			all.add(new Unlock(level, "Bank stand within " + bankRange(level) + " tiles"));
		}
		all.add(new Unlock(Experience.MAX_LEVEL, "Bank stand within " + bankRange(Experience.MAX_LEVEL) + " tiles"));
		all.add(new Unlock(Experience.MAX_LEVEL, "Bank Standing cape, worn while you stand"));
		ALL = Collections.unmodifiableList(all);
	}

	private Unlocks()
	{
	}

	/** The next level that unlocks something, or -1 at 99. */
	public static int nextUnlockLevel(int level)
	{
		for (Unlock unlock : ALL)
		{
			if (unlock.level > level)
			{
				return unlock.level;
			}
		}
		return -1;
	}
}
