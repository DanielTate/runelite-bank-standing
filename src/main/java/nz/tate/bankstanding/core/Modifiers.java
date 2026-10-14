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
import java.time.Month;
import java.util.Set;

/**
 * Works out the hidden bonuses for one roll. Nothing here is ever shown to the player.
 *
 * <p>Every modifier is small. "Gear" ones add +1 to the max hit and lift the average a
 * little; the rest only lift the average. All of it together is capped at
 * {@link XpRoller#MAX_BOOST}, so a fully kitted player is only about 10% faster.
 */
public final class Modifiers
{
	static final double TINY = 0.005;
	static final double SMALL = 0.01;
	static final double MEDIUM = 0.02;
	static final double GEAR = 0.015;

	static final double NEW_SPOT_HOURS = 10;
	static final int STREAK_CAP_DAYS = 5;
	static final long WEALTH_THRESHOLD = 100_000_000L;

	/** The result: how much to lift the average roll, and how much to add to the max hit. */
	public static final class Effect
	{
		public double boost;
		public int bonusHit;

		void gear()
		{
			bonusHit++;
			boost += GEAR;
		}
	}

	private Modifiers()
	{
	}

	public static Effect evaluate(StandContext c)
	{
		Effect e = new Effect();

		if (holidayItemWorn(c))
		{
			e.gear();
		}

		Set<Integer> boss = Items.BOSS_ITEMS.get(c.bankKey);
		if (boss != null && c.wearingAny(boss))
		{
			e.gear();
		}

		for (Outfit outfit : Items.OUTFITS)
		{
			if (c.at(outfit.bankKey()) && outfit.wornBy(c))
			{
				e.gear();
			}
		}

		Set<Integer> guildCapes = Items.GUILD_CAPES.get(c.bankKey);
		boolean capeBonus = false;
		if (guildCapes != null && c.wearingAny(guildCapes))
		{
			capeBonus = true;
		}
		else if (c.wearingAny(Items.RARE_CAPES))
		{
			capeBonus = true;
		}
		if (capeBonus)
		{
			e.gear();
		}

		if (c.wornValue >= WEALTH_THRESHOLD)
		{
			e.gear();
		}
		if (c.wearingAny(Items.PARTYHATS))
		{
			e.boost += TINY;
		}
		if (c.worn.isEmpty())
		{
			e.boost += TINY;
		}
		if (c.follower)
		{
			e.boost += TINY;
		}
		if (c.wearingAny(Items.RING_OF_WEALTH))
		{
			e.boost += TINY;
		}
		if (c.hitpoints == 1)
		{
			e.gear();
		}

		e.boost += newSpot(c.hoursAtBank);
		e.boost += SMALL * Math.min(c.streakDays, STREAK_CAP_DAYS) / STREAK_CAP_DAYS;

		switch (c.emote)
		{
			case LEAN:
				e.boost += SMALL;
				break;
			case YAWN:
				e.boost += TINY;
				break;
			case FLEX:
				e.gear();
				break;
			case SKILLCAPE:
				if (!capeBonus)
				{
					e.gear();
				}
				break;
			default:
				break;
		}

		lore(c, e);

		e.boost = Math.min(e.boost, XpRoller.MAX_BOOST);
		e.bonusHit = Math.min(e.bonusHit, XpRoller.MAX_BONUS_HIT);
		return e;
	}

	private static void lore(StandContext c, Effect e)
	{
		if (c.at("falador-east", "falador-west") && c.today.getMonth() == Month.JUNE && c.today.getDayOfMonth() == 6)
		{
			e.boost += MEDIUM;
			if (c.wearingAny(Items.ZAMORAK_ROBES))
			{
				e.boost += SMALL;
			}
		}

		if (c.at("draynor"))
		{
			if (c.wearing(Items.BLUE_PARTYHAT) && c.wearing(Items.FAKE_BEARD))
			{
				e.gear();
			}
			if (c.wearing(Items.CABBAGE))
			{
				e.boost += TINY;
			}
		}

		if (c.at("edgeville") && c.wearing(1163) && c.wearing(1127) && (c.wearing(1079) || c.wearing(1093))
			&& c.wearingAny(Items.GOD_CAPES))
		{
			e.gear();
		}
	}

	/** Full at a bank you've never stood at, fading to nothing over the first ten hours. */
	static double newSpot(double hours)
	{
		if (hours >= NEW_SPOT_HOURS)
		{
			return 0;
		}
		return MEDIUM * (1 - Math.max(0, hours) / NEW_SPOT_HOURS);
	}

	static boolean holidayItemWorn(StandContext c)
	{
		LocalDate d = c.today;
		int year = d.getYear();
		if (within(d, LocalDate.of(year, 12, 20), LocalDate.of(year, 12, 31)) && c.wearingAny(Items.CHRISTMAS))
		{
			return true;
		}
		if ((within(d, LocalDate.of(year, 10, 25), LocalDate.of(year, 10, 31)) || d.equals(LocalDate.of(year, 11, 1)))
			&& c.wearingAny(Items.HALLOWEEN))
		{
			return true;
		}
		LocalDate easter = easterSunday(year);
		if (within(d, easter.minusDays(3), easter.plusDays(3)) && c.wearingAny(Items.EASTER))
		{
			return true;
		}
		return within(d, LocalDate.of(year, 2, 20), LocalDate.of(year, 2, 24)) && c.wearingAny(Items.BIRTHDAY);
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
