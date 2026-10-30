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
 * The answers to {@code !bs} (or {@code !bankstanding}) in chat. On its way out the message
 * is swapped for {@link #SHARED} plus a short answer, so other players see it too: as plain
 * text without the plugin, and with the command hidden with it. Nothing here ever names a
 * secret: secrets are only counted.
 */
public final class ChatCommands
{
	public static final String SHORT = "!bs";
	public static final String LONG = "!bankstanding";
	/** Starts an answer sent to chat: "!bs - Bank Standing level 87 (...)". */
	public static final String SHARED = SHORT + " - ";
	/** The game's limit on a chat message. */
	static final int MAX_MESSAGE = 80;
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

	/** The answer to this message, in full, or null to leave it as typed. */
	public static String answer(String message, Facts f)
	{
		String rest = command(message);
		List<String> answers = rest == null ? null : answers(rest, f);
		return answers == null || answers.isEmpty() ? null : answers.get(0);
	}

	/**
	 * What to send instead of this message, so other players see the answer: {@link #SHARED}
	 * and the longest answer that fits in a chat message. Null to send it as typed, as for
	 * {@code help}, which only lists the commands.
	 */
	public static String shared(String message, Facts f)
	{
		String rest = command(message);
		if (rest == null || rest.equals("help") || rest.startsWith("-"))
		{
			return null;
		}
		for (String answer : answers(rest, f))
		{
			if (SHARED.length() + answer.length() <= MAX_MESSAGE)
			{
				return SHARED + answer;
			}
		}
		return null;
	}

	/** The answer inside a message someone sent with {@link #shared}, or null if it isn't one. */
	public static String sharedAnswer(String message)
	{
		String text = message.trim();
		if (text.length() <= SHARED.length() || !text.substring(0, SHARED.length()).equalsIgnoreCase(SHARED))
		{
			return null;
		}
		String answer = text.substring(SHARED.length()).trim();
		return answer.isEmpty() ? null : answer;
	}

	/** What follows {@code !bs} in the message, lower case; null if it isn't the command. */
	private static String command(String message)
	{
		String text = message.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
		if (text.equals(LONG) || text.startsWith(LONG + " "))
		{
			return text.substring(LONG.length()).trim();
		}
		if (text.equals(SHORT) || text.startsWith(SHORT + " "))
		{
			return text.substring(SHORT.length()).trim();
		}
		return null;
	}

	/** The answer, longest first, then shorter ways to say it for a chat message. Empty if there's none. */
	private static List<String> answers(String rest, Facts f)
	{
		Progress p = f.progress;
		switch (rest)
		{
			case "":
				return one("Bank Standing level " + p.level() + " (" + StatsView.number(p.xp) + " xp), "
					+ StatsView.duration(p.records.totalTicks) + " stood");
			case "here":
				return f.hereKey == null ? new ArrayList<>() : bank(p, f.hereKey);
			case "fav":
				List<String> top = top(p, 1);
				return one(top.isEmpty() ? null : "My home bank: " + name(p, top.get(0)) + ", "
					+ StatsView.duration(p.banks.get(top.get(0)).ticks));
			case "regulars":
				return regulars(p);
			case "banks":
				return one("Banks visited: " + CollectionLog.banksVisited(p) + "/" + BankRegions.total());
			case "log":
				return one("Bank Standing log: " + f.log[0] + "/" + f.log[1]);
			case "pet":
				return pets(p);
			case "secrets":
				return one("Secrets found: " + f.secrets);
			case "session":
				return one("This session: " + StatsView.duration(f.session.ticks) + ", "
					+ StatsView.number(f.session.xp) + " xp");
			case "streak":
				int days = DailyStreak.current(p, f.today);
				return one("Daily streak: " + days + (days == 1 ? " day" : " days"));
			case "title":
				return one(Unlocks.title(p.level()) + " (level " + p.level() + ")");
			case "help":
				return one("!bs, then: here, a bank's name, fav, regulars, banks, log, pet, secrets, session, streak, title");
			default:
				String key = findBank(p, rest);
				return key == null ? new ArrayList<>() : bank(p, key);
		}
	}

	private static List<String> one(String answer)
	{
		List<String> answers = new ArrayList<>();
		if (answer != null)
		{
			answers.add(answer);
		}
		return answers;
	}

	/** One bank's record: time, visits and since when, as far as they're known; then without since when, then visits. */
	private static List<String> bank(Progress p, String key)
	{
		BankRecord record = p.banks.get(key);
		if (record == null || record.ticks == 0)
		{
			return one(name(p, key) + ": not stood here yet");
		}
		List<String> answers = new ArrayList<>();
		String line = name(p, key) + ": " + StatsView.duration(record.ticks) + " stood";
		String visits = record.visits == 0 ? line : line + ", " + StatsView.number(record.visits)
			+ (record.visits == 1 ? " visit" : " visits");
		if (record.firstAt > 0)
		{
			answers.add(visits + ", regular since " + StatsView.date(record.firstAt));
		}
		answers.add(visits);
		if (!visits.equals(line))
		{
			answers.add(line);
		}
		return answers;
	}

	/** The top banks, then fewer of them. */
	private static List<String> regulars(Progress p)
	{
		List<String> top = top(p, TOP);
		List<String> answers = new ArrayList<>();
		for (int count = top.size(); count >= 1; count--)
		{
			answers.add(regulars(p, top.subList(0, count)));
		}
		return answers;
	}

	private static String regulars(Progress p, List<String> top)
	{
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

	/**
	 * The pets you have and how long each took, then just their names, then how many; a
	 * secret one isn't mentioned until it drops.
	 */
	private static List<String> pets(Progress p)
	{
		List<String> lines = new ArrayList<>();
		List<String> names = new ArrayList<>();
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
			names.add(drop.name);
		}
		if (lines.isEmpty())
		{
			return one("No pets yet");
		}
		String hunting = missing ? ", still hunting" : "";
		List<String> answers = new ArrayList<>();
		answers.add("Pets: " + String.join(", ", lines) + hunting);
		answers.add("Pets: " + String.join(", ", names) + hunting);
		answers.add("Pets: " + names.size() + hunting);
		return answers;
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
