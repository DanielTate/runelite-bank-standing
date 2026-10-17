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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Everything saved for one account. Field names are permanent: they are the save format. */
public final class Progress
{
	public int xp;
	public Map<String, BankRecord> banks = new LinkedHashMap<>();

	/** The last day (ISO date) that counted toward the streak, and how long the streak is. */
	public String streakDate;
	public int streakDays;
	/** Standing ticks so far on {@link #todayDate}. */
	public String todayDate;
	public long todayTicks;

	/** When each keyword last paid out, and how many paid out on {@link #keywordDate}. */
	public Map<String, Long> keywordPaid = new LinkedHashMap<>();
	public String keywordDate;
	public int keywordCount;
	/** When the journal started (epoch ms); "Dressed for it" finds before then don't get a star. */
	public long journalSince;
	/** "Dressed for it" moment keys already seen in the journal. */
	public Set<String> journalRead = new TreeSet<>();
	/** What was said for each phrase when it first paid out, and where, for the journal. */
	public Map<String, Keywords.Noted> phraseNotes = new LinkedHashMap<>();

	/** Personal records and the milestone history. */
	public Records records = new Records();

	/** Collection log drops by key, and how many XP rolls there have been since it began. */
	public Map<String, CollectionLog.Obtained> collection = new LinkedHashMap<>();
	public long rolls;

	/** {@link Moments} by key, with when each first happened. */
	public Map<String, Long> moments = new LinkedHashMap<>();
	/** When the Sit emote last paid out a boosted roll, in epoch milliseconds. */
	public long lastSitRoll;
	/** Every local date (ISO) with some standing, for the calendar moments. */
	public Set<String> daysStood = new TreeSet<>();
	/** When standing on a fire last paid out a boosted roll, in epoch milliseconds. */
	public long lastFireRoll;
	/** Things done on {@link #notesDate} (an ISO date) that count toward the one-day moments. */
	public String notesDate;
	public Set<String> notes = new TreeSet<>();
	/** The day (ISO) of the last normal roll, so the first roll of a day survives a restart. */
	public String firstRollDate;

	public int level()
	{
		return Experience.levelForXp(xp);
	}

	public BankRecord bank(String key, int x, int y)
	{
		BankRecord record = banks.get(key);
		if (record == null)
		{
			record = new BankRecord();
			record.x = x;
			record.y = y;
			banks.put(key, record);
		}
		return record;
	}

	/**
	 * Moves records saved under a map region onto a bank name that now covers them, so
	 * adding a name later keeps the time already spent there.
	 */
	public void renameBanks()
	{
		Map<String, BankRecord> renamed = new LinkedHashMap<>();
		for (Map.Entry<String, BankRecord> entry : banks.entrySet())
		{
			BankRecord record = entry.getValue();
			String key = entry.getKey().startsWith("region-") ? BankNames.keyFor(record.x, record.y) : entry.getKey();
			BankRecord existing = renamed.get(key);
			if (existing == null)
			{
				renamed.put(key, record);
			}
			else
			{
				existing.ticks += record.ticks;
				existing.xp += record.xp;
			}
		}
		banks = renamed;
	}

	/**
	 * The save to keep when there's more than one: the most XP, then the most rolls. Null if
	 * there are none.
	 */
	public static Progress best(List<Progress> saves)
	{
		Progress best = null;
		for (Progress save : saves)
		{
			if (best == null || save.xp > best.xp || save.xp == best.xp && save.rolls > best.rolls)
			{
				best = save;
			}
		}
		return best;
	}

	/**
	 * One save made from several copies of the same account's progress: the one with the most
	 * XP (see {@link #best}), plus anything found in the others that it's missing. Counts that
	 * the copies share, like XP and time at each bank, aren't added together, since both
	 * copies usually include the same history; the bigger one is kept. Null if there are none.
	 */
	public static Progress merge(List<Progress> saves)
	{
		Progress merged = best(saves);
		for (Progress other : saves)
		{
			if (other != merged)
			{
				merged.add(other);
			}
		}
		return merged;
	}

	private void add(Progress other)
	{
		for (Map.Entry<String, BankRecord> entry : other.banks.entrySet())
		{
			BankRecord theirs = entry.getValue();
			BankRecord ours = banks.get(entry.getKey());
			if (ours == null)
			{
				banks.put(entry.getKey(), theirs);
				continue;
			}
			ours.ticks = Math.max(ours.ticks, theirs.ticks);
			ours.xp = Math.max(ours.xp, theirs.xp);
			ours.visits = Math.max(ours.visits, theirs.visits);
			ours.firstAt = earliest(ours.firstAt, theirs.firstAt);
		}

		for (Map.Entry<String, CollectionLog.Obtained> entry : other.collection.entrySet())
		{
			CollectionLog.Obtained theirs = entry.getValue();
			CollectionLog.Obtained ours = collection.get(entry.getKey());
			if (ours == null)
			{
				collection.put(entry.getKey(), theirs);
				continue;
			}
			if (theirs.firstAt != 0 && (ours.firstAt == 0 || theirs.firstAt < ours.firstAt))
			{
				ours.firstAt = theirs.firstAt;
				ours.firstRolls = theirs.firstRolls;
			}
			ours.count = Math.max(ours.count, theirs.count);
		}

		for (Map.Entry<String, Long> entry : other.moments.entrySet())
		{
			moments.merge(entry.getKey(), entry.getValue(), Math::min);
		}
		// The latest payout of each phrase, so its hour's wait still holds.
		for (Map.Entry<String, Long> entry : other.keywordPaid.entrySet())
		{
			keywordPaid.merge(entry.getKey(), entry.getValue(), Math::max);
		}
		if (other.keywordDate != null && other.keywordDate.equals(keywordDate))
		{
			keywordCount = Math.max(keywordCount, other.keywordCount);
		}
		for (Map.Entry<String, Keywords.Noted> entry : other.phraseNotes.entrySet())
		{
			phraseNotes.putIfAbsent(entry.getKey(), entry.getValue());
		}
		journalRead.addAll(other.journalRead);
		journalSince = earliest(journalSince, other.journalSince);
		daysStood.addAll(other.daysStood);
		if (other.notesDate != null && other.notesDate.equals(notesDate))
		{
			notes.addAll(other.notes);
		}
		lastSitRoll = Math.max(lastSitRoll, other.lastSitRoll);
		lastFireRoll = Math.max(lastFireRoll, other.lastFireRoll);
	}

	/** The earlier of two times, where 0 means unknown. */
	private static long earliest(long a, long b)
	{
		return a == 0 ? b : b == 0 ? a : Math.min(a, b);
	}
}
