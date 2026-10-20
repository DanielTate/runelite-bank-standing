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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The named banks grouped by region, for the collection log's Banks tab. Tutorial Island is
 * left out on purpose: it's a secret place instead.
 */
public final class BankRegions
{
	private static final Map<String, List<String>> REGIONS = new LinkedHashMap<>();

	static
	{
		region("Misthalin", "varrock-west", "varrock-east", "ge", "edgeville", "edgeville-pvp", "draynor",
			"lumbridge", "lumbridge-pvp", "cooking-guild",
			"fractured-archive-west", "fractured-archive-east");
		region("Asgarnia", "falador-west", "falador-east", "falador-pvp", "port-sarim", "crafting-guild",
			"warriors-guild", "mining-guild", "rogues-den", "motherlode", "camdozaal", "nex");
		region("Kandarin", "seers", "catherby", "camelot-pvp", "ardougne-north", "ardougne-south",
			"fishing-guild", "legends-guild", "yanille", "port-khazard", "castle-wars", "gnome-stronghold-north",
			"gnome-stronghold-south", "barbarian-outpost", "piscatoris", "myths-guild", "corsair-cove", "ourania");
		region("Fremennik Province", "neitiznot", "jatizso", "etceteria", "lunar-isle", "keldagrim",
			"blast-furnace");
		region("Wilderness", "ferox", "mage-arena", "daimons-crater");
		region("Morytania", "canifis", "port-phasmatys", "burgh-de-rott", "darkmeyer", "ver-sinhaza",
			"tarns-lair", "hallowed-sepulchre", "guardians-of-the-rift");
		region("Kharidian Desert", "al-kharid", "shantay-pass", "sophanem", "nardah", "unkah",
			"tombs-of-amascut", "emirs-arena", "mage-training-arena", "giants-foundry", "culinaromancers-chest");
		region("Karamja", "shilo", "tzhaar", "mor-ul-rek", "pandemonium");
		region("Tirannwn", "lletya", "prifddinas-north", "prifddinas-south", "gauntlet", "gwenith", "zul-andra");
		region("Great Kourend", "farming-guild", "farming-guild-upper", "quidamortem", "chambers-of-xeric",
			"mount-karuulm", "lovakengj", "lovakengj-mine", "sulphur-mine", "blast-mine", "shayzien",
			"shayzien-encampment", "lands-end", "woodcutting-guild", "ent-dungeon", "kourend-castle", "arceuus",
			"wintertodt", "hosidius", "hosidius-kitchen", "saltpetre", "charcoal-camp", "port-piscarilius",
			"vinery", "port-roberts");
		region("Varlamore", "tal-teklan", "mistrock", "nemus-retreat", "aldarin", "aldarin-dock",
			"alchemical-society", "auburnvale", "cam-torum", "quetzacalli-gorge", "darkfrost", "hunters-guild",
			"fortis", "fortis-west", "fortis-cothon", "colosseum");
		region("Islands and the sea", "isle-of-souls", "bank-boat", "sunbleak-island", "buccaneers-haven",
			"deepfin-point", "deepfin-mine-west", "deepfin-mine-east", "wyrmscraig", "charred-island",
			"red-rock", "onyx-crest", "great-conch", "great-conch-dock", "the-node", "ape-atoll",
			"mos-le-harmless", "trouble-brewing", "fossil-island", "fossil-island-wreck", "volcanic-mine",
			"pest-control");
		region("Other realms", "zanaris", "clan-hall", "dorgesh-kaan", "sangvesti");
	}

	private BankRegions()
	{
	}

	private static void region(String name, String... keys)
	{
		REGIONS.put(name, Collections.unmodifiableList(Arrays.asList(keys)));
	}

	public static List<String> names()
	{
		return new ArrayList<>(REGIONS.keySet());
	}

	/** The region a bank is listed under, or null if it isn't listed. */
	public static String regionOf(String key)
	{
		for (Map.Entry<String, List<String>> region : REGIONS.entrySet())
		{
			if (region.getValue().contains(key))
			{
				return region.getKey();
			}
		}
		return null;
	}

	/** How many banks the log lists. */
	public static int total()
	{
		int total = 0;
		for (List<String> keys : REGIONS.values())
		{
			total += keys.size();
		}
		return total;
	}

	/** The bank keys in a region, in the order listed. */
	public static List<String> keys(String region)
	{
		return REGIONS.getOrDefault(region, Collections.emptyList());
	}
}
