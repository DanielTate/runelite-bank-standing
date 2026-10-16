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
		BOSS_ITEMS.put("mor-ul-rek", ids(6570, 21295, 13329, 21285));
		BOSS_ITEMS.put("fortis", ids(28947, 28951, 28955, 28902));
		BOSS_ITEMS.put("canifis", ids(4151, 12773, 12774, 12006, 26482, 26484));
		BOSS_ITEMS.put("warriors-guild", ids(12954, 19722, 27008, 22322));
		BOSS_ITEMS.put("piscatoris", ids(11905, 11907, 11908, 12004, 22288, 22290, 33322, 33323, 33326,
			33328, 33434));
		BOSS_ITEMS.put("shayzien", ids(13576, 26710, 28035));
	}

	/** Graceful in every colour, both item IDs of each piece. */
	static final Integer[] GRACEFUL_HOODS = {11850, 11851, 13579, 13580, 13591, 13592, 13603, 13604, 13615, 13616, 13627, 13628, 13667, 13668,
		21061, 21063, 24743, 24745, 25069, 25071, 27444, 27446, 30045, 30047};
	static final Integer[] GRACEFUL_TOPS = {11854, 11855, 13583, 13584, 13595, 13596, 13607, 13608, 13619, 13620, 13631, 13632, 13671, 13672,
		21067, 21069, 24749, 24751, 25075, 25077, 27450, 27452, 30051, 30053};
	static final Integer[] GRACEFUL_LEGS = {11856, 11857, 13585, 13586, 13597, 13598, 13609, 13610, 13621, 13622, 13633, 13634, 13673, 13674,
		21070, 21072, 24752, 24754, 25078, 25080, 27453, 27455, 30054, 30056};
	static final Integer[] GRACEFUL_GLOVES = {11858, 11859, 13587, 13588, 13599, 13600, 13611, 13612, 13623, 13624, 13635, 13636, 13675, 13676,
		21073, 21075, 24755, 24757, 25081, 25083, 27456, 27458, 30057, 30059};
	static final Integer[] GRACEFUL_BOOTS = {11860, 11861, 13589, 13590, 13601, 13602, 13613, 13614, 13625, 13626, 13637, 13638, 13677, 13678,
		21076, 21078, 24758, 24760, 25084, 25086, 27459, 27461, 30060, 30062};
	static final Integer[] GRACEFUL_CAPES = {11852, 11853, 13581, 13582, 13593, 13594, 13605, 13606, 13617, 13618, 13629, 13630, 13669, 13670,
		21064, 21066, 24746, 24748, 25072, 25074, 27447, 27449, 30048, 30050};

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
		new Outfit("nex").piece(26235).piece(26374),
		new Outfit("lunar-isle").piece(9096).piece(9097).piece(9098).piece(9099).piece(9100).piece(9101),
		new Outfit("tzhaar", "mor-ul-rek").piece(21298).piece(21301).piece(21304),
		new Outfit("tombs-of-amascut").piece(27226, 27235).piece(27229, 27238, 33190).piece(27232, 27241, 33192),
		new Outfit("colosseum").piece(28933).piece(28936).piece(28939),
		new Outfit("cam-torum").piece(29010, 29019, 29028).piece(29004, 29013, 29022).piece(29007, 29016, 29025),
		new Outfit("guardians-of-the-rift").piece(26850, 26858, 26864, 26870).piece(26852, 26860, 26866, 26872)
			.piece(26854, 26862, 26868, 26874).piece(26856),
		new Outfit("arceuus").piece(25398, 25413).piece(25389, 25404).piece(25401, 25416).piece(25392, 25407)
			.piece(25395, 25410),
		new Outfit("prifddinas-north", "prifddinas-south")
			.piece(23971, 27705, 27717, 27729, 27741, 27753, 27765, 27777, 33031, 33170)
			.piece(23975, 27697, 27709, 27721, 27733, 27745, 27757, 27769, 33023, 33166)
			.piece(23979, 27701, 27713, 27725, 27737, 27749, 27761, 27773, 33027, 33168),
		new Outfit("falador-east", "falador-west").piece(6623).piece(6617).piece(6625, 6627),
		new Outfit("lumbridge").piece(4302).piece(4298).piece(4300).piece(4304).piece(4306).piece(4308).piece(4310),
		new Outfit("port-phasmatys").piece(6109).piece(6107).piece(6108).piece(6110).piece(6106).piece(6111),
		new Outfit("darkmeyer").piece(24676, 24673).piece(24678, 24674).piece(24680, 24675),
		new Outfit("neitiznot").piece(10828, 23591, 28070).piece(10822).piece(10824).piece(10826),
		new Outfit("giants-foundry").piece(27023).piece(27025).piece(27027).piece(27029, 27031),
		new Outfit("shayzien").piece(13379).piece(13381).piece(13380).piece(13377).piece(13378),
		new Outfit("draynor").piece(579, 7394, 7396).piece(577, 7390, 7392).piece(1011, 7386, 7388),
		new Outfit("edgeville").piece(544, 20199, 23303).piece(542, 20202, 23306),
		new Outfit("ardougne-north", "ardougne-south").piece(1506).piece(6065).piece(6067).piece(6068).piece(6069)
			.piece(6070),
		new Outfit("burgh-de-rott").piece(25438).piece(25434).piece(25436).piece(25440),
		new Outfit("hosidius", "hosidius-kitchen").piece(24872).piece(24874).piece(24876).piece(24878),
		new Outfit("ferox").piece(20595, 27119, 27176).piece(20517, 27115, 27174).piece(20520, 27117, 27175),
		new Outfit("hallowed-sepulchre").piece(GRACEFUL_HOODS).piece(GRACEFUL_TOPS).piece(GRACEFUL_LEGS)
			.piece(GRACEFUL_GLOVES).piece(GRACEFUL_BOOTS).piece(GRACEFUL_CAPES),
		new Outfit("ape-atoll").piece(4024, 4025, 4026, 4027, 4028, 4029, 4030, 4031, 19525),
		new Outfit("clan-hall").piece(25712, 25714, 25715, 25716, 25717, 25718, 25719, 25720)
			.piece(25721, 25723, 25724, 25725, 25726, 25727, 25728, 25729),
		new Outfit("jatizso").piece(10836).piece(10837).piece(10838).piece(10839).piece(10840)
	));

	/** Raw fish, for a full inventory at the Fishing Guild. */
	static final Set<Integer> RAW_FISH = ids(317, 321, 327, 331, 335, 338, 341, 345, 349, 353, 359, 363, 371, 377,
		383, 389, 395, 2148, 3142, 3150, 3379, 5001, 7944, 10138, 11328, 11330, 11332, 11934, 13439, 23872, 25564,
		25652, 25658, 25664, 25670, 29216, 31553, 31561, 32309, 32317, 32325, 32333, 32341, 32349);
	/** Logs you can cut, for a full inventory at the Woodcutting Guild. */
	static final Set<Integer> LOGS = ids(1511, 1513, 1515, 1517, 1519, 1521, 2862, 6332, 6333, 10810, 13355, 19669,
		24691, 32902, 32904, 32907, 32910);

	static final Set<Integer> ORES = ids(434, 436, 438, 440, 442, 444, 447, 449, 451, 453, 21347);
	static final Set<Integer> BARS = ids(2349, 2351, 2353, 2355, 2357, 2359, 2361, 2363);
	static final Set<Integer> RUNE_ESSENCE = ids(1436, 7936, 24704);
	static final Set<Integer> HERBS = ids(199, 201, 203, 205, 207, 209, 211, 213, 215, 217, 219, 249, 251, 253, 255,
		257, 259, 261, 263, 265, 267, 269, 2481, 2485, 2998, 3000, 3049, 3051, 30094, 30097);
	static final Set<Integer> GEMS = ids(1601, 1603, 1605, 1607, 1609, 1611, 1613, 1615, 1617, 1619, 1621, 1623,
		1625, 1627, 1629, 1631, 6571, 6573, 19493, 19496);
	/** Bones the Ectofuntus takes: not burnt, ground or blessed. */
	static final Set<Integer> BONES = ids(526, 530, 532, 534, 536, 2859, 3123, 3125, 3179, 3180, 3181, 3182, 3183,
		3185, 3186, 4812, 4830, 4832, 4834, 6729, 6812, 11943, 22124, 22780, 22783, 22786, 28899, 30898, 31726,
		31729, 33115);

	/** Achievement diary rewards (any tier), each at the banks of its region. */
	static final List<Outfit> DIARY = Collections.unmodifiableList(Arrays.asList(
		new Outfit("ardougne-north", "ardougne-south").piece(13121, 13122, 13123, 13124),
		new Outfit("varrock-east", "varrock-west", "ge").piece(13104, 13105, 13106, 13107),
		new Outfit("falador-east", "falador-west").piece(13117, 13118, 13119, 13120),
		new Outfit("shilo").piece(11136, 11138, 11140, 13103),
		new Outfit("canifis", "port-phasmatys").piece(13112, 13113, 13114, 13115),
		new Outfit("nardah", "sophanem").piece(13133, 13134, 13135, 13136),
		new Outfit("neitiznot", "jatizso").piece(13129, 13130, 13131, 13132),
		new Outfit("seers", "catherby").piece(13137, 13138, 13139, 13140),
		new Outfit("lumbridge").piece(13125, 13126, 13127, 13128),
		new Outfit("piscatoris").piece(13141, 13142, 13143, 13144),
		new Outfit("ferox", "edgeville").piece(13108, 13109, 13110, 13111),
		new Outfit("hosidius", "hosidius-kitchen", "kourend-castle", "arceuus", "lovakengj", "lovakengj-mine",
			"port-piscarilius", "shayzien", "shayzien-encampment", "blast-mine", "vinery", "sulphur-mine",
			"charcoal-camp", "woodcutting-guild", "farming-guild", "farming-guild-upper", "wintertodt",
			"mount-karuulm", "quidamortem", "chambers-of-xeric", "lands-end")
			.piece(22941, 22943, 22945, 22947)
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

	/** Every max cape you can wear, trouver-locked ones too, but not the hoods. */
	static final Set<Integer> MAX_CAPES = ids(13280, 13342, 13329, 13331, 13333, 13335, 13337, 20760, 21285, 21776,
		21780, 21784, 21898, 24133, 24134, 24135, 24232, 24233, 24234, 24855, 27363, 27365, 28902, 28906, 34625);
	static final Set<Integer> ABYSSAL_WHIPS = ids(4151, 12773, 12774, 20405, 26482, 26484);
	static final Set<Integer> TWISTED_BOWS = ids(20997, 28540);
	static final Set<Integer> RUBBER_CHICKENS = ids(4566);
	static final Set<Integer> GNOME_CHILD_HATS = ids(13655, 25321);

	/** Each Barrows brother's set: head, weapon, body and legs, in every charge and ornament form. */
	static final List<Outfit> BARROWS = Collections.unmodifiableList(Arrays.asList(
		new Outfit("canifis", "ferox")
			.piece(4708, 4856, 4857, 4858, 4859, 30445, 30519, 30525, 30531, 30537)
			.piece(4710, 4862, 4863, 4864, 4865, 23653, 30568, 30570, 30571, 30572, 30573)
			.piece(4712, 4868, 4869, 4870, 4871, 20598, 30447, 30521, 30527, 30533, 30539)
			.piece(4714, 4874, 4875, 4876, 4877, 20599, 30449, 30523, 30529, 30535, 30541),
		new Outfit("canifis", "ferox")
			.piece(4716, 4880, 4881, 4882, 4883, 23639)
			.piece(4718, 4886, 4887, 4888, 4889, 25516)
			.piece(4720, 4892, 4893, 4894, 4895, 25515)
			.piece(4722, 4898, 4899, 4900, 4901, 23633),
		new Outfit("canifis", "ferox")
			.piece(4724, 4904, 4905, 4906, 4907, 23638)
			.piece(4726, 4910, 4911, 4912, 4913)
			.piece(4728, 4916, 4917, 4918, 4919)
			.piece(4730, 4922, 4923, 4924, 4925),
		new Outfit("canifis", "ferox")
			.piece(4732, 4928, 4929, 4930, 4931)
			.piece(4734, 4934, 4935, 4936, 4937)
			.piece(4736, 4940, 4941, 4942, 4943, 23632)
			.piece(4738, 4946, 4947, 4948, 4949, 34609),
		new Outfit("canifis", "ferox")
			.piece(4745, 4952, 4953, 4954, 4955, 23637)
			.piece(4747, 4958, 4959, 4960, 4961)
			.piece(4749, 4964, 4965, 4966, 4967)
			.piece(4751, 4970, 4971, 4972, 4973, 23634),
		new Outfit("canifis", "ferox")
			.piece(4753, 4976, 4977, 4978, 4979, 23636)
			.piece(4755, 4982, 4983, 4984, 4985, 27189)
			.piece(4757, 4988, 4989, 4990, 4991, 27190)
			.piece(4759, 4994, 4995, 4996, 4997, 23635)
	));

	static final Outfit DHAROK = BARROWS.get(1);

	/** Skilling pets by follower name (any colour), keyed by the bank that is home to that skill. */
	static final Map<String, String> SKILLING_PETS = new HashMap<>();

	static
	{
		SKILLING_PETS.put("fishing-guild", "heron");
		SKILLING_PETS.put("woodcutting-guild", "beaver");
		SKILLING_PETS.put("mining-guild", "rock golem");
		SKILLING_PETS.put("farming-guild", "tangleroot");
		SKILLING_PETS.put("wintertodt", "phoenix");
		SKILLING_PETS.put("guardians-of-the-rift", "rift guardian");
		SKILLING_PETS.put("rogues-den", "rocky");
	}

	private Items()
	{
	}

	static Set<Integer> ids(Integer... ids)
	{
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ids)));
	}
}
