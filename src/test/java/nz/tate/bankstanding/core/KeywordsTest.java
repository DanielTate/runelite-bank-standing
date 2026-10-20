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
import java.util.Collections;
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
		Keywords.phrase(Keywords.hash("local phrase"), null, "ge", "edgeville"),
		Keywords.phrase(Keywords.hash("dressed phrase"), null).wearing(1234),
		Keywords.phrase(Keywords.hash("party phrase"), Holiday.HALLOWEEN, "canifis")));

	private boolean claim(Progress p, String message, long now, LocalDate day)
	{
		return keywords.claim(p, message, now, day, "varrock-west", Collections.emptySet()) != null;
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
	public void trailingPunctuationStillCounts()
	{
		assertTrue(claim(new Progress(), "Test phrase?", 0, DAY));
		assertTrue(claim(new Progress(), "test phrase!!", 0, DAY));
		assertTrue(claim(new Progress(), "test phrase...", 0, DAY));
		assertFalse(claim(new Progress(), "test, phrase", 0, DAY));
		Progress p = new Progress();
		assertTrue(claim(p, "test phrase?", 0, DAY));
		assertEquals("test phrase", keywords.found(p, Keywords.Group.ANYWHERE).get(0).said);
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
	public void foundCountsEachPhraseOnce()
	{
		Progress p = new Progress();
		assertEquals(0, keywords.found(p));
		claim(p, "test phrase", 0, DAY);
		claim(p, "test phrase", 2 * HOUR, DAY);
		claim(p, "other phrase", 0, DAY);
		claim(p, "not a phrase", 0, DAY);
		assertEquals(2, keywords.found(p));
		assertEquals(6, keywords.count());
	}

	@Test
	public void groupsCountEveryPhraseOnce()
	{
		Progress p = new Progress();
		claim(p, "test phrase", 0, DAY);
		java.util.Map<Keywords.Group, int[]> groups = keywords.foundByGroup(p);
		assertEquals(1, groups.get(Keywords.Group.ANYWHERE)[0]);
		assertEquals(2, groups.get(Keywords.Group.ANYWHERE)[1]);
		assertEquals(1, groups.get(Keywords.Group.BANK)[1]);
		assertEquals(2, groups.get(Keywords.Group.HOLIDAY)[1]);
		assertEquals(1, groups.get(Keywords.Group.WORN)[1]);

		int total = 0;
		for (int[] count : new Keywords(Keywords.PHRASES).foundByGroup(p).values())
		{
			total += count[1];
		}
		assertEquals(Keywords.PHRASES.size(), total);
	}

	@Test
	public void clanPhraseEndsWithYourClanAtTheClanHall()
	{
		Keywords clan = new Keywords(Arrays.asList(Keywords.phrase(Keywords.hash("hello"), null, "clan-hall").beforeClanName()));
		assertEquals(Keywords.Kind.LOCATION, clan.claim(new Progress(), "Hello Iron\u00A0Squad", 0, DAY, "clan-hall",
			Collections.emptySet(), "Iron Squad"));
		assertEquals(null, clan.claim(new Progress(), "hello other clan", 0, DAY, "clan-hall",
			Collections.emptySet(), "Iron Squad"));
		assertEquals(null, clan.claim(new Progress(), "hello iron squad", 0, DAY, "ge",
			Collections.emptySet(), "Iron Squad"));
		assertEquals(null, clan.claim(new Progress(), "hello", 0, DAY, "clan-hall", Collections.emptySet(), null));

		Progress p = new Progress();
		clan.claim(p, "hello iron squad", 0, DAY, "clan-hall", Collections.emptySet(), "Iron Squad");
		assertEquals(1, clan.found(p));
	}

	@Test
	public void holidayPhrasesOnlyWorkOnTheHoliday()
	{
		assertFalse(claim(new Progress(), "spooky phrase", 0, DAY));
		assertEquals(Keywords.Kind.DATE, keywords.claim(new Progress(), "spooky phrase", 0, LocalDate.of(2026, 10, 31),
			"varrock-west", Collections.emptySet()));
	}

	@Test
	public void bankPhrasesOnlyWorkAtTheirBanks()
	{
		assertFalse(claim(new Progress(), "local phrase", 0, DAY));
		assertEquals(Keywords.Kind.LOCATION,
			keywords.claim(new Progress(), "local phrase", 0, DAY, "edgeville", Collections.emptySet()));
	}

	@Test
	public void shippedPhrasesAreUnique()
	{
		Set<String> hashes = new HashSet<>();
		for (Keywords.Phrase phrase : Keywords.PHRASES)
		{
			assertTrue(hashes.add(phrase.hash));
		}
		assertEquals(407, hashes.size());
	}

	@Test
	public void dateAndPlacePhrasesAreTheirOwnKind()
	{
		assertEquals(Keywords.Kind.DATE_AND_LOCATION, keywords.claim(new Progress(), "party phrase", 0,
			LocalDate.of(2026, 10, 31), "canifis", Collections.emptySet()));
	}

	@Test
	public void itemPhrasesNeedTheItemWorn()
	{
		assertFalse(claim(new Progress(), "dressed phrase", 0, DAY));
		assertEquals(Keywords.Kind.ANYWHERE,
			keywords.claim(new Progress(), "dressed phrase", 0, DAY, "ge", Collections.singleton(1234)));
	}

	@Test
	public void shippedPhrasesOnlyNameRealBanks()
	{
		Set<String> named = new HashSet<>();
		for (BankArea area : BankNames.all())
		{
			named.add(area.getKey());
		}
		for (Keywords.Phrase phrase : Keywords.PHRASES)
		{
			for (String bank : phrase.banks)
			{
				assertTrue(bank, named.contains(bank));
			}
		}
	}

	@Test
	public void spellbookPhrasesNeedThatSpellbook()
	{
		Keywords book = new Keywords(Arrays.asList(
			Keywords.phrase(Keywords.hash("moon phrase"), null).onSpellbook(Spellbook.LUNAR)));
		assertEquals(null, book.claim(new Progress(), "moon phrase", 0, DAY, "ge", Collections.emptySet(), null,
			Spellbook.STANDARD));
		assertEquals(null, book.claim(new Progress(), "moon phrase", 0, DAY, "ge", Collections.emptySet()));
		Progress p = new Progress();
		assertEquals(Keywords.Kind.ANYWHERE, book.claim(p, "moon phrase", 0, DAY, "ge", Collections.emptySet(), null,
			Spellbook.LUNAR));
		assertEquals(1, book.foundByGroup(p).get(Keywords.Group.SPELLBOOK)[0]);
	}

	@Test
	public void theJournalNotesWhatWasSaidWhereAndWhenItWasFirstFound()
	{
		Progress p = new Progress();
		keywords.claim(p, "  Local   PHRASE ", 5, DAY, "ge", Collections.emptySet());
		keywords.claim(p, "local phrase", 5 + 2 * HOUR, DAY, "edgeville", Collections.emptySet());
		java.util.List<Keywords.Noted> found = keywords.found(p, Keywords.Group.BANK);
		assertEquals(1, found.size());
		assertEquals("local phrase", found.get(0).said);
		assertEquals("ge", found.get(0).bank);
		assertEquals(5, found.get(0).at);
		assertTrue(keywords.unread(p, Keywords.Group.BANK));
		assertFalse(keywords.unread(p, Keywords.Group.ANYWHERE));

		found.get(0).read = true;
		assertFalse(keywords.unread(p, Keywords.Group.BANK));
	}

	@Test
	public void phrasesFoundBeforeTheJournalAreListedWithoutTheirWords()
	{
		Progress p = new Progress();
		p.keywordPaid.put(Keywords.hash("test phrase"), 7L);
		java.util.List<Keywords.Noted> found = keywords.found(p, Keywords.Group.ANYWHERE);
		assertEquals(1, found.size());
		assertEquals(null, found.get(0).said);
		assertFalse(keywords.unread(p, Keywords.Group.ANYWHERE));

		keywords.claim(p, "test phrase", 2 * HOUR, DAY, "ge", Collections.emptySet());
		assertEquals("test phrase", keywords.found(p, Keywords.Group.ANYWHERE).get(0).said);
	}
}
