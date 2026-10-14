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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import javax.inject.Singleton;
import lombok.Value;

/** Recent drops, shared by the XP drop and hitsplat overlays. */
@Singleton
class DropFeed
{
	static final long XP_DROP_MILLIS = 1800;
	static final long HITSPLAT_MILLIS = 1200;

	@Value
	static class Drop
	{
		int amount;
		/** Exactly the normal max hit for the level (from level 3, where the max is above 1). */
		boolean maxHit;
		/** Above the normal max hit for the level, which only a gear bonus allows. */
		boolean boosted;
		long createdMillis;
	}

	private final Deque<Drop> drops = new ArrayDeque<>();

	synchronized void add(int amount, boolean maxHit, boolean boosted)
	{
		drops.addLast(new Drop(amount, maxHit, boosted, System.currentTimeMillis()));
	}

	/** Drops younger than {@code millis}, oldest first. */
	synchronized Drop[] recent(long millis)
	{
		long now = System.currentTimeMillis();
		for (Iterator<Drop> it = drops.iterator(); it.hasNext(); )
		{
			if (now - it.next().getCreatedMillis() > XP_DROP_MILLIS)
			{
				it.remove();
			}
		}
		return drops.stream().filter(d -> now - d.getCreatedMillis() <= millis).toArray(Drop[]::new);
	}

	synchronized void clear()
	{
		drops.clear();
	}
}
