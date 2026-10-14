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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExperienceTest
{
	@Test
	public void matchesTheOsrsTable()
	{
		assertEquals(0, Experience.xpForLevel(1));
		assertEquals(83, Experience.xpForLevel(2));
		assertEquals(101_333, Experience.xpForLevel(50));
		assertEquals(13_034_431, Experience.xpForLevel(99));
	}

	@Test
	public void levelForXpRoundsDown()
	{
		assertEquals(1, Experience.levelForXp(82));
		assertEquals(2, Experience.levelForXp(83));
		assertEquals(98, Experience.levelForXp(13_034_430));
		assertEquals(99, Experience.levelForXp(13_034_431));
	}

	@Test
	public void staysAt99PastTheTableAndStopsAt200m()
	{
		assertEquals(99, Experience.levelForXp(150_000_000));
		assertEquals(Experience.MAX_XP, Experience.addXp(199_999_999, 5));
	}
}
