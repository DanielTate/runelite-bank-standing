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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Uses made-up phrases, never the real ones. */
public class KeywordsTest
{
	private static final LocalDate DAY = LocalDate.of(2026, 10, 14);
	private static final long HOUR = Keywords.COOLDOWN_MILLIS;

	private final Keywords keywords = new Keywords(Arrays.asList(
		Keywords.phrase(Keywords.hash("test phrase"), null),
		Keywords.phrase(Keywords.hash("other phrase"), null),
		Keywords.phrase(Keywords.hash("spooky phrase"), Holiday.HALLOWEEN),
		Keywords.phrase(Keywords.hash("local phrase"), null, "ge", "edgeville")));

	private boolean claim(Progress p, String message, long now, LocalDate day)
	{
		return keywords.claim(p, message, now, day, "varrock-west");
	}

	@Test
	public void wholeMessageOnlyIgnoringCaseAndSpacing()
	{
		Progress p = new Progress();
		assertTrue(claim(p, "  Test   PHRASE ", 0, DAY));
		assertFalse(claim(p, "lol test phrase", 5 * HOUR, DAY));
		assertFalse(claim(p, "hello", 5 * HOUR, DAY));
	}

	@Test
	public void eachPhrasePaysOncePerHour()
	{
		Progress p = new Progress();
		assertTrue(claim(p, "test phrase", 0, DAY));
		assertFalse(claim(p, "test phrase", HOUR - 1, DAY));
		assertTrue(claim(p, "other phrase", HOUR - 1, DAY));
		assertTrue(claim(p, "test phrase", HOUR, DAY));
	}

	@Test
	public void dailyCapAcrossAllPhrasesResetsAtMidnightUtc()
	{
		Progress p = new Progress();
		for (int i = 0; i < Keywords.DAILY_CAP; i++)
		{
			assertTrue(claim(p, "test phrase", i * HOUR, DAY));
		}
		assertFalse(claim(p, "test phrase", 20 * HOUR, DAY));
		assertTrue(claim(p, "test phrase", 21 * HOUR, DAY.plusDays(1)));
	}

	@Test
	public void holidayPhrasesOnlyWorkOnTheHoliday()
	{
		assertFalse(claim(new Progress(), "spooky phrase", 0, DAY));
		assertTrue(claim(new Progress(), "spooky phrase", 0, LocalDate.of(2026, 10, 31)));
	}

	@Test
	public void bankPhrasesOnlyWorkAtTheirBanks()
	{
		assertFalse(claim(new Progress(), "local phrase", 0, DAY));
		assertTrue(keywords.claim(new Progress(), "local phrase", 0, DAY, "edgeville"));
	}

	@Test
	public void shippedPhrasesAreUnique()
	{
		Set<String> hashes = new HashSet<>();
		for (Keywords.Phrase phrase : Keywords.PHRASES)
		{
			assertTrue(hashes.add(phrase.hash));
		}
		assertEquals(31, hashes.size());
	}
}
