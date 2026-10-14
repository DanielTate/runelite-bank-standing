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
package nz.tate.bankstanding;

import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ColorTextureOverride;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import nz.tate.bankstanding.core.CapePalette;

/**
 * Shows the 99 Bank Standing cape on your own character, on your own client only.
 *
 * <p>It puts a skillcape in the cape slot of your appearance and turns it gold with a colour
 * override on your appearance alone. Item definitions are never changed, so other players'
 * capes, including the same skillcape, look exactly as normal. The game rebuilds your
 * appearance whenever equipment changes, so this is re-applied each tick; nothing is sent to
 * the server.
 */
@Singleton
class CapeDisplay
{
	/** Skillcapes the cape can be built from; they all have recolour slots. */
	static final int[] BASES = {
		ItemID.SKILLCAPE_QP, ItemID.SKILLCAPE_ATTACK, ItemID.SKILLCAPE_STRENGTH, ItemID.SKILLCAPE_DEFENCE,
		ItemID.SKILLCAPE_RANGING, ItemID.SKILLCAPE_PRAYER, ItemID.SKILLCAPE_MAGIC, ItemID.SKILLCAPE_RUNECRAFTING,
		ItemID.SKILLCAPE_HITPOINTS, ItemID.SKILLCAPE_AGILITY, ItemID.SKILLCAPE_HERBLORE, ItemID.SKILLCAPE_THIEVING,
		ItemID.SKILLCAPE_CRAFTING, ItemID.SKILLCAPE_FLETCHING, ItemID.SKILLCAPE_SLAYER,
		ItemID.SKILLCAPE_CONSTRUCTION, ItemID.SKILLCAPE_MINING, ItemID.SKILLCAPE_SMITHING, ItemID.SKILLCAPE_FISHING,
		ItemID.SKILLCAPE_COOKING, ItemID.SKILLCAPE_FIREMAKING, ItemID.SKILLCAPE_WOODCUTTING,
		ItemID.SKILLCAPE_FARMING, ItemID.SKILLCAPE_HUNTING, ItemID.SKILLCAPE_SAILING, ItemID.SKILLCAPE_AD,
	};
	private static final String[] NAMES = {
		"Quest point", "Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft", "Hitpoints",
		"Agility", "Herblore", "Thieving", "Crafting", "Fletching", "Slayer", "Construction", "Mining", "Smithing",
		"Fishing", "Cooking", "Firemaking", "Woodcutting", "Farming", "Hunter", "Sailing", "Achievement diary",
	};
	static final int DEFAULT_BASE = ItemID.SKILLCAPE_QP;

	/** The skillcape's name, for the dev panel. */
	static String name(int base)
	{
		for (int i = 0; i < BASES.length; i++)
		{
			if (BASES[i] == base)
			{
				return NAMES[i];
			}
		}
		return String.valueOf(base);
	}

	private final Client client;
	private final DevOptions dev;
	private boolean applied;

	@Inject
	CapeDisplay(Client client, DevOptions dev)
	{
		this.client = client;
		this.dev = dev;
	}

	/** The skillcape in use: the default, or another chosen in the dev panel. */
	private int base()
	{
		return dev.capeBase > 0 ? dev.capeBase : DEFAULT_BASE;
	}

	void apply()
	{
		Player player = client.getLocalPlayer();
		PlayerComposition appearance = player == null ? null : player.getPlayerComposition();
		if (appearance == null)
		{
			return;
		}
		int base = base();
		ItemComposition cape = client.getItemDefinition(base);
		short[] original = cape.getColorToReplaceWith();
		if (original == null || original.length == 0)
		{
			return;
		}
		short[] gold = gold(original);

		int[] equipment = appearance.getEquipmentIds();
		int slot = KitType.CAPE.getIndex();
		int wanted = base + PlayerComposition.ITEM_OFFSET;
		boolean changed = equipment[slot] != wanted;
		equipment[slot] = wanted;

		ColorTextureOverride override = appearance.getColorTextureOverride(KitType.CAPE);
		if (changed || override == null || !Arrays.equals(override.getColorToReplaceWith(), gold))
		{
			override = appearance.createColorTextureOverride(KitType.CAPE, base);
			short[] colours = override.getColorToReplaceWith();
			System.arraycopy(gold, 0, colours, 0, Math.min(gold.length, colours.length));
			changed = true;
		}
		if (changed)
		{
			appearance.setHash();
		}
		applied = true;
	}

	/** Puts back the cape actually worn, or none. {@code wornCapeId} is -1 for an empty slot. */
	void restore(int wornCapeId)
	{
		if (!applied)
		{
			return;
		}
		applied = false;
		Player player = client.getLocalPlayer();
		PlayerComposition appearance = player == null ? null : player.getPlayerComposition();
		if (appearance == null)
		{
			return;
		}
		appearance.removeColorTextureOverride(KitType.CAPE);
		appearance.getEquipmentIds()[KitType.CAPE.getIndex()] =
			wornCapeId > 0 ? wornCapeId + PlayerComposition.ITEM_OFFSET : 0;
		appearance.setHash();
	}

	/** Forget everything without touching the player, e.g. after logging out. */
	void forget()
	{
		applied = false;
	}

	private static short[] gold(short[] colours)
	{
		short[] out = new short[colours.length];
		for (int i = 0; i < colours.length; i++)
		{
			out[i] = CapePalette.recolour(colours[i]);
		}
		return out;
	}
}
