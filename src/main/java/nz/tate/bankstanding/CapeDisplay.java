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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ColorTextureOverride;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import nz.tate.bankstanding.core.CapePalette;

/**
 * Shows the 99 Bank Standing cape on your own character, on your own client only.
 *
 * <p>It swaps the cape slot of the local player's appearance to a max cape and recolours
 * it through the appearance's colour overrides. The game rebuilds the appearance whenever
 * equipment changes, so this is re-applied each tick; nothing is sent to the server.
 */
@Slf4j
@Singleton
class CapeDisplay
{
	static final int MAX_CAPE = 13280;

	private final Client client;
	private short[] colours;
	private boolean warned;
	private boolean applied;

	@Inject
	CapeDisplay(Client client)
	{
		this.client = client;
	}

	void apply()
	{
		Player player = client.getLocalPlayer();
		PlayerComposition appearance = player == null ? null : player.getPlayerComposition();
		if (appearance == null)
		{
			return;
		}

		int[] equipment = appearance.getEquipmentIds();
		int slot = KitType.CAPE.getIndex();
		int wanted = MAX_CAPE + PlayerComposition.ITEM_OFFSET;
		boolean changed = equipment[slot] != wanted;
		equipment[slot] = wanted;

		short[] palette = palette();
		if (palette != null)
		{
			ColorTextureOverride current = appearance.getColorTextureOverride(KitType.CAPE);
			if (current == null || !Arrays.equals(current.getColorToReplaceWith(), palette))
			{
				ColorTextureOverride override = appearance.createColorTextureOverride(KitType.CAPE, palette.length);
				System.arraycopy(palette, 0, override.getColorToReplaceWith(), 0, palette.length);
				changed = true;
			}
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
		appearance.getEquipmentIds()[KitType.CAPE.getIndex()] =
			wornCapeId > 0 ? wornCapeId + PlayerComposition.ITEM_OFFSET : 0;
		appearance.removeColorTextureOverride(KitType.CAPE);
		appearance.setHash();
	}

	/** Forget everything without touching the player, e.g. after logging out. */
	void forget()
	{
		applied = false;
	}

	/** The recoloured max cape colours, or null if the cape has no recolour slots. */
	private short[] palette()
	{
		if (colours != null)
		{
			return colours;
		}
		ItemComposition cape = client.getItemDefinition(MAX_CAPE);
		short[] from = cape.getColorToReplaceWith();
		if (from == null || from.length == 0)
		{
			if (!warned)
			{
				log.debug("Max cape has no recolour slots; showing it unrecoloured");
				warned = true;
			}
			return null;
		}
		colours = new short[from.length];
		for (int i = 0; i < from.length; i++)
		{
			colours[i] = CapePalette.recolour(from[i]);
		}
		return colours;
	}
}
