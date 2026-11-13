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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * One bank each day where XP comes faster. Picked from the UTC date, so every player has
 * the same bank at the same moment and it changes at UTC midnight. The banks come round in
 * a shuffled order, each once before any repeats.
 */
public final class BankOfTheDay
{
	/** XP at the bank of the day, on top of every other bonus and their cap. */
	public static final double XP_MULTIPLIER = 1.25;
	/** How the bonus is described to players. */
	public static final String BONUS = "+25% XP";
	/** Changing this reshuffles every day's bank, so leave it be. */
	private static final long SEED = 0x6261_6e6b_6461_79L;

	private BankOfTheDay()
	{
	}

	/** Today's date in UTC. */
	public static LocalDate today(long nowMillis)
	{
		return Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC).toLocalDate();
	}

	/** The last day worked out, as {epoch day, key}, since it's asked for every tick. */
	private static volatile Object[] cached = {Long.MIN_VALUE, null};

	/** The bank of the day on this UTC date. */
	public static String key(LocalDate utcDate)
	{
		Object[] last = cached;
		if ((long) last[0] == utcDate.toEpochDay())
		{
			return (String) last[1];
		}
		String key = pick(utcDate);
		cached = new Object[]{utcDate.toEpochDay(), key};
		return key;
	}

	private static String pick(LocalDate utcDate)
	{
		List<String> banks = new ArrayList<>(BankRegions.allKeys());
		long day = utcDate.toEpochDay();
		long round = Math.floorDiv(day, banks.size());
		Collections.shuffle(banks, new Random(SEED ^ round * 0x9e37_79b9_7f4a_7c15L));
		return banks.get((int) Math.floorMod(day, banks.size()));
	}

	/** Dev only: a bank to use as the bank of the day instead, or null for the real one. */
	private static volatile String devOverride;

	public static void devOverride(String key)
	{
		devOverride = key;
	}

	public static String keyAt(long nowMillis)
	{
		String override = devOverride;
		return override != null ? override : key(today(nowMillis));
	}

	/** Milliseconds until the next bank, at UTC midnight. */
	public static long millisUntilChange(long nowMillis)
	{
		long day = 86_400_000L;
		return day - Math.floorMod(nowMillis, day);
	}

	/** "5h 12m" or "12m" until the next bank. */
	public static String timeLeft(long nowMillis)
	{
		long minutes = (millisUntilChange(nowMillis) + 59_999) / 60_000;
		return minutes < 60 ? minutes + "m" : String.format(Locale.ENGLISH, "%dh %02dm", minutes / 60, minutes % 60);
	}
}
