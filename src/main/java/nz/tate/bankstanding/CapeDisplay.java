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
import java.util.LinkedHashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ColorTextureOverride;
import net.runelite.api.ItemComposition;
import net.runelite.api.ModelData;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.kit.KitType;
import nz.tate.bankstanding.core.CapePalette;

/**
 * Shows the 99 Bank Standing cape on your own character, on your own client only.
 *
 * <p>It swaps the cape slot of the local player's appearance to a base cape and turns it
 * gold. A cape built with recolour slots is recoloured through the appearance's colour
 * override. One without them (the cape of skulls) is given slots on this client's copy of
 * its item definition, taken from its model's colours, so on this client every cape of
 * skulls shows gold. The game rebuilds the appearance whenever equipment changes, so this
 * is re-applied each tick; nothing is sent to the server.
 */
@Slf4j
@Singleton
class CapeDisplay
{
	static final int BASE_CAPE = ItemID.CAPE_OF_SKULLS;

	private final Client client;
	private int capeItem = BASE_CAPE;
	private boolean applied;

	/** Gold colours for the override path, or null when the definition is recoloured instead. */
	private short[] overrideColours;
	/** The definition recolour, kept so it can be re-applied if the game reloads the item. */
	private short[] definitionFrom;
	private short[] definitionTo;
	private boolean prepared;
	private boolean warned;

	@Inject
	CapeDisplay(Client client)
	{
		this.client = client;
	}

	void apply()
	{
		Player player = client.getLocalPlayer();
		PlayerComposition appearance = player == null ? null : player.getPlayerComposition();
		if (appearance == null || !prepare())
		{
			return;
		}

		int[] equipment = appearance.getEquipmentIds();
		int slot = KitType.CAPE.getIndex();
		int wanted = capeItem + PlayerComposition.ITEM_OFFSET;
		boolean changed = equipment[slot] != wanted;
		equipment[slot] = wanted;

		if (overrideColours != null)
		{
			ColorTextureOverride current = appearance.getColorTextureOverride(KitType.CAPE);
			if (current == null || !Arrays.equals(current.getColorToReplaceWith(), overrideColours))
			{
				ColorTextureOverride override = appearance.createColorTextureOverride(KitType.CAPE, overrideColours.length);
				System.arraycopy(overrideColours, 0, override.getColorToReplaceWith(), 0, overrideColours.length);
				changed = true;
			}
		}
		else if (definitionTo != null)
		{
			ItemComposition cape = client.getItemDefinition(capeItem);
			if (!Arrays.equals(cape.getColorToReplaceWith(), definitionTo))
			{
				cape.setColorToReplace(definitionFrom);
				cape.setColorToReplaceWith(definitionTo);
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
		undoDefinition();

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

	/** Which cape item the gold cape is built on. */
	void setCapeItem(int itemId)
	{
		if (itemId != capeItem)
		{
			undoDefinition();
			capeItem = itemId;
			prepared = false;
			warned = false;
		}
	}

	/** Works out the gold colours for the base cape. False until its model has loaded. */
	private boolean prepare()
	{
		if (prepared)
		{
			return true;
		}
		overrideColours = null;
		definitionFrom = null;
		definitionTo = null;

		ItemComposition cape = client.getItemDefinition(capeItem);
		short[] slots = cape.getColorToReplaceWith();
		if (slots != null && slots.length > 0)
		{
			overrideColours = gold(slots);
			prepared = true;
			return true;
		}

		ModelData model = client.loadModelData(cape.getInventoryModel());
		if (model == null)
		{
			if (!warned)
			{
				log.debug("Cape {} model not loaded yet", capeItem);
				warned = true;
			}
			return false;
		}
		Set<Short> distinct = new LinkedHashSet<>();
		for (short colour : model.getFaceColors())
		{
			distinct.add(colour);
		}
		definitionFrom = new short[distinct.size()];
		int i = 0;
		for (short colour : distinct)
		{
			definitionFrom[i++] = colour;
		}
		definitionTo = gold(definitionFrom);
		log.debug("Cape {} recoloured through its definition ({} colours)", capeItem, definitionFrom.length);
		prepared = true;
		return true;
	}

	private void undoDefinition()
	{
		if (definitionTo != null)
		{
			ItemComposition cape = client.getItemDefinition(capeItem);
			if (Arrays.equals(cape.getColorToReplaceWith(), definitionTo))
			{
				cape.setColorToReplace(null);
				cape.setColorToReplaceWith(null);
			}
		}
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
