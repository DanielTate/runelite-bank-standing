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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The collection log: rare cosmetic drops that can come with any XP roll, and the banks you've
 * stood at. Drops are flavour only; they never change XP.
 */
public final class CollectionLog
{
	/** Which entry of the log a drop is listed under. */
	public enum Group
	{
		RARE("Rare drops"),
		OUTFIT("Banker's outfit"),
		JUNK("Pocket junk"),
		SEASONAL("Seasonal"),
		PET("Pets");

		public final String title;

		Group(String title)
		{
			this.title = title;
		}
	}

	/** One thing to collect. Rates are one in {@code rate} XP rolls. */
	public static final class Drop
	{
		public final String key;
		public final String name;
		/** The item whose icon it uses. */
		public final int icon;
		public final int rate;
		/** Whether the icon is shown turned gold. */
		public final boolean gold;
		public final boolean pet;
		public final Group group;
		/** The holiday it only drops during, or null. */
		public final Holiday holiday;

		Drop(String key, String name, int icon, int rate, boolean gold, boolean pet)
		{
			this(key, name, icon, rate, gold, pet ? Group.PET : Group.RARE, null);
		}

		Drop(String key, String name, int icon, int rate, boolean gold, Group group, Holiday holiday)
		{
			this.key = key;
			this.name = name;
			this.icon = icon;
			this.rate = rate;
			this.gold = gold;
			this.pet = group == Group.PET;
			this.group = group;
			this.holiday = holiday;
		}

		public boolean rare()
		{
			return group == Group.RARE || group == Group.OUTFIT || group == Group.PET;
		}
	}

	private static final int SEASONAL_RATE = 2_000;

	/** In log order within each group. About 5.3 million rolls reach 99. */
	public static final List<Drop> DROPS = Collections.unmodifiableList(Arrays.asList(
		new Drop("lost-bank-pin", "Lost bank PIN", 970, 50_000, false, false),
		new Drop("bankers-quill", "Banker's quill", 21260, 250_000, false, false),
		new Drop("forgotten-note", "Forgotten note", 28767, 1_000_000, false, false),
		new Drop("gold-leaf", "Gold leaf", 8784, 2_500_000, false, false),
		new Drop("golden-booth-key", "Golden booth key", 989, 5_000_000, true, false),
		new Drop("bankers-hat", "Banker's hat", 12432, 150_000, false, Group.OUTFIT, null),
		new Drop("bankers-waistcoat", "Banker's waistcoat", 24798, 200_000, false, Group.OUTFIT, null),
		new Drop("bankers-trousers", "Banker's trousers", 5036, 200_000, false, Group.OUTFIT, null),
		new Drop("bankers-shoes", "Banker's shoes", 6790, 150_000, false, Group.OUTFIT, null),
		new Drop("bankers-monocle", "Banker's monocle", 12353, 400_000, false, Group.OUTFIT, null),
		new Drop("pocket-lint", "Pocket lint", 1759, 500, false, Group.JUNK, null),
		new Drop("crumpled-receipt", "Crumpled receipt", 4603, 1_000, false, Group.JUNK, null),
		new Drop("stale-bread", "Stale bread", 2309, 1_500, false, Group.JUNK, null),
		new Drop("loose-button", "Loose button", 688, 2_500, false, Group.JUNK, null),
		new Drop("lost-earring", "Lost earring", 1635, 4_000, false, Group.JUNK, null),
		new Drop("bank-queue-ticket", "Bank queue ticket", 2996, 5_000, false, Group.JUNK, null),
		new Drop("cracker-crumb", "Cracker crumb", 962, SEASONAL_RATE, false, Group.SEASONAL, Holiday.CHRISTMAS),
		new Drop("cobweb", "Cobweb", 28601, SEASONAL_RATE, false, Group.SEASONAL, Holiday.HALLOWEEN),
		new Drop("chocolate-coin", "Chocolate coin", 1973, SEASONAL_RATE, false, Group.SEASONAL, Holiday.EASTER),
		new Drop("party-balloon", "Party balloon", 9937, SEASONAL_RATE, false, Group.SEASONAL, Holiday.BIRTHDAY),
		new Drop("singed-note", "Singed note", 20718, SEASONAL_RATE, false, Group.SEASONAL,
			Holiday.FALADOR_MASSACRE),
		new Drop("pet-moneybag", "Pet moneybag", 22533, 10_000_000, false, true),
		new Drop("bank-rat", "Bank rat", 523, 25_000_000, false, true)
	));

	/** Rarest first, which is the order drops are rolled in. */
	private static final List<Drop> BY_RARITY;

	static
	{
		List<Drop> sorted = new ArrayList<>(DROPS);
		sorted.sort((a, b) -> Integer.compare(b.rate, a.rate));
		BY_RARITY = Collections.unmodifiableList(sorted);
	}

	/** What's saved for each drop you've had. */
	public static final class Obtained
	{
		public int count;
		/** When the first one dropped, in epoch milliseconds. */
		public long firstAt;
		/** How many rolls it took to get the first one. */
		public long firstRolls;
	}

	private CollectionLog()
	{
	}

	/**
	 * Rolls for a drop alongside one XP roll; the rarest is checked first and at most one
	 * drops. Seasonal drops only roll on their holiday (UTC).
	 */
	public static Drop roll(Random random, LocalDate today)
	{
		for (Drop drop : BY_RARITY)
		{
			if ((drop.holiday == null || drop.holiday.isOn(today)) && random.nextInt(drop.rate) == 0)
			{
				return drop;
			}
		}
		return null;
	}

	public static Drop find(String key)
	{
		for (Drop drop : DROPS)
		{
			if (drop.key.equals(key))
			{
				return drop;
			}
		}
		return null;
	}

	/** Adds a drop to the log; true if it's the first one. */
	public static boolean collect(Progress progress, Drop drop, long now)
	{
		Obtained obtained = progress.collection.get(drop.key);
		boolean first = obtained == null;
		if (first)
		{
			obtained = new Obtained();
			obtained.firstAt = now;
			obtained.firstRolls = progress.rolls;
			progress.collection.put(drop.key, obtained);
		}
		obtained.count++;
		return first;
	}

	/** Whether the moneybag, the pet that sits beside you, has dropped. */
	public static boolean hasPet(Progress progress)
	{
		return progress.collection.containsKey("pet-moneybag");
	}

	/** How many of the named banks you've stood at. */
	public static int banksVisited(Progress progress)
	{
		int visited = 0;
		for (BankArea area : BankNames.all())
		{
			if (progress.banks.containsKey(area.getKey()))
			{
				visited++;
			}
		}
		return visited;
	}
}
