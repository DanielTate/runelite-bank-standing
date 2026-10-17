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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ModifiersTest
{
	private static final double EPS = 1e-9;

	private static StandContext at(String bank, Integer... worn)
	{
		StandContext c = new StandContext();
		c.bankKey = bank;
		c.worn = new HashSet<>(Arrays.asList(worn));
		c.today = LocalDate.of(2026, 8, 1);
		c.hoursAtBank = Modifiers.NEW_SPOT_HOURS;
		return c;
	}

	@Test
	public void ordinaryStandHasNoBonus()
	{
		Modifiers.Effect e = Modifiers.evaluate(at("ge", 4151));
		assertEquals(0, e.boost, EPS);
		assertEquals(0, e.bonusHit);
	}

	@Test
	public void hardToReachBanksGiveALittleMore()
	{
		assertEquals(Modifiers.MEDIUM, Modifiers.evaluate(at("lunar-isle", 4151)).boost, EPS);
		assertEquals(0, Modifiers.evaluate(at("ge", 4151)).boost, EPS);
	}

	@Test
	public void fullZaryteAtNex()
	{
		assertEquals(1, Modifiers.evaluate(at("nex", 26235, 26374)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("nex", 26235)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("ge", 26235, 26374)).bonusHit);
	}

	@Test
	public void fullLunarAtLunarIsle()
	{
		assertEquals(1, Modifiers.evaluate(at("lunar-isle", 9096, 9097, 9098, 9099, 9100, 9101)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("lunar-isle", 9096, 9097, 9098, 9099, 9100)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("ge", 9096, 9097, 9098, 9099, 9100, 9101)).bonusHit);
	}

	@Test
	public void outfitsCanBelongToSeveralBanks()
	{
		assertEquals(1, Modifiers.evaluate(at("tzhaar", 21298, 21301, 21304)).bonusHit);
		assertEquals(1, Modifiers.evaluate(at("mor-ul-rek", 21298, 21301, 21304)).bonusHit);
		assertEquals(1, Modifiers.evaluate(at("falador-west", 6623, 6617, 6627)).bonusHit);
	}

	@Test
	public void mixedMoonPiecesCount()
	{
		assertEquals(1, Modifiers.evaluate(at("cam-torum", 29010, 29013, 29025)).bonusHit);
	}

	@Test
	public void diaryItemsCountInTheirRegion()
	{
		assertEquals(1, Modifiers.evaluate(at("ardougne-north", 13124)).bonusHit);
		assertEquals(1, Modifiers.evaluate(at("hosidius", 22941)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("ge", 13124)).bonusHit);
		assertTrue(Modifiers.evaluate(at("seers", 13137)).found.contains("diary:seers"));
	}

	@Test
	public void clanCloakAndVexillumAtTheClanHall()
	{
		assertEquals(1, Modifiers.evaluate(at("clan-hall", 25716, 25725)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("clan-hall", 25716)).bonusHit);
	}

	@Test
	public void jesterAtJatizso()
	{
		assertEquals(1, Modifiers.evaluate(at("jatizso", 10836, 10837, 10838, 10839, 10840)).bonusHit);
	}

	@Test
	public void fullInventoryOfFishAtTheFishingGuild()
	{
		StandContext c = at("fishing-guild", 4151);
		c.inventory = new int[28];
		java.util.Arrays.fill(c.inventory, 383);
		c.inventory[0] = 371;
		assertEquals(1, Modifiers.evaluate(c).bonusHit);
		assertTrue(Modifiers.evaluate(c).found.contains("haul:fishing-guild"));
		c.inventory[5] = -1;
		assertEquals(0, Modifiers.evaluate(c).bonusHit);

		c = at("woodcutting-guild", 4151);
		c.inventory = new int[28];
		java.util.Arrays.fill(c.inventory, 1515);
		assertEquals(1, Modifiers.evaluate(c).bonusHit);
		c.bankKey = "ge";
		assertEquals(0, Modifiers.evaluate(c).bonusHit);
	}

	@Test
	public void collectionLogKeysAreUnique()
	{
		java.util.Set<String> keys = new HashSet<>();
		for (Outfit outfit : Items.OUTFITS)
		{
			assertTrue(outfit.key(), keys.add("outfit:" + outfit.key()));
		}
		for (Outfit diary : Items.DIARY)
		{
			assertTrue(diary.key(), keys.add("diary:" + diary.key()));
		}
		for (Modifiers.Haul haul : Modifiers.HAULS)
		{
			assertTrue(haul.bankKey, keys.add("haul:" + haul.bankKey));
		}
	}

	@Test
	public void foundByGroupCountsOnlyKnownKeys()
	{
		java.util.Map<String, Long> moments = new java.util.HashMap<>();
		moments.put("outfit:lunar-isle", 1L);
		moments.put("outfit:not-a-bank", 1L);
		java.util.Map<String, int[]> groups = Modifiers.foundByGroup(moments);
		assertEquals(1, groups.get("Outfits")[0]);
		assertEquals(Items.OUTFITS.size(), groups.get("Outfits")[1]);
		assertEquals(0, groups.get("Diary items")[0]);
		assertEquals(Modifiers.HAULS.size(), groups.get("Full inventories")[1]);
	}

	@Test
	public void bossItemOnlyCountsAtItsBank()
	{
		assertEquals(1, Modifiers.evaluate(at("canifis", 4151)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("edgeville", 4151)).bonusHit);
	}

	@Test
	public void outfitsNeedTheFullSet()
	{
		assertEquals(1, Modifiers.evaluate(at("fishing-guild", 13258, 13259, 13260, 13261)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("fishing-guild", 13258, 13259, 13260)).bonusHit);
		assertEquals("mixed spirit and normal pieces count", 1,
			Modifiers.evaluate(at("fishing-guild", 25592, 13259, 25596, 13261)).bonusHit);
	}

	@Test
	public void outfitAndSkillcapeStackAtTheGuild()
	{
		assertEquals(2, Modifiers.evaluate(at("fishing-guild", 13258, 13259, 13260, 13261, 9799)).bonusHit);
	}

	@Test
	public void rareCapeCountsAnywhereButOnlyOnce()
	{
		assertEquals(1, Modifiers.evaluate(at("ge", 9771)).bonusHit);
		StandContext c = at("ge", 9771);
		c.emote = Emote.SKILLCAPE;
		assertEquals("cape emote doesn't stack with a cape bonus", 1, Modifiers.evaluate(c).bonusHit);
	}

	@Test
	public void holidayItemsOnlyCountInTheirWindow()
	{
		StandContext c = at("ge", 1050);
		c.today = LocalDate.of(2026, 12, 25);
		assertEquals(1, Modifiers.evaluate(c).bonusHit);
		c.today = LocalDate.of(2026, 12, 19);
		assertEquals(0, Modifiers.evaluate(c).bonusHit);
	}

	@Test
	public void easterMovesEachYear()
	{
		assertEquals(LocalDate.of(2026, 4, 5), Holiday.easterSunday(2026));
		assertEquals(LocalDate.of(2027, 3, 28), Holiday.easterSunday(2027));
		StandContext c = at("ge", 1037);
		c.today = LocalDate.of(2027, 3, 26);
		assertTrue(Modifiers.holidayItemWorn(c));
	}

	@Test
	public void newSpotFadesOverTenHours()
	{
		assertEquals(Modifiers.MEDIUM, Modifiers.newSpot(0), EPS);
		assertEquals(Modifiers.MEDIUM / 2, Modifiers.newSpot(5), EPS);
		assertEquals(0, Modifiers.newSpot(10), EPS);
	}

	@Test
	public void streakCapsAtFiveDays()
	{
		StandContext c = at("ge");
		c.worn = new HashSet<>(Arrays.asList(4151));
		c.streakDays = 9;
		assertEquals(Modifiers.SMALL, Modifiers.evaluate(c).boost, EPS);
	}

	@Test
	public void wiseOldManHeistNeedsHatAndBeardAtDraynor()
	{
		assertEquals(1, Modifiers.evaluate(at("draynor", 1042, 4593)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("varrock-west", 1042, 4593)).bonusHit);
	}

	@Test
	public void edgevillePkerNeedsFullRuneAndAGodCape()
	{
		assertEquals(1, Modifiers.evaluate(at("edgeville", 1163, 1127, 1093, 2414)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("edgeville", 1163, 1127, 1093)).bonusHit);
	}

	@Test
	public void everythingTogetherIsCapped()
	{
		StandContext c = at("fishing-guild", 13258, 13259, 13260, 13261, 9799, 1048, 2572);
		c.today = LocalDate.of(2026, 12, 25);
		c.wornValue = 2_000_000_000L;
		c.hitpoints = 1;
		c.follower = true;
		c.streakDays = 5;
		c.hoursAtBank = 0;
		c.emote = Emote.FLEX;

		Modifiers.Effect e = Modifiers.evaluate(c);
		assertEquals(XpRoller.MAX_BOOST, e.boost, EPS);
		assertEquals(XpRoller.MAX_BONUS_HIT, e.bonusHit);
	}

	@Test
	public void maxCapeCountsOnceLikeARareCape()
	{
		assertEquals(1, Modifiers.evaluate(at("ge", 13280)).bonusHit);
		assertEquals(1, Modifiers.evaluate(at("fishing-guild", 13280, 9798)).bonusHit);
	}

	@Test
	public void skillingPetAtItsHomeBank()
	{
		StandContext c = at("fishing-guild", 4151);
		c.followerName = "heron";
		Modifiers.Effect e = Modifiers.evaluate(c);
		assertEquals(Modifiers.SMALL, e.boost, EPS);
		assertTrue(e.found.contains("skilling-pet:fishing-guild"));
		c.bankKey = "ge";
		assertEquals(0, Modifiers.evaluate(c).boost, EPS);
	}

	@Test
	public void fullBarrowsSetAtCanifisAndDharokAtOneHitpoint()
	{
		assertEquals(1, Modifiers.evaluate(at("canifis", 4716, 4718, 4720, 4722)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("canifis", 4716, 4718, 4720)).bonusHit);
		assertEquals(0, Modifiers.evaluate(at("ge", 4716, 4718, 4720, 4722)).bonusHit);
		StandContext dharok = at("ferox", 4880, 4886, 4892, 4898);
		dharok.hitpoints = 1;
		Modifiers.Effect e = Modifiers.evaluate(dharok);
		// One for standing at 1 hitpoint, one for the set, and a little more for both together.
		assertEquals(2, e.bonusHit);
		assertEquals(2 * Modifiers.GEAR + Modifiers.SMALL, e.boost, EPS);
	}

	@Test
	public void gnomeChildHatAndSkullAtTheirBanks()
	{
		assertEquals(Modifiers.TINY, Modifiers.evaluate(at("gnome-stronghold-north", 13655)).boost, EPS);
		assertEquals(0, Modifiers.evaluate(at("ge", 13655)).boost, EPS);
		StandContext skulled = at("edgeville", 4151);
		skulled.skulled = true;
		assertEquals(Modifiers.SMALL, Modifiers.evaluate(skulled).boost, EPS);
	}

	@Test
	public void eachSpellbookAtItsOwnBank()
	{
		for (Spellbook book : Spellbook.values())
		{
			StandContext home = at(book.bankKey, 4151);
			home.spellbook = book;
			Modifiers.Effect e = Modifiers.evaluate(home);
			assertEquals(book.name(), 1, e.bonusHit);
			assertTrue(e.found.contains(book.momentKey()));
			assertEquals(book, Spellbook.forMomentKey(book.momentKey()));

			StandContext away = at("ge", 4151);
			away.spellbook = book;
			assertEquals(0, Modifiers.evaluate(away).bonusHit);
		}
	}
}
