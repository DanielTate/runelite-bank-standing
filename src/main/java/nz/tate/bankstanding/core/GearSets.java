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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Collection log sets: every tier of a tool, each collected by standing at its guild's bank
 * with it worn or carried, and whole outfits worn at any bank. Purely for the log; they never
 * change XP.
 */
public final class GearSets
{
	/** One tier, like "Rune axe": any of its item IDs counts, and the first is its icon. */
	public static final class Tier
	{
		public final String key;
		public final String name;
		public final int icon;
		final Set<Integer> ids;
		/** For an outfit: one of each piece must be worn, and carrying it doesn't count. Else null. */
		final List<Set<Integer>> pieces;

		Tier(String key, String name, Integer... ids)
		{
			this.key = key;
			this.name = name;
			this.ids = Items.ids(ids);
			this.icon = ids[0];
			this.pieces = null;
		}

		private Tier(String key, String name, List<Set<Integer>> pieces)
		{
			this.key = key;
			this.name = name;
			this.ids = Collections.emptySet();
			this.icon = pieces.get(0).iterator().next();
			this.pieces = Collections.unmodifiableList(pieces);
		}

		/** A whole outfit; each array is one piece, any item of which counts. The first item is its icon. */
		static Tier outfit(String key, String name, Integer[]... pieces)
		{
			List<Set<Integer>> sets = new ArrayList<>();
			for (Integer[] piece : pieces)
			{
				sets.add(Items.ids(piece));
			}
			Tier tier = new Tier(key, name, sets);
			return tier;
		}

		public boolean outfit()
		{
			return pieces != null;
		}
	}

	/** A set collected at its banks. */
	public static final class GearSet
	{
		public final String key;
		public final String title;
		/** The banks it's collected at, the first naming it in the log; empty for any bank. */
		public final List<String> banks;
		public final List<Tier> tiers;

		GearSet(String key, String title, String bank, Tier... tiers)
		{
			this(key, title, Collections.singletonList(bank), tiers);
		}

		GearSet(String key, String title, List<String> banks, Tier... tiers)
		{
			this.key = key;
			this.title = title;
			this.banks = Collections.unmodifiableList(banks);
			this.tiers = Collections.unmodifiableList(Arrays.asList(tiers));
		}

		/** The key saved in {@link Progress#moments} once the tier is collected. */
		public String momentKey(Tier tier)
		{
			return "gear:" + key + ":" + tier.key;
		}
	}

	public static final List<GearSet> SETS = Collections.unmodifiableList(Arrays.asList(
		new GearSet("axes", "Axes", "woodcutting-guild",
			new Tier("bronze", "Bronze axe", 1351, 28196),
			new Tier("iron", "Iron axe", 1349, 28199),
			new Tier("steel", "Steel axe", 1353, 28202),
			new Tier("black", "Black axe", 1361, 28205),
			new Tier("mithril", "Mithril axe", 1355, 28208),
			new Tier("adamant", "Adamant axe", 1357, 28211),
			new Tier("rune", "Rune axe", 1359, 28214),
			new Tier("gilded", "Gilded axe", 23279),
			new Tier("dragon", "Dragon axe", 6739, 25378, 30352, 28217),
			new Tier("infernal", "Infernal axe", 13241, 13242, 25066, 25371, 30347, 30348),
			new Tier("crystal", "Crystal axe", 23673, 23675, 28220, 28223),
			new Tier("3rd-age", "3rd age axe", 20011, 28226)),
		new GearSet("pickaxes", "Pickaxes", "mining-guild",
			new Tier("bronze", "Bronze pickaxe", 1265),
			new Tier("iron", "Iron pickaxe", 1267, 11721),
			new Tier("steel", "Steel pickaxe", 1269),
			new Tier("black", "Black pickaxe", 12297),
			new Tier("mithril", "Mithril pickaxe", 1273, 11720),
			new Tier("adamant", "Adamant pickaxe", 1271),
			new Tier("rune", "Rune pickaxe", 1275, 11719),
			new Tier("gilded", "Gilded pickaxe", 23276),
			new Tier("dragon", "Dragon pickaxe", 11920, 12797, 23677, 25376, 30351),
			new Tier("infernal", "Infernal pickaxe", 13243, 13244, 25063, 25369, 30345, 30346),
			new Tier("crystal", "Crystal pickaxe", 23680, 23682),
			new Tier("3rd-age", "3rd age pickaxe", 20014)),
		new GearSet("harpoons", "Harpoons", "fishing-guild",
			new Tier("harpoon", "Harpoon", 311),
			new Tier("barb-tail", "Barb-tail harpoon", 10129),
			new Tier("dragon", "Dragon harpoon", 21028, 25373, 30349),
			new Tier("infernal", "Infernal harpoon", 21031, 21033, 25059, 25367, 30342, 30343),
			new Tier("crystal", "Crystal harpoon", 23762, 23764)),
		new GearSet("defenders", "Defenders", "warriors-guild",
			new Tier("bronze", "Bronze defender", 8844, 24136),
			new Tier("iron", "Iron defender", 8845, 24137),
			new Tier("steel", "Steel defender", 8846, 24138),
			new Tier("black", "Black defender", 8847, 24139),
			new Tier("mithril", "Mithril defender", 8848, 24140),
			new Tier("adamant", "Adamant defender", 8849, 24141),
			new Tier("rune", "Rune defender", 8850, 23230, 24142, 27009),
			new Tier("dragon", "Dragon defender", 12954, 19722, 24143, 27008),
			new Tier("avernic", "Avernic defender", 22322, 24186)),
		new GearSet("godswords", "Godswords", "nex",
			new Tier("armadyl", "Armadyl godsword", 11802, 20368),
			new Tier("bandos", "Bandos godsword", 11804, 20370),
			new Tier("saradomin", "Saradomin godsword", 11806, 20372),
			new Tier("zamorak", "Zamorak godsword", 11808, 20374),
			new Tier("ancient", "Ancient godsword", 26233)),
		new GearSet("partyhats", "Partyhats", Arrays.asList("falador-east", "falador-west"),
			new Tier("red", "Red partyhat", 1038),
			new Tier("yellow", "Yellow partyhat", 1040),
			new Tier("green", "Green partyhat", 1044),
			new Tier("blue", "Blue partyhat", 1042),
			new Tier("purple", "Purple partyhat", 1046),
			new Tier("white", "White partyhat", 1048)),
		new GearSet("slayer-helmets", "Slayer helmets", "mount-karuulm",
			new Tier("slayer", "Slayer helmet", 11864, 11865, 25177, 26674),
			new Tier("black", "Black slayer helmet", 19639, 19641, 25179, 26675),
			new Tier("green", "Green slayer helmet", 19643, 19645, 25181, 26676),
			new Tier("red", "Red slayer helmet", 19647, 19649, 25183, 26677),
			new Tier("purple", "Purple slayer helmet", 21264, 21266, 25185, 26678),
			new Tier("turquoise", "Turquoise slayer helmet", 21888, 21890, 25187, 26679),
			new Tier("hydra", "Hydra slayer helmet", 23073, 23075, 25189, 26680),
			new Tier("twisted", "Twisted slayer helmet", 24370, 24444, 25191, 26681),
			new Tier("tztok", "Tztok slayer helmet", 25898, 25900, 25902, 26682),
			new Tier("vampyric", "Vampyric slayer helmet", 25904, 25906, 25908, 26683),
			new Tier("tzkal", "Tzkal slayer helmet", 25910, 25912, 25914, 26684),
			new Tier("araxyte", "Araxyte slayer helmet", 29816, 29818, 29820, 29822),
			new Tier("hooded", "Hooded slayer helmet", 33066, 33068, 33070, 33072),
			new Tier("oathplate", "Oathplate slayer helmet", 33338, 33439, 33441, 33443),
			new Tier("radiant", "Radiant slayer helmet", 33340, 33445, 33447, 33449)),
		new GearSet("wilderness-weapons", "Wilderness weapons", "ferox",
			new Tier("craws-bow", "Craw's bow", 22550, 22547),
			new Tier("viggoras-chainmace", "Viggora's chainmace", 22545, 22542),
			new Tier("thammarons-sceptre", "Thammaron's sceptre", 22555, 22552, 27785, 27788),
			new Tier("webweaver-bow", "Webweaver bow", 27655, 27652),
			new Tier("ursine-chainmace", "Ursine chainmace", 27660, 27657),
			new Tier("accursed-sceptre", "Accursed sceptre", 27665, 27662, 27676, 27679)),
		new GearSet("essence-pouches", "Essence pouches", Arrays.asList("ourania", "guardians-of-the-rift"),
			new Tier("small", "Small pouch", 5509),
			new Tier("medium", "Medium pouch", 5510, 5511),
			new Tier("large", "Large pouch", 5512, 5513),
			new Tier("giant", "Giant pouch", 5514, 5515),
			new Tier("colossal", "Colossal pouch", 26784, 26786, 26906)),
		new GearSet("god-books", "God books", "barbarian-outpost",
			new Tier("holy", "Holy book", 3840, 26496),
			new Tier("unholy", "Unholy book", 3842, 26498),
			new Tier("balance", "Book of balance", 3844, 26488),
			new Tier("war", "Book of war", 12608, 26494),
			new Tier("law", "Book of law", 12610, 26492),
			new Tier("darkness", "Book of darkness", 12612, 26490)),
		new GearSet("battlestaves", "Battlestaves", "varrock-west",
			new Tier("air", "Air battlestaff", 1397),
			new Tier("water", "Water battlestaff", 1395),
			new Tier("earth", "Earth battlestaff", 1399),
			new Tier("fire", "Fire battlestaff", 1393),
			new Tier("lava", "Lava battlestaff", 3053),
			new Tier("mud", "Mud battlestaff", 6562),
			new Tier("steam", "Steam battlestaff", 11787),
			new Tier("smoke", "Smoke battlestaff", 11998),
			new Tier("mist", "Mist battlestaff", 20730),
			new Tier("dust", "Dust battlestaff", 20736)),
		new GearSet("fishing-tools", "Fishing tools", "catherby",
			new Tier("small-net", "Small fishing net", 303),
			new Tier("big-net", "Big fishing net", 305),
			new Tier("rod", "Fishing rod", 307, 22846),
			new Tier("fly-rod", "Fly fishing rod", 309, 22844),
			new Tier("lobster-pot", "Lobster pot", 301),
			new Tier("oily-rod", "Oily fishing rod", 1585),
			new Tier("barbarian-rod", "Barbarian rod", 11323, 22842),
			new Tier("karambwan-vessel", "Karambwan vessel", 3157, 3159)),
		new GearSet("hween-masks", "H'ween masks", "draynor",
			new Tier("green", "Green h'ween mask", 1053),
			new Tier("blue", "Blue h'ween mask", 1055),
			new Tier("red", "Red h'ween mask", 1057)),
		new GearSet("random-events", "Random event outfits", Collections.emptyList(),
			Tier.outfit("mime", "Mime outfit", p(3057), p(3058), p(3059), p(3060), p(3061)),
			Tier.outfit("frog", "Frog mask", p(6188)),
			Tier.outfit("camo", "Camo outfit", p(6656), p(6654), p(6655)),
			Tier.outfit("zombie", "Zombie outfit", p(7594), p(7592), p(7593), p(7595), p(7596)),
			Tier.outfit("lederhosen", "Lederhosen outfit", p(6182), p(6180), p(6181)),
			Tier.outfit("royal", "Prince or princess outfit", p(6184, 6186), p(6185, 6187)))
	));

	/** One piece of an outfit: any of these items. */
	private static Integer[] p(Integer... ids)
	{
		return ids;
	}

	private GearSets()
	{
	}

	/** Collects every tier worn or carried at its set's bank; returns the ones that are new. */
	public static List<Tier> collect(Progress p, String bankKey, Set<Integer> worn, int[] inventory, long now)
	{
		List<Tier> found = new ArrayList<>();
		for (GearSet set : SETS)
		{
			if (!set.banks.isEmpty() && !set.banks.contains(bankKey))
			{
				continue;
			}
			for (Tier tier : set.tiers)
			{
				if (holds(tier, worn, inventory) && Moments.record(p, set.momentKey(tier), now))
				{
					found.add(tier);
				}
			}
		}
		return found;
	}

	private static boolean holds(Tier tier, Set<Integer> worn, int[] inventory)
	{
		if (tier.outfit())
		{
			for (Set<Integer> piece : tier.pieces)
			{
				if (piece.stream().noneMatch(worn::contains))
				{
					return false;
				}
			}
			return true;
		}
		for (int id : tier.ids)
		{
			if (worn.contains(id))
			{
				return true;
			}
		}
		for (int id : inventory)
		{
			if (tier.ids.contains(id))
			{
				return true;
			}
		}
		return false;
	}
}
