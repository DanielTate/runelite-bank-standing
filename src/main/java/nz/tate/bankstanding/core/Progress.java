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
import java.util.Map;

/** Everything saved for one account. Field names are permanent: they are the save format. */
public final class Progress
{
	public int xp;
	public Map<String, BankRecord> banks = new LinkedHashMap<>();

	/** The last UTC day (ISO date) that counted toward the streak, and how long the streak is. */
	public String streakDate;
	public int streakDays;
	/** Standing ticks so far on {@link #todayDate}. */
	public String todayDate;
	public long todayTicks;

	/** When each keyword last paid out, and how many paid out on {@link #keywordDate}. */
	public Map<String, Long> keywordPaid = new LinkedHashMap<>();
	public String keywordDate;
	public int keywordCount;

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
}
