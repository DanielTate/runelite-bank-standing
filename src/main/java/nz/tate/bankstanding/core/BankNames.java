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
import java.util.List;

/**
 * Friendly names for known banks, matched by where the player stands.
 *
 * <p>Centres are approximate and need checking in game (see the coordinates an unknown bank
 * shows in the panel). A bank not listed here still counts; it is tracked under its map
 * region and shown as "Unknown bank (x, y)".
 */
public final class BankNames
{
	private static final int RADIUS = 12;

	private static final List<BankArea> AREAS = Collections.unmodifiableList(Arrays.asList(
		area("ge", "Grand Exchange", 3164, 3487),
		area("varrock-west", "Varrock West", 3185, 3438),
		area("varrock-east", "Varrock East", 3253, 3420),
		area("edgeville", "Edgeville", 3094, 3493),
		area("draynor", "Draynor Village", 3092, 3243),
		area("falador-east", "Falador East", 3013, 3355),
		area("falador-west", "Falador West", 2946, 3368),
		area("lumbridge", "Lumbridge Castle", 3208, 3220),
		area("al-kharid", "Al Kharid", 3269, 3167),
		area("catherby", "Catherby", 2809, 3441),
		area("seers", "Seers' Village", 2725, 3492),
		area("ardougne-north", "Ardougne North", 2615, 3332),
		area("ardougne-south", "Ardougne South", 2655, 3283),
		area("yanille", "Yanille", 2613, 3093),
		area("canifis", "Canifis", 3512, 3480),
		area("port-phasmatys", "Port Phasmatys", 3688, 3467),
		area("ferox", "Ferox Enclave", 3130, 3631),
		area("castle-wars", "Castle Wars", 2443, 3083),
		area("shilo", "Shilo Village", 2852, 2954),
		area("lunar-isle", "Lunar Isle", 2099, 3919),
		area("zanaris", "Zanaris", 2383, 4458),
		area("nardah", "Nardah", 3428, 2892),
		area("fishing-guild", "Fishing Guild", 2586, 3420),
		area("cooking-guild", "Cooking Guild", 3143, 3443),
		area("crafting-guild", "Crafting Guild", 2936, 3281),
		area("warriors-guild", "Warriors' Guild", 2843, 3543),
		area("woodcutting-guild", "Woodcutting Guild", 1592, 3475),
		area("farming-guild", "Farming Guild", 1253, 3741),
		area("wintertodt", "Wintertodt Camp", 1640, 3944),
		area("rogues-den", "Rogues' Den", 3043, 4972),
		area("motherlode", "Motherlode Mine", 3760, 5666),
		area("barbarian-outpost", "Barbarian Outpost", 2536, 3574),
		area("pest-control", "Void Knights' Outpost", 2667, 2653),
		area("piscatoris", "Piscatoris", 2330, 3689),
		area("ver-sinhaza", "Ver Sinhaza", 3650, 3208),
		area("mor-ul-rek", "Mor Ul Rek", 2540, 5140),
		area("shayzien", "Shayzien", 1504, 3615),
		area("quidamortem", "Mount Quidamortem", 1255, 3570),
		area("fortis", "Civitas illa Fortis", 1780, 3100),
		area("hunters-guild", "Hunters' Guild", 1558, 3047),
		area("nex", "Ancient Prison (Nex)", 2904, 5204)
	));

	private BankNames()
	{
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
		return "Unknown bank (" + record.x + ", " + record.y + ")";
	}
}
