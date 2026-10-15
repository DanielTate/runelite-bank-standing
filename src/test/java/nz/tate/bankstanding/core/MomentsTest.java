package nz.tate.bankstanding.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MomentsTest
{
	private static final LocalDate DAY = LocalDate.of(2026, 10, 15);

	private static Moments.Tick tick(LocalDate utcDay, int hour)
	{
		Moments.Tick t = new Moments.Tick();
		t.now = 1000;
		t.utcDay = utcDay;
		t.local = utcDay.atTime(hour, 0);
		t.world = 302;
		t.prayer = 50;
		return t;
	}

	private static Set<Moments.Odd> stand(Progress p, Moments.Watch w, Moments.Tick t)
	{
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		Moments.standingTick(p, w, t, EnumSet.noneOf(Moments.HolidayEntry.class), odd);
		return odd;
	}

	@Test
	public void eachSplatIsNewOnlyOnce()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		assertEquals(Moments.Splat.HIT, Moments.drop(p, w, false, false, null, 20, 1, odd));
		assertNull(Moments.drop(p, w, false, false, null, 20, 2, odd));
		assertEquals(Moments.Splat.PHRASE_BANK, Moments.drop(p, w, true, false, Keywords.Kind.LOCATION, 20, 3, odd));
		assertEquals(Moments.Splat.BOOSTED, Moments.drop(p, w, false, true, null, 20, 4, odd));
	}

	@Test
	public void backToBackNeedsTwoMaxHitsInARowAtTenOrMore()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		Moments.drop(p, w, true, false, null, 9, 0, odd);
		Moments.drop(p, w, true, false, null, 9, 0, odd);
		assertTrue(odd.isEmpty());
		Moments.drop(p, w, true, false, null, 10, 0, odd);
		Moments.drop(p, w, false, false, null, 10, 0, odd);
		Moments.drop(p, w, true, false, null, 10, 0, odd);
		assertTrue(odd.isEmpty());
		Moments.drop(p, w, true, false, null, 10, 0, odd);
		assertEquals(EnumSet.of(Moments.Odd.BACK_TO_BACK), odd);
	}

	@Test
	public void holidaysAreNotedOnTheDay()
	{
		Progress p = new Progress();
		Set<Moments.HolidayEntry> holidays = EnumSet.noneOf(Moments.HolidayEntry.class);
		Moments.standingTick(p, new Moments.Watch(), tick(LocalDate.of(2026, 10, 31), 12), holidays,
			EnumSet.noneOf(Moments.Odd.class));
		assertEquals(EnumSet.of(Moments.HolidayEntry.HALLOWEEN), holidays);
		assertTrue(Moments.has(p, Moments.HolidayEntry.HALLOWEEN.key()));
	}

	@Test
	public void midnightNeedsTheSameStandAcrossTheDayChange()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		stand(p, w, tick(DAY, 12));
		w.standEnded();
		assertFalse(stand(p, w, tick(DAY.plusDays(1), 12)).contains(Moments.Odd.MIDNIGHT));
		assertTrue(stand(p, w, tick(DAY.plusDays(2), 12)).contains(Moments.Odd.MIDNIGHT));
	}

	@Test
	public void datesAndHours()
	{
		assertTrue(stand(new Progress(), new Moments.Watch(), tick(DAY, 3)).contains(Moments.Odd.NIGHT_SHIFT));
		assertTrue(stand(new Progress(), new Moments.Watch(), tick(LocalDate.of(2026, 11, 13), 12))
			.contains(Moments.Odd.FRIDAY_13TH));
		assertTrue(stand(new Progress(), new Moments.Watch(), tick(LocalDate.of(2028, 2, 29), 12))
			.contains(Moments.Odd.LEAP_DAY));
		assertTrue(stand(new Progress(), new Moments.Watch(), tick(DAY, 14)).isEmpty());
		assertTrue(stand(new Progress(), new Moments.Watch(), tick(DAY, 12)).contains(Moments.Odd.LUNCH_BREAK));
		assertTrue(stand(new Progress(), new Moments.Watch(), tick(DAY, 5)).contains(Moments.Odd.EARLY_BIRD));
	}

	@Test
	public void statueNeedsAnHourOfStillCamera()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Moments.Tick t = tick(DAY, 12);
		for (long i = 0; i < Moments.STATUE_TICKS; i++)
		{
			assertFalse(stand(p, w, t).contains(Moments.Odd.STATUE));
		}
		assertTrue(stand(p, w, t).contains(Moments.Odd.STATUE));
	}

	@Test
	public void outOfFaithNeedsAnHourAtZeroPrayer()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Moments.Tick t = tick(DAY, 12);
		t.prayer = 0;
		for (long i = 1; i < Moments.OUT_OF_FAITH_TICKS; i++)
		{
			stand(p, w, t);
		}
		t.prayer = 1;
		stand(p, w, t);
		t.prayer = 0;
		for (long i = 1; i < Moments.OUT_OF_FAITH_TICKS; i++)
		{
			assertFalse(stand(p, w, t).contains(Moments.Odd.OUT_OF_FAITH));
		}
		assertTrue(stand(p, w, t).contains(Moments.Odd.OUT_OF_FAITH));
	}

	@Test
	public void bankHopperAndWorldTour()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Set<Moments.Odd> seen = EnumSet.noneOf(Moments.Odd.class);
		for (String bank : Arrays.asList("ge", "edgeville", "draynor", "catherby", "seers"))
		{
			Moments.Tick t = tick(DAY, 12);
			t.namedBank = bank;
			t.world = 300 + bank.length() + seen.size();
			seen.addAll(stand(p, w, t));
		}
		assertTrue(seen.contains(Moments.Odd.BANK_HOPPER));
	}

	@Test
	public void worldTourResetsEachDay()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		for (int world = 301; world <= 304; world++)
		{
			Moments.Tick t = tick(DAY, 12);
			t.world = world;
			stand(p, w, t);
		}
		Moments.Tick t = tick(DAY.plusDays(1), 12);
		t.world = 305;
		assertFalse(stand(p, w, t).contains(Moments.Odd.WORLD_TOUR));
	}

	@Test
	public void backfillMarksWhatOldSavesShow()
	{
		Progress p = new Progress();
		p.rolls = 5;
		Keywords keywords = new Keywords(Arrays.asList(Keywords.phrase(Keywords.hash("at the bank"), null, "ge")));
		p.keywordPaid.put(Keywords.hash("at the bank"), 1L);
		Moments.backfill(p, keywords);
		assertEquals(Long.valueOf(Moments.BEFORE_TRACKING), p.moments.get(Moments.Splat.HIT.key));
		assertTrue(Moments.has(p, Moments.Splat.PHRASE_BANK.key));
		assertFalse(Moments.has(p, Moments.Splat.MAX_HIT.key));
	}

	@Test
	public void localTimeIsUsedForNightShift()
	{
		Moments.Tick t = tick(DAY, 12);
		t.local = LocalDateTime.of(2026, 10, 16, 3, 30);
		assertTrue(stand(new Progress(), new Moments.Watch(), t).contains(Moments.Odd.NIGHT_SHIFT));
	}

	@Test
	public void weekendWeekAndStreaksComeFromTheDaysStood()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Set<Moments.Odd> seen = EnumSet.noneOf(Moments.Odd.class);
		LocalDate monday = LocalDate.of(2026, 10, 12);
		for (int i = 0; i < 7; i++)
		{
			seen.addAll(stand(p, w, tick(monday.plusDays(i), 14)));
		}
		assertTrue(seen.contains(Moments.Odd.WEEKEND_WARRIOR));
		assertTrue(seen.contains(Moments.Odd.FULL_WEEK));
		assertTrue(seen.contains(Moments.Odd.STREAK_7));
		assertFalse(seen.contains(Moments.Odd.STREAK_30));
		assertFalse(seen.contains(Moments.Odd.ALL_YEAR));
	}

	@Test
	public void streakBreaksOnAMissedDay()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Set<Moments.Odd> seen = EnumSet.noneOf(Moments.Odd.class);
		for (int i = 0; i < 8; i++)
		{
			if (i != 3)
			{
				seen.addAll(stand(p, w, tick(DAY.plusDays(i), 14)));
			}
		}
		assertFalse(seen.contains(Moments.Odd.STREAK_7));
	}

	@Test
	public void roughPatchAndFirstTry()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		Moments.drop(p, w, 20, true, false, null, 20, DAY, 0, odd);
		assertEquals(EnumSet.of(Moments.Odd.FIRST_TRY), odd);
		odd.clear();
		Moments.drop(p, w, 1, false, false, null, 20, DAY, 0, odd);
		Moments.drop(p, w, 1, false, false, null, 20, DAY, 0, odd);
		assertTrue(odd.isEmpty());
		Moments.drop(p, w, 1, false, false, null, 20, DAY, 0, odd);
		assertEquals(EnumSet.of(Moments.Odd.ROUGH_PATCH), odd);
	}

	@Test
	public void lightningNeedsTwoRareDropsInAnHour()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		Moments.rareDrop(p, w, 0, odd);
		Moments.rareDrop(p, w, Moments.LIGHTNING_MILLIS + 1, odd);
		assertTrue(odd.isEmpty());
		Moments.rareDrop(p, w, 2 * Moments.LIGHTNING_MILLIS, odd);
		assertEquals(EnumSet.of(Moments.Odd.LIGHTNING), odd);
	}

	@Test
	public void crowdsWorldsAndPoison()
	{
		Moments.Tick t = tick(DAY, 14);
		t.playersAround = 0;
		t.namedBank = "ge";
		t.world = 301;
		t.pvpWorld = true;
		t.poisoned = true;
		Set<Moments.Odd> odd = stand(new Progress(), new Moments.Watch(), t);
		assertTrue(odd.contains(Moments.Odd.GHOST_TOWN));
		assertTrue(odd.contains(Moments.Odd.THE_ORIGINAL));
		assertTrue(odd.contains(Moments.Odd.LIVING_DANGEROUSLY));
		assertTrue(odd.contains(Moments.Odd.POISONED));
		assertFalse(odd.contains(Moments.Odd.OWN_MEDICINE));

		t = tick(DAY, 14);
		t.playersAround = 60;
		t.stillestNeighbourTicks = Moments.GOOD_COMPANY_TICKS;
		t.poisoned = true;
		t.wearingPoison = true;
		t.skillTotalWorld = true;
		odd = stand(new Progress(), new Moments.Watch(), t);
		assertTrue(odd.contains(Moments.Odd.RUSH_HOUR));
		assertTrue(odd.contains(Moments.Odd.GOOD_COMPANY));
		assertTrue(odd.contains(Moments.Odd.OWN_MEDICINE));
		assertTrue(odd.contains(Moments.Odd.ELITE_COMPANY));
		assertFalse(odd.contains(Moments.Odd.GHOST_TOWN));
	}

	@Test
	public void standLengths()
	{
		Moments.Tick t = tick(DAY, 14);
		t.playersAround = 5;
		t.standTicks = Moments.COFFEE_BREAK_TICKS;
		t.bankTicks = Moments.LOYAL_CUSTOMER_TICKS;
		Progress p = new Progress();
		p.todayTicks = Moments.FULL_SHIFT_TICKS;
		Set<Moments.Odd> odd = stand(p, new Moments.Watch(), t);
		assertTrue(odd.contains(Moments.Odd.COFFEE_BREAK));
		assertTrue(odd.contains(Moments.Odd.LOYAL_CUSTOMER));
		assertTrue(odd.contains(Moments.Odd.FULL_SHIFT));
		assertFalse(odd.contains(Moments.Odd.OVERTIME));
	}

	@Test
	public void keysAreUnique()
	{
		Set<String> keys = new java.util.HashSet<>();
		for (Moments.Odd moment : Moments.Odd.values())
		{
			assertTrue(keys.add(moment.key));
		}
	}

	@Test
	public void sittingPaysOutOnceAnHour()
	{
		Progress p = new Progress();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		assertTrue(Moments.sat(p, Moments.SIT_COOLDOWN_MILLIS, odd));
		assertEquals(EnumSet.of(Moments.Odd.TAKE_A_SEAT), odd);
		assertFalse(Moments.sat(p, 2 * Moments.SIT_COOLDOWN_MILLIS - 1, odd));
		assertTrue(Moments.sat(p, 2 * Moments.SIT_COOLDOWN_MILLIS, odd));
	}
}
