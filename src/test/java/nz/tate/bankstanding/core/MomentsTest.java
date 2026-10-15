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
		t.world = 301;
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
		assertTrue(stand(new Progress(), new Moments.Watch(), tick(DAY, 12)).isEmpty());
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
}
