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
import java.util.Set;

/** A full set: one item from every piece must be worn. */
final class Outfit
{
	private final String[] bankKeys;
	private final List<Set<Integer>> pieces = new ArrayList<>();
	/** Pieces that count worn or just carried, like a book that isn't wielded. */
	private final List<Set<Integer>> carried = new ArrayList<>();
	private String name;
	private int icon = -1;
	private boolean zeal;

	Outfit(String... bankKeys)
	{
		this.bankKeys = bankKeys;
	}

	Outfit piece(Integer... ids)
	{
		pieces.add(Items.ids(ids));
		return this;
	}

	/** One item from these, worn or in the inventory. */
	Outfit carried(Integer... ids)
	{
		carried.add(Items.ids(ids));
		return this;
	}

	/** Its own key, for a bank with more than one set. */
	Outfit named(String name)
	{
		this.name = name;
		return this;
	}

	Outfit icon(int item)
	{
		icon = item;
		return this;
	}

	/** Counts only with the Zeal prayer unlocked. */
	Outfit zeal()
	{
		zeal = true;
		return this;
	}

	String[] bankKeys()
	{
		return bankKeys;
	}

	/** An item from its first piece, to show for it. */
	int icon()
	{
		if (icon != -1)
		{
			return icon;
		}
		return Collections.min(pieces.isEmpty() ? carried.get(0) : pieces.get(0));
	}

	/** Saved in the collection log, so it must never change: its name, or else the first bank's key. */
	String key()
	{
		return name != null ? name : bankKeys[0];
	}

	boolean wornBy(StandContext context)
	{
		for (Set<Integer> piece : pieces)
		{
			if (!context.wearingAny(piece))
			{
				return false;
			}
		}
		for (Set<Integer> piece : carried)
		{
			if (!context.wearingAny(piece) && !context.carryingAny(piece))
			{
				return false;
			}
		}
		return !zeal || context.zealUnlocked;
	}
}
