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

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

/**
 * Knows where the banks are in the loaded scene.
 *
 * <p>A bank is recognised by what it offers, not by a list of IDs: anything with a "Bank" or
 * "Deposit" option, or a bank chest you can "Use". That covers booths, chests, deposit
 * boxes, bankers and new banks such as Sailing's without updates. Objects that change state,
 * like the Fossil Island chest that has to be built first, are checked through their current
 * form every time, so an unbuilt chest never counts.
 */
@Singleton
class BankLocator
{
	static final int RANGE = 5;

	private final Client client;
	private final Set<TileObject> candidates = new HashSet<>();

	@Inject
	BankLocator(Client client)
	{
		this.client = client;
	}

	void objectSpawned(TileObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition != null && couldBeBank(composition))
		{
			candidates.add(object);
		}
	}

	void objectDespawned(TileObject object)
	{
		candidates.remove(object);
	}

	void clear()
	{
		candidates.clear();
	}

	/** Picks up banks already loaded, for when the plugin starts mid-session. */
	void scan(Scene scene)
	{
		for (Tile[][] plane : scene.getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}
					for (GameObject object : tile.getGameObjects())
					{
						if (object != null)
						{
							objectSpawned(object);
						}
					}
				}
			}
		}
	}

	boolean isNearBank(WorldPoint player)
	{
		for (TileObject object : candidates)
		{
			if (inRange(player, object.getWorldLocation()) && isBank(currentForm(object)))
			{
				return true;
			}
		}

		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			NPCComposition composition = npc.getTransformedComposition();
			if (composition != null && hasAction(composition.getActions(), "Bank")
				&& inRange(player, npc.getWorldLocation()))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean inRange(WorldPoint player, WorldPoint target)
	{
		return target != null && player.getPlane() == target.getPlane() && player.distanceTo2D(target) <= RANGE;
	}

	private ObjectComposition currentForm(TileObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition != null && composition.getImpostorIds() != null)
		{
			return composition.getImpostor();
		}
		return composition;
	}

	/** True if this object, or any form it can change into, is a bank. */
	private boolean couldBeBank(ObjectComposition composition)
	{
		if (isBank(composition))
		{
			return true;
		}
		int[] impostors = composition.getImpostorIds();
		if (impostors != null)
		{
			for (int id : impostors)
			{
				if (id != -1 && isBank(client.getObjectDefinition(id)))
				{
					return true;
				}
			}
		}
		return false;
	}

	static boolean isBank(ObjectComposition composition)
	{
		if (composition == null)
		{
			return false;
		}
		String[] actions = composition.getActions();
		if (hasAction(actions, "Bank") || hasAction(actions, "Deposit"))
		{
			return true;
		}
		String name = composition.getName();
		return name != null && name.toLowerCase().contains("bank chest") && hasAction(actions, "Use");
	}

	private static boolean hasAction(String[] actions, String wanted)
	{
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if (wanted.equalsIgnoreCase(action))
			{
				return true;
			}
		}
		return false;
	}
}
