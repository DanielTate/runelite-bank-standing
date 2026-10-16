package nz.tate.bankstanding.core;

import java.util.Collections;
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

	private static Moments.Tick tick(LocalDate day, int hour)
	{
		Moments.Tick t = new Moments.Tick();
		t.now = 1000;
		t.day = day;
		t.local = day.atTime(hour, 0);
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
	public void maxHitOnlyCountsForTheLogFromAMaxOfTen()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		assertEquals(Moments.Splat.HIT, Moments.drop(p, w, 2, true, false, null, 2, DAY, 0, odd));
		assertNull(Moments.drop(p, w, 9, true, false, null, 9, DAY.plusDays(1), 1, odd));
		assertFalse(Moments.has(p, Moments.Splat.MAX_HIT.key));
		assertTrue(odd.isEmpty());
		assertEquals(Moments.Splat.MAX_HIT, Moments.drop(p, w, 10, true, false, null, 10, DAY, 2, odd));
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
	public void firstTryIsOncePerDayEvenAfterARestart()
	{
		Progress p = new Progress();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		Moments.drop(p, new Moments.Watch(), 1, false, false, null, 20, DAY, 0, odd);
		// A restart brings a new watch, but the day's first roll has already gone.
		Moments.drop(p, new Moments.Watch(), 20, true, false, null, 20, DAY, 0, odd);
		assertFalse(odd.contains(Moments.Odd.FIRST_TRY));
		Moments.drop(p, new Moments.Watch(), 20, true, false, null, 20, DAY.plusDays(1), 0, odd);
		assertTrue(odd.contains(Moments.Odd.FIRST_TRY));
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

	private static Moments.Tick at(String bank, LocalDate day, int hour)
	{
		Moments.Tick t = tick(day, hour);
		t.namedBank = bank;
		return t;
	}

	@Test
	public void placesAreAllHidden()
	{
		for (Moments.Odd moment : Moments.Odd.values())
		{
			if (moment.category == Moments.Category.PLACES)
			{
				assertTrue(moment.key, moment.hidden);
			}
		}
	}

	@Test
	public void placesAreFoundAtTheirBank()
	{
		Progress p = new Progress();
		assertTrue(stand(p, new Moments.Watch(), at("tutorial-island", DAY, 12)).contains(Moments.Odd.WHERE_IT_BEGAN));
		assertTrue(stand(p, new Moments.Watch(), at("rogues-den", DAY, 3)).contains(Moments.Odd.NIGHT_HEIST));
		assertFalse(stand(p, new Moments.Watch(), at("port-phasmatys", DAY, 1)).contains(Moments.Odd.GRAVEYARD_SHIFT));
	}

	@Test
	public void islandHopperNeedsFiveIslandsInOneUtcDay()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		for (String island : Arrays.asList("lunar-isle", "neitiznot", "jatizso", "ape-atoll"))
		{
			assertFalse(stand(p, w, at(island, DAY, 12)).contains(Moments.Odd.ISLAND_HOPPER));
		}
		assertFalse(stand(p, w, at("fossil-island", DAY.plusDays(1), 12)).contains(Moments.Odd.ISLAND_HOPPER));
		for (String island : Arrays.asList("lunar-isle", "neitiznot", "jatizso"))
		{
			stand(p, w, at(island, DAY.plusDays(1), 12));
		}
		assertTrue(stand(p, w, at("ape-atoll", DAY.plusDays(1), 12)).contains(Moments.Odd.ISLAND_HOPPER));
	}

	@Test
	public void frequentFlyerNeedsTwoRegionsWithinTwoMinutes()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		Moments.Tick varrock = at("varrock-west", DAY, 12);
		stand(p, w, varrock);
		Moments.Tick late = at("seers", DAY, 12);
		late.now = varrock.now + Moments.FREQUENT_FLYER_MILLIS + 1;
		assertFalse(stand(p, w, late).contains(Moments.Odd.FREQUENT_FLYER));
		Moments.Tick quick = at("ge", DAY, 12);
		quick.now = late.now + Moments.FREQUENT_FLYER_MILLIS;
		assertTrue(stand(p, w, quick).contains(Moments.Odd.FREQUENT_FLYER));
	}

	@Test
	public void walkItOffHealsOnePointAtATimeWithoutEating()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		boolean healed = false;
		for (int hp = 1; hp <= 5; hp++)
		{
			Moments.Tick t = tick(DAY, 12);
			t.hitpoints = hp;
			t.maxHitpoints = 5;
			stand(p, w, t);
			healed |= t.healed;
		}
		assertTrue(healed);
		assertTrue(Moments.has(p, Moments.Odd.WALK_IT_OFF.key));

		Progress ate = new Progress();
		Moments.Watch w2 = new Moments.Watch();
		for (int hp : new int[]{1, 2, 5})
		{
			Moments.Tick t = tick(DAY, 12);
			t.hitpoints = hp;
			t.maxHitpoints = 5;
			stand(ate, w2, t);
			assertFalse(t.healed);
		}
		assertFalse(Moments.has(ate, Moments.Odd.WALK_IT_OFF.key));
	}

	@Test
	public void partyPooperNeedsAMinuteOfBalloonsAtFaladorEast()
	{
		Progress p = new Progress();
		Moments.Watch w = new Moments.Watch();
		for (int i = 1; i < Moments.DROP_PARTY_TICKS; i++)
		{
			Moments.Tick t = at("falador-east", DAY, 12);
			t.balloons = true;
			assertFalse(stand(p, w, t).contains(Moments.Odd.PARTY_POOPER));
		}
		Moments.Tick t = at("falador-east", DAY, 12);
		t.balloons = true;
		assertTrue(stand(p, w, t).contains(Moments.Odd.PARTY_POOPER));
	}

	@Test
	public void quickChangeNeedsFiveOutfitsInADay()
	{
		Progress p = new Progress();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		Moments.outfitsWorn(p, DAY, Arrays.asList("outfit:a", "outfit:b", "outfit:c", "outfit:d"), 0, odd);
		Moments.outfitsWorn(p, DAY.plusDays(1), Collections.singletonList("outfit:e"), 0, odd);
		assertTrue(odd.isEmpty());
		Moments.outfitsWorn(p, DAY.plusDays(1), Arrays.asList("outfit:a", "outfit:b", "outfit:c", "outfit:d"), 0, odd);
		assertEquals(EnumSet.of(Moments.Odd.QUICK_CHANGE), odd);
	}

	@Test
	public void fireRollsTheFirstTimeThenHourly()
	{
		Progress p = new Progress();
		Set<Moments.Odd> odd = EnumSet.noneOf(Moments.Odd.class);
		long start = 5_000_000;
		assertTrue(Moments.onFire(p, start, odd));
		assertEquals(EnumSet.of(Moments.Odd.HOT_SEAT), odd);
		assertFalse(Moments.onFire(p, start + Moments.SIT_COOLDOWN_MILLIS - 1, odd));
		assertTrue(Moments.onFire(p, start + Moments.SIT_COOLDOWN_MILLIS, odd));
	}
}
