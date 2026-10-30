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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ChatCommandsTest
{
	private static final long HOUR_TICKS = 6000;

	private ChatCommands.Facts facts()
	{
		ChatCommands.Facts f = new ChatCommands.Facts();
		f.progress = new Progress();
		f.session = new Session();
		f.today = LocalDate.of(2026, 10, 18);
		f.log = new int[]{212, 480};
		f.secrets = 41;
		return f;
	}

	private static BankRecord stood(Progress p, String key, long ticks)
	{
		BankRecord record = new BankRecord();
		record.ticks = ticks;
		p.banks.put(key, record);
		return record;
	}

	@Test
	public void onlyItsOwnCommands()
	{
		ChatCommands.Facts f = facts();
		assertNull(ChatCommands.answer("hello", f));
		assertNull(ChatCommands.answer("!bsx", f));
		assertNull(ChatCommands.answer("!kc zulrah", f));
		assertTrue(ChatCommands.answer("!bs", f).startsWith("Bank Standing level 1"));
		assertTrue(ChatCommands.answer("!BankStanding", f).startsWith("Bank Standing level 1"));
	}

	@Test
	public void counts()
	{
		ChatCommands.Facts f = facts();
		assertEquals("Bank Standing log: 212/480", ChatCommands.answer("!bs log", f));
		assertEquals("Secrets found: 41", ChatCommands.answer("!bs  SECRETS ", f));
		assertEquals("Banks visited: 0/" + BankRegions.total(), ChatCommands.answer("!bs banks", f));
		assertEquals("Loiterer (level 1)", ChatCommands.answer("!bs title", f));
		assertEquals("No pets yet", ChatCommands.answer("!bs pet", f));
	}

	@Test
	public void banksByTimeAndByName()
	{
		ChatCommands.Facts f = facts();
		assertNull(ChatCommands.answer("!bs fav", f));
		stood(f.progress, "edgeville", 41 * HOUR_TICKS);
		BankRecord ge = stood(f.progress, "ge", 2 * HOUR_TICKS);
		ge.visits = 37;
		stood(f.progress, "varrock-west", HOUR_TICKS);
		assertEquals("My home bank: Edgeville, 41h 00m", ChatCommands.answer("!bs fav", f));
		assertEquals("Top banks: 1. Edgeville 41h 00m, 2. Grand Exchange 2h 00m, 3. Varrock West 1h 00m",
			ChatCommands.answer("!bs regulars", f));
		assertEquals("Grand Exchange: 2h 00m stood, 37 visits", ChatCommands.answer("!bs grand exchange", f));
		assertEquals("Varrock West: 1h 00m stood", ChatCommands.answer("!bs varrock west", f));
		assertEquals("Varrock West: 1h 00m stood", ChatCommands.answer("!bs varrock", f));
		assertNull(ChatCommands.answer("!bs nowhere at all", f));
	}

	@Test
	public void hereNeedsABank()
	{
		ChatCommands.Facts f = facts();
		assertNull(ChatCommands.answer("!bs here", f));
		f.hereKey = "ge";
		assertEquals("Grand Exchange: not stood here yet", ChatCommands.answer("!bs here", f));
	}

	@Test
	public void onlyListedOrVisitedBanksByName()
	{
		ChatCommands.Facts f = facts();
		assertNull(ChatCommands.answer("!bs tutorial island", f));
		stood(f.progress, "tutorial-island", 100);
		assertEquals("Tutorial Island: 1m stood", ChatCommands.answer("!bs tutorial island", f));
	}

	@Test
	public void sharedAnswersCarryTheAnswer()
	{
		ChatCommands.Facts f = facts();
		assertEquals("!bs - Bank Standing log: 212/480", ChatCommands.shared("!bs log", f));
		assertEquals("!bs - Secrets found: 41", ChatCommands.shared("!BankStanding secrets", f));
		assertTrue(ChatCommands.shared("!bs", f).startsWith("!bs - Bank Standing level 1 ("));
		// Not commands, no answer, and help: sent as typed.
		assertNull(ChatCommands.shared("hello", f));
		assertNull(ChatCommands.shared("!bs fav", f));
		assertNull(ChatCommands.shared("!bs help", f));
		// Already an answer: left alone.
		assertNull(ChatCommands.shared("!bs - Secrets found: 41", f));
	}

	@Test
	public void sharedAnswersFitInAChatMessage()
	{
		ChatCommands.Facts f = facts();
		BankRecord ge = stood(f.progress, "ge", 123456 * HOUR_TICKS);
		ge.visits = 1234567;
		ge.firstAt = 1_760_000_000_000L;
		stood(f.progress, "varrock-west", 900 * HOUR_TICKS);
		stood(f.progress, "edgeville", 800 * HOUR_TICKS);
		for (String command : new String[]{"", " here", " grand exchange", " fav", " regulars", " banks", " log",
			" pet", " secrets", " session", " streak", " title"})
		{
			f.hereKey = "ge";
			String shared = ChatCommands.shared("!bs" + command, f);
			if (shared != null)
			{
				assertTrue(shared, shared.length() <= ChatCommands.MAX_MESSAGE);
			}
		}
		// The bank's full answer is too long, so "regular since" goes first.
		assertEquals("!bs - Grand Exchange: 123456h 00m stood, 1,234,567 visits", ChatCommands.shared("!bs ge", f));
		assertTrue(ChatCommands.answer("!bs ge", f).contains("regular since"));
		// Three top banks don't fit, two do.
		assertEquals("!bs - Top banks: 1. Grand Exchange 123456h 00m, 2. Varrock West 900h 00m",
			ChatCommands.shared("!bs regulars", f));
	}

	@Test
	public void readsSharedAnswers()
	{
		assertEquals("Secrets found: 41", ChatCommands.sharedAnswer("!bs - Secrets found: 41"));
		assertEquals("Secrets found: 41", ChatCommands.sharedAnswer("!BS - Secrets found: 41 "));
		assertNull(ChatCommands.sharedAnswer("!bs"));
		assertNull(ChatCommands.sharedAnswer("!bs - "));
		assertNull(ChatCommands.sharedAnswer("!bs log"));
		assertNull(ChatCommands.sharedAnswer("hello !bs - hi"));
		assertFalse(ChatCommands.SHARED.isEmpty());
	}
}
