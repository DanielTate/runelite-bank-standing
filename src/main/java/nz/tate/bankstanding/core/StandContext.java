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

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

/** A snapshot of the player at the moment a drop is rolled. */
public final class StandContext
{
	/** Item IDs currently worn. */
	public Set<Integer> worn = Collections.emptySet();
	/** GE value of everything worn. */
	public long wornValue;
	public int hitpoints = 99;
	public String bankKey = "";
	/** Today on your own clock, like every other day in the plugin. */
	public LocalDate today = LocalDate.of(2000, 1, 1);
	public Emote emote = Emote.NONE;
	public boolean follower;
	/** The follower's name in lower case, or null for none. */
	public String followerName;
	/** Showing a PK skull. */
	public boolean skulled;
	public int streakDays;
	public double hoursAtBank;
	/** Item IDs in each inventory slot, -1 for an empty slot. */
	public int[] inventory = new int[0];

	boolean wearing(int id)
	{
		return worn.contains(id);
	}

	boolean wearingAny(Set<Integer> ids)
	{
		for (int id : ids)
		{
			if (worn.contains(id))
			{
				return true;
			}
		}
		return false;
	}

	boolean at(String... keys)
	{
		for (String key : keys)
		{
			if (key.equals(bankKey))
			{
				return true;
			}
		}
		return false;
	}
}
