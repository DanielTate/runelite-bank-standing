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

public class CapePaletteTest
{
	@Test
	public void redBecomesBrownKeepingShade()
	{
		short red = CapePalette.pack(0, 7, 60);
		assertEquals(CapePalette.pack(CapePalette.BROWN_HUE, CapePalette.BROWN_SAT, 50), CapePalette.recolour(red));
	}

	@Test
	public void goldStaysGold()
	{
		short yellow = CapePalette.pack(10, 5, 90);
		assertEquals(CapePalette.pack(CapePalette.GOLD_HUE, CapePalette.GOLD_SAT, 90), CapePalette.recolour(yellow));
	}

	@Test
	public void greysAreUntouched()
	{
		short grey = CapePalette.pack(20, 1, 100);
		assertEquals(grey, CapePalette.recolour(grey));
	}
}
