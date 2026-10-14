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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Item IDs the modifiers look for. */
final class Items
{
	static final Set<Integer> CHRISTMAS = ids(1050, 13343, 13344, 21859, 12887, 12888, 12889, 12890, 12891,
		12892, 12893, 12894, 12895, 12896, 10507, 21841, 21842, 21843, 21844, 21845, 21846, 26310, 26312,
		26314, 27566, 962);
	static final Set<Integer> HALLOWEEN = ids(1053, 1055, 1057, 11847, 12845, 7592, 7593, 7594, 7595, 7596,
		9920, 9921, 9922, 9923, 9924, 9925, 26256, 26258, 27473, 27475, 27477, 27479, 27481, 27497);
	static final Set<Integer> EASTER = ids(1037, 13182, 13663, 13664, 13665, 7927, 11019, 11020, 11021, 11022,
		20433, 20436, 20439, 20442, 21214, 13185);
	static final Set<Integer> BIRTHDAY = ids(21209, 21211, 25322, 25324, 25326, 25328, 25330, 25332, 25334,
		27804, 27812, 27820, 33080, 33082, 33084, 33086);

	static final Set<Integer> PARTYHATS = ids(1038, 1040, 1042, 1044, 1046, 1048, 11862, 11863, 12399);
	static final int BLUE_PARTYHAT = 1042;
	static final int FAKE_BEARD = 4593;
	static final int CABBAGE = 1965;
	static final Set<Integer> RING_OF_WEALTH = ids(2572, 11980, 11982, 11984, 11986, 11988, 12785, 20786,
		20787, 20788, 20789, 20790);
	static final Set<Integer> ZAMORAK_ROBES = ids(1033, 1035);
	static final Set<Integer> GOD_CAPES = ids(2412, 2413, 2414, 21791, 21793, 21795);
	static final int ZAMORAK_CAPE = 2414;
	static final int IMBUED_ZAMORAK_CAPE = 21795;

	/** Boss uniques, keyed by the bank nearest that boss. */
	static final Map<String, Set<Integer>> BOSS_ITEMS = new HashMap<>();

	static
	{
		BOSS_ITEMS.put("quidamortem", ids(20997, 21000, 21003, 21006, 21012, 21015, 21018, 21021, 21024, 13652));
		BOSS_ITEMS.put("ver-sinhaza", ids(22322, 22323, 22324, 22325, 22326, 22327, 22328, 22481, 22486,
			25731, 25733, 25734, 25736, 25738, 25739, 25741));
		BOSS_ITEMS.put("mor-ul-rek", ids(6570, 21295, 13329, 21284));
		BOSS_ITEMS.put("fortis", ids(28947, 28951, 28955, 28902));
		BOSS_ITEMS.put("canifis", ids(4151, 12773, 12774, 12006, 26482, 26484));
		BOSS_ITEMS.put("warriors-guild", ids(12954, 19722, 27008, 22322));
		BOSS_ITEMS.put("piscatoris", ids(11905, 11907, 11908, 12004, 22288, 22290, 33322, 33323, 33326,
			33328, 33434));
		BOSS_ITEMS.put("shayzien", ids(13576, 26710, 28035));
	}

	static final List<Outfit> OUTFITS = Collections.unmodifiableList(Arrays.asList(
		new Outfit("fishing-guild").piece(13258, 25592, 31252).piece(13259, 25594).piece(13260, 25596).piece(13261, 25598),
		new Outfit("woodcutting-guild").piece(10941, 28173).piece(10939, 28169).piece(10940, 28171).piece(10933, 28175),
		new Outfit("motherlode").piece(12013, 25549, 29472).piece(12014, 25551, 29474).piece(12015, 25553, 29476)
			.piece(12016, 25555, 29478),
		new Outfit("farming-guild").piece(13646, 13647, 21253, 21254).piece(13642, 13643).piece(13640, 13641)
			.piece(13644, 13645),
		new Outfit("wintertodt").piece(20708).piece(20704).piece(20706).piece(20710),
		new Outfit("hunters-guild").piece(29263).piece(29265).piece(29267).piece(29269),
		new Outfit("cooking-guild").piece(20205).piece(20208),
		new Outfit("crafting-guild").piece(1757, 20208),
		new Outfit("rogues-den").piece(5554).piece(5553).piece(5555).piece(5556).piece(5557),
		new Outfit("varrock-east").piece(27023).piece(27025).piece(27027).piece(27029, 27031),
		new Outfit("pest-control").piece(11663, 11664, 11665, 24183, 24184, 24185, 26473, 26475, 26477, 27005)
			.piece(8839, 13072, 24177, 24178, 26463, 26469, 27000, 27003)
			.piece(8840, 13073, 24179, 24180, 26465, 26471, 27001, 27004)
			.piece(8842, 24182, 26467, 27002),
		new Outfit("barbarian-outpost").piece(10551, 24175, 28067, 28069).piece(10555, 24176).piece(10553, 10554),
		new Outfit("castle-wars").piece(4071, 4506, 4511).piece(4069, 4504, 4509).piece(4070, 4505, 4510),
		new Outfit("nardah").piece(1833).piece(1835).piece(1837),
		new Outfit("nex").piece(26235).piece(26374)
	));

	/** 99 skillcapes (trimmed and untrimmed), keyed by the bank that is home to that skill. */
	static final Map<String, Set<Integer>> GUILD_CAPES = new HashMap<>();

	static
	{
		GUILD_CAPES.put("fishing-guild", ids(9798, 9799));
		GUILD_CAPES.put("woodcutting-guild", ids(9807, 9808));
		GUILD_CAPES.put("motherlode", ids(9792, 9793));
		GUILD_CAPES.put("farming-guild", ids(9810, 9811));
		GUILD_CAPES.put("cooking-guild", ids(9801, 9802));
		GUILD_CAPES.put("crafting-guild", ids(9780, 9781));
		GUILD_CAPES.put("wintertodt", ids(9804, 9805));
		GUILD_CAPES.put("warriors-guild", ids(9747, 9748, 9750, 9751, 9753, 9754));
		GUILD_CAPES.put("rogues-den", ids(9777, 9778));
		GUILD_CAPES.put("varrock-east", ids(9795, 9796));
		GUILD_CAPES.put("canifis", ids(9786, 9787));
		GUILD_CAPES.put("hunters-guild", ids(9948, 9949));
	}

	/**
	 * The three rarest 99s by hiscore count (Sailing, Runecraft, Agility as of 1 October 2026,
	 * per the OSRS Wiki's Cape of Accomplishment page). Refresh with updates.
	 */
	static final Set<Integer> RARE_CAPES = ids(31288, 31290, 9765, 9766, 9771, 9772);

	private Items()
	{
	}

	static Set<Integer> ids(Integer... ids)
	{
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ids)));
	}
}
