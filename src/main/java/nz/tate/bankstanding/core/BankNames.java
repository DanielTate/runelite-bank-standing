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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Friendly names for known banks, matched by where the player stands.
 *
 * <p>Centres come from the OSRS Wiki's List of banks (checked 2026-10-14). A bank not listed
 * here still counts: it is tracked under its map region and named after the closest listed
 * bank ("Bank near Catherby").
 */
public final class BankNames
{
	private static final int RADIUS = 12;

	private static final List<BankArea> AREAS = Collections.unmodifiableList(Arrays.asList(
		area("varrock-west", "Varrock West", 3185, 3441),
		area("falador-west", "Falador West", 2946, 3367),
		area("edgeville", "Edgeville", 3095, 3494),
		area("draynor", "Draynor Village", 3093, 3244),
		area("varrock-east", "Varrock East", 3254, 3421),
		area("falador-east", "Falador East", 3015, 3357),
		area("al-kharid", "Al Kharid", 3269, 3167),
		area("catherby", "Catherby", 2810, 3442),
		area("zanaris", "Zanaris", 2383, 4459),
		area("seers", "Seers' Village", 2725, 3492),
		area("ardougne-south", "Ardougne South", 2653, 3284),
		area("ardougne-north", "Ardougne North", 2617, 3333),
		area("tutorial-island", "Tutorial Island", 3122, 3124),
		area("yanille", "Yanille", 2613, 3093),
		area("gnome-stronghold-south", "Gnome Stronghold South", 2446, 3424),
		area("gnome-stronghold-north", "Gnome Stronghold North", 2444, 3485),
		area("shilo", "Shilo Village", 2852, 2955),
		area("shantay-pass", "Shantay Pass", 3308, 3119),
		area("legends-guild", "Legends' Guild", 2733, 3379),
		area("mage-arena", "Mage Arena", 2534, 4714),
		area("fishing-guild", "Fishing Guild", 2588, 3416),
		area("emirs-arena", "Emir's Arena", 3384, 3270),
		area("canifis", "Canifis", 3512, 3478),
		area("etceteria", "Etceteria", 2620, 3895),
		area("castle-wars", "Castle Wars", 2445, 3082),
		area("port-phasmatys", "Port Phasmatys", 3692, 3468),
		area("keldagrim", "Keldagrim", 2838, 10210),
		area("rogues-den", "Rogues' Den", 3042, 4972),
		area("lletya", "Lletya", 2352, 3162),
		area("tzhaar", "TzHaar City", 2446, 5179),
		area("nardah", "Nardah", 3427, 2891),
		area("mos-le-harmless", "Mos Le'Harmless", 3681, 2982),
		area("culinaromancers-chest", "Culinaromancer's Chest", 3219, 9623),
		area("burgh-de-rott", "Burgh de Rott", 3496, 3211),
		area("pest-control", "Void Knights' Outpost", 2667, 2653),
		area("piscatoris", "Piscatoris", 2330, 3690),
		area("warriors-guild", "Warriors' Guild", 2843, 3543),
		area("lunar-isle", "Lunar Isle", 2101, 3919),
		area("lumbridge", "Lumbridge Castle", 3209, 3219),
		area("sophanem", "Sophanem", 3312, 2799),
		area("tarns-lair", "Tarn's Lair", 3195, 4570),
		area("neitiznot", "Neitiznot", 2337, 3807),
		area("jatizso", "Jatizso", 2417, 3801),
		area("dorgesh-kaan", "Dorgesh-Kaan", 2702, 5350),
		area("lumbridge-pvp", "Lumbridge PvP Chest", 3221, 3219),
		area("falador-pvp", "Falador PvP Chest", 2970, 3342),
		area("edgeville-pvp", "Edgeville PvP Chest", 3094, 3469),
		area("camelot-pvp", "Camelot PvP Chest", 2757, 3478),
		area("blast-furnace", "Blast Furnace", 2931, 10195),
		area("motherlode", "Motherlode Mine", 3759, 5666),
		area("ge", "Grand Exchange", 3165, 3490),
		area("crafting-guild", "Crafting Guild", 2936, 3280),
		area("barbarian-outpost", "Barbarian Outpost", 2537, 3574),
		area("port-khazard", "Port Khazard", 2662, 3161),
		area("cooking-guild", "Cooks' Guild", 3148, 3448),
		area("arceuus", "Arceuus", 1630, 3745),
		area("blast-mine", "Blast Mine", 1489, 3864),
		area("charcoal-camp", "Charcoal Camp", 1717, 3464),
		area("hosidius", "Hosidius", 1748, 3599),
		area("hosidius-kitchen", "Hosidius Kitchen", 1676, 3616),
		area("kourend-castle", "Kourend Castle", 1612, 3682),
		area("lovakengj", "Lovakengj", 1526, 3739),
		area("lovakengj-mine", "Lovakengj Mine", 1438, 3829),
		area("port-piscarilius", "Port Piscarilius", 1803, 3788),
		area("shayzien", "Shayzien", 1487, 3593),
		area("sulphur-mine", "Sulphur Mine", 1455, 3860),
		area("vinery", "Vinery", 1809, 3567),
		area("ape-atoll", "Ape Atoll", 2781, 2784),
		area("woodcutting-guild", "Woodcutting Guild", 1591, 3478),
		area("ent-dungeon", "Woodcutting Guild Dungeon", 1549, 9875),
		area("wintertodt", "Wintertodt", 1640, 3944),
		area("ourania", "Ourania Altar", 3016, 5625),
		area("lands-end", "Land's End", 1513, 3421),
		area("quidamortem", "Mount Quidamortem", 1254, 3572),
		area("mor-ul-rek", "Mor Ul Rek", 2543, 5144),
		area("mining-guild", "Mining Guild", 3012, 9718),
		area("fossil-island", "Fossil Island", 3741, 3804),
		area("fossil-island-wreck", "Fossil Island Wreck", 3772, 3898),
		area("volcanic-mine", "Volcanic Mine", 3819, 3809),
		area("myths-guild", "Myths' Guild", 2463, 2848),
		area("corsair-cove", "Corsair Cove", 2570, 2865),
		area("ver-sinhaza", "Ver Sinhaza", 3651, 3206),
		area("farming-guild", "Farming Guild", 1253, 3742),
		area("farming-guild-upper", "Farming Guild (Advanced)", 1249, 3761),
		area("mount-karuulm", "Mount Karuulm", 1324, 3824),
		area("prifddinas-south", "Prifddinas South", 3297, 6060),
		area("prifddinas-north", "Prifddinas North", 3257, 6108),
		area("darkmeyer", "Darkmeyer", 3602, 3366),
		area("hallowed-sepulchre", "Hallowed Sepulchre", 2400, 5983),
		area("ferox", "Ferox Enclave", 3130, 3632),
		area("trouble-brewing", "Trouble Brewing", 3810, 3020),
		area("the-node", "The Node", 3096, 3027),
		area("unkah", "Ruins of Unkah", 3156, 2837),
		area("camdozaal", "Camdozaal", 2978, 5799),
		area("clan-hall", "Clan Hall", 1748, 5476),
		area("shayzien-encampment", "Shayzien Encampment", 1485, 3646),
		area("nex", "Ancient Prison (Nex)", 2904, 5205),
		area("guardians-of-the-rift", "Guardians of the Rift", 3619, 9473),
		area("giants-foundry", "Giants' Foundry", 3462, 9588),
		area("tombs-of-amascut", "Tombs of Amascut", 3354, 9120),
		area("chambers-of-xeric", "Chambers of Xeric", 3303, 5199),
		area("daimons-crater", "Daimon's Crater", 3426, 4064),
		area("cam-torum", "Cam Torum", 1456, 9568),
		area("hunters-guild", "Hunters' Guild", 1542, 3040),
		area("fortis", "Civitas illa Fortis", 1780, 3096),
		area("fortis-west", "Fortis West", 1648, 3118),
		area("colosseum", "Fortis Colosseum", 1805, 9501),
		area("mage-training-arena", "Mage Training Arena", 3367, 3318),
		area("alchemical-society", "Alchemical Society", 1399, 9313),
		area("aldarin", "Aldarin", 1399, 2927),
		area("mistrock", "Mistrock", 1382, 2866),
		area("quetzacalli-gorge", "Quetzacalli Gorge", 1520, 3229),
		area("darkfrost", "The Darkfrost", 1526, 3293),
		area("auburnvale", "Auburnvale", 1416, 3353),
		area("tal-teklan", "Tal Teklan", 1244, 3121),
		area("nemus-retreat", "Nemus Retreat", 1387, 3311),
		area("pandemonium", "The Pandemonium", 3039, 3000),
		area("deepfin-point", "Deepfin Point", 1936, 2754),
		area("great-conch", "The Great Conch", 3182, 2419),
		area("onyx-crest", "The Onyx Crest", 2985, 2268),
		area("charred-island", "Charred Island", 2655, 2387),
		area("port-roberts", "Port Roberts", 1883, 3316),
		area("sunbleak-island", "Sunbleak Island", 2194, 2314),
		area("bank-boat", "Bank Boat", 2280, 2544),
		area("buccaneers-haven", "Buccaneers' Haven", 2097, 3687),
		area("deepfin-mine-west", "Deepfin Mine West", 2011, 9185),
		area("deepfin-mine-east", "Deepfin Mine East", 2093, 9197),
		area("red-rock", "Red Rock", 2809, 2521),
		area("sangvesti", "Sangvesti", 2581, 7832),
		area("wyrmscraig", "Wyrmscraig", 2586, 2260),
		area("port-sarim", "Port Sarim", 3045, 3234),
		area("saltpetre", "Saltpetre Mine", 1702, 3527),
		area("gwenith", "Gwenith", 2202, 3409),
		area("isle-of-souls", "Isle of Souls", 2210, 2861),
		area("fortis-cothon", "Fortis Cothon", 1782, 3136),
		area("aldarin-dock", "Aldarin Dock", 1444, 2966),
		area("great-conch-dock", "Great Conch Dock", 3182, 2368),
		area("gauntlet", "The Gauntlet", 3038, 6129),
		area("zul-andra", "Zul-Andra", 2196, 3051),
		area("fractured-archive-west", "Fractured Archive West", 4121, 5552),
		area("fractured-archive-east", "Fractured Archive East", 4133, 5552)
	));

	/**
	 * Banks that take real effort to reach: quest-locked, or with no teleport close by.
	 * Standing at one gives a small hidden bonus.
	 */
	private static final Set<String> REMOTE = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		"lunar-isle", "zanaris", "shilo", "piscatoris", "mor-ul-rek", "nex", "colosseum", "lletya",
		"prifddinas-north", "prifddinas-south", "sophanem", "neitiznot", "jatizso", "burgh-de-rott",
		"darkmeyer", "myths-guild", "fossil-island", "tzhaar", "mage-arena", "keldagrim", "dorgesh-kaan",
		"mos-le-harmless", "ape-atoll", "tarns-lair", "etceteria", "corsair-cove", "hallowed-sepulchre",
		"fractured-archive-west", "fractured-archive-east"
	)));

	private BankNames()
	{
	}

	/** Every named bank. */
	public static List<BankArea> all()
	{
		return AREAS;
	}

	public static boolean isRemote(String key)
	{
		return REMOTE.contains(key);
	}

	private static BankArea area(String key, String name, int x, int y)
	{
		return new BankArea(key, name, x, y, RADIUS);
	}

	/** The closest named bank around this tile, or null if none is listed here. */
	public static BankArea find(int x, int y)
	{
		BankArea best = null;
		for (BankArea area : AREAS)
		{
			if (area.contains(x, y) && (best == null || area.distanceTo(x, y) < best.distanceTo(x, y)))
			{
				best = area;
			}
		}
		return best;
	}

	/** The listed bank closest to this tile, however far away. */
	static BankArea nearest(int x, int y)
	{
		BankArea best = AREAS.get(0);
		for (BankArea area : AREAS)
		{
			if (area.distanceTo(x, y) < best.distanceTo(x, y))
			{
				best = area;
			}
		}
		return best;
	}

	/** The save key for the bank at this tile: its listed key, or its map region. */
	public static String keyFor(int x, int y)
	{
		BankArea area = find(x, y);
		return area != null ? area.getKey() : "region-" + ((x >> 6) << 8 | (y >> 6));
	}

	public static String displayName(String key, BankRecord record)
	{
		for (BankArea area : AREAS)
		{
			if (area.getKey().equals(key))
			{
				return area.getName();
			}
		}
		return "Bank near " + nearest(record.x, record.y).getName();
	}
}
