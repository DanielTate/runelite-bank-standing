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

/** Holiday windows, by UTC date. */
public enum Holiday
{
	/** 20 to 31 December. */
	CHRISTMAS,
	/** 31 December to 2 January. */
	NEW_YEAR,
	/** 25 October to 1 November. */
	HALLOWEEN,
	/** Easter Sunday, three days either side. */
	EASTER,
	/** RuneScape's birthday week, 20 to 24 February. */
	BIRTHDAY;

	public boolean isOn(LocalDate d)
	{
		int year = d.getYear();
		switch (this)
		{
			case CHRISTMAS:
				return within(d, LocalDate.of(year, 12, 20), LocalDate.of(year, 12, 31));
			case NEW_YEAR:
				return d.equals(LocalDate.of(year, 12, 31)) || within(d, LocalDate.of(year, 1, 1), LocalDate.of(year, 1, 2));
			case HALLOWEEN:
				return within(d, LocalDate.of(year, 10, 25), LocalDate.of(year, 11, 1));
			case EASTER:
				LocalDate easter = easterSunday(year);
				return within(d, easter.minusDays(3), easter.plusDays(3));
			case BIRTHDAY:
				return within(d, LocalDate.of(year, 2, 20), LocalDate.of(year, 2, 24));
			default:
				return false;
		}
	}

	private static boolean within(LocalDate d, LocalDate from, LocalDate to)
	{
		return !d.isBefore(from) && !d.isAfter(to);
	}

	/** Western Easter Sunday (anonymous Gregorian algorithm). */
	static LocalDate easterSunday(int year)
	{
		int a = year % 19;
		int b = year / 100;
		int c = year % 100;
		int d = b / 4;
		int e = b % 4;
		int f = (b + 8) / 25;
		int g = (b - f + 1) / 3;
		int h = (19 * a + b - d - g + 15) % 30;
		int i = c / 4;
		int k = c % 4;
		int l = (32 + 2 * e + 2 * i - h - k) % 7;
		int m = (a + 11 * h + 22 * l) / 451;
		int month = (h + l - 7 * m + 114) / 31;
		int day = ((h + l - 7 * m + 114) % 31) + 1;
		return LocalDate.of(year, month, day);
	}
}
