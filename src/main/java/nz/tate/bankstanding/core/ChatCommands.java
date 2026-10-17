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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The answers to {@code !bs} (or {@code !bankstanding}) in chat. The message goes to the
 * game exactly as typed; only your own copy of it is redrawn with the answer, so other
 * players see the command. Nothing here ever names a secret: secrets are only counted.
 */
public final class ChatCommands
{
	public static final String SHORT = "!bs";
	public static final String LONG = "!bankstanding";
	private static final int TOP = 3;

	/** Everything the answers are made from, gathered by the plugin on the client thread. */
	public static final class Facts
	{
		public Progress progress;
		public Session session;
		/** The bank you're at, or null when you aren't at one. */
		public String hereKey;
		public LocalDate today;
		/** The collection log's {found, total}. */
		public int[] log;
		/** How many secrets you've found, of every kind. */
		public int secrets;
	}

	private ChatCommands()
	{
	}

	/** The answer to this message, or null to leave it as typed. */
	public static String answer(String message, Facts f)
	{
		String text = message.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
		String rest;
		if (text.equals(LONG) || text.startsWith(LONG + " "))
		{
			rest = text.substring(LONG.length()).trim();
		}
		else if (text.equals(SHORT) || text.startsWith(SHORT + " "))
		{
			rest = text.substring(SHORT.length()).trim();
		}
		else
		{
			return null;
		}
		Progress p = f.progress;
		switch (rest)
		{
			case "":
				return "Bank Standing level " + p.level() + " (" + StatsView.number(p.xp) + " xp), "
					+ StatsView.duration(p.records.totalTicks) + " stood";
			case "here":
				return f.hereKey == null ? null : bank(p, f.hereKey);
			case "fav":
				List<String> top = top(p, 1);
				return top.isEmpty() ? null : "My home bank: " + name(p, top.get(0)) + ", "
					+ StatsView.duration(p.banks.get(top.get(0)).ticks);
			case "regulars":
				return regulars(p);
			case "banks":
				return "Banks visited: " + CollectionLog.banksVisited(p) + "/" + BankRegions.total();
			case "log":
				return "Bank Standing log: " + f.log[0] + "/" + f.log[1];
			case "pet":
				return pets(p);
			case "secrets":
				return "Secrets found: " + f.secrets;
			case "session":
				return "This session: " + StatsView.duration(f.session.ticks) + ", "
					+ StatsView.number(f.session.xp) + " xp";
			case "streak":
				int days = DailyStreak.current(p, f.today);
				return "Daily streak: " + days + (days == 1 ? " day" : " days");
			case "title":
				return Unlocks.title(p.level()) + " (level " + p.level() + ")";
			case "help":
				return "!bs, then: here, a bank's name, fav, regulars, banks, log, pet, secrets, session, streak, title";
			default:
				String key = findBank(p, rest);
				return key == null ? null : bank(p, key);
		}
	}

	/** One bank's record: time, visits and since when, as far as they're known. */
	private static String bank(Progress p, String key)
	{
		BankRecord record = p.banks.get(key);
		if (record == null || record.ticks == 0)
		{
			return name(p, key) + ": not stood here yet";
		}
		StringBuilder line = new StringBuilder(name(p, key)).append(": ")
			.append(StatsView.duration(record.ticks)).append(" stood");
		if (record.visits > 0)
		{
			line.append(", ").append(StatsView.number(record.visits)).append(record.visits == 1 ? " visit" : " visits");
		}
		if (record.firstAt > 0)
		{
			line.append(", regular since ").append(StatsView.date(record.firstAt));
		}
		return line.toString();
	}

	private static String regulars(Progress p)
	{
		List<String> top = top(p, TOP);
		if (top.isEmpty())
		{
			return null;
		}
		StringBuilder line = new StringBuilder("Top banks: ");
		for (int i = 0; i < top.size(); i++)
		{
			if (i > 0)
			{
				line.append(", ");
			}
			line.append(i + 1).append(". ").append(name(p, top.get(i))).append(' ')
				.append(StatsView.duration(p.banks.get(top.get(i)).ticks));
		}
		return line.toString();
	}

	/** The pets you have and how long each took; a secret one isn't mentioned until it drops. */
	private static String pets(Progress p)
	{
		List<String> lines = new ArrayList<>();
		boolean missing = false;
		for (CollectionLog.Drop drop : CollectionLog.DROPS)
		{
			if (!drop.pet)
			{
				continue;
			}
			CollectionLog.Obtained got = p.collection.get(drop.key);
			if (got == null)
			{
				missing = true;
				continue;
			}
			lines.add(drop.name + " after " + StatsView.duration(got.firstRolls * XpRoller.ROLL_INTERVAL_TICKS));
		}
		if (lines.isEmpty())
		{
			return "No pets yet";
		}
		return "Pets: " + String.join(", ", lines) + (missing ? ", still hunting" : "");
	}

	/** The banks you've stood at longest, longest first. */
	private static List<String> top(Progress p, int count)
	{
		List<Map.Entry<String, BankRecord>> banks = new ArrayList<>(p.banks.entrySet());
		banks.removeIf(e -> e.getValue().ticks == 0);
		banks.sort((a, b) -> Long.compare(b.getValue().ticks, a.getValue().ticks));
		List<String> keys = new ArrayList<>();
		for (int i = 0; i < Math.min(count, banks.size()); i++)
		{
			keys.add(banks.get(i).getKey());
		}
		return keys;
	}

	/** The bank this name means: an exact name first, then one that starts with it, then any containing it. */
	static String findBank(Progress p, String asked)
	{
		String wanted = squash(asked);
		if (wanted.isEmpty())
		{
			return null;
		}
		String starts = null;
		String contains = null;
		for (BankArea area : BankNames.all())
		{
			// Only banks the log lists, or ones you've stood at: a secret one stays secret.
			if (BankRegions.regionOf(area.getKey()) == null && !p.banks.containsKey(area.getKey()))
			{
				continue;
			}
			String name = squash(area.getName());
			String key = squash(area.getKey());
			if (name.equals(wanted) || key.equals(wanted))
			{
				return area.getKey();
			}
			if (starts == null && (name.startsWith(wanted) || key.startsWith(wanted)))
			{
				starts = area.getKey();
			}
			if (contains == null && (name.contains(wanted) || key.contains(wanted)))
			{
				contains = area.getKey();
			}
		}
		return starts != null ? starts : contains;
	}

	private static String squash(String text)
	{
		return text.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]", "");
	}

	private static String name(Progress p, String key)
	{
		return BankNames.displayName(key, p.banks.get(key));
	}
}
