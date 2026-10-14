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

/**
 * Turns max cape colours into Bank Standing cape colours: bank-booth brown with gold trim.
 *
 * <p>Colours are the game's packed HSL shorts: 6 bits hue, 3 bits saturation, 7 bits
 * lightness. Lightness is always kept so the cape's shading survives; only hue and
 * saturation change. Near-grey colours (detail lines, highlights) are left alone.
 */
public final class CapePalette
{
	static final int BROWN_HUE = 6;
	static final int BROWN_SAT = 3;
	static final int GOLD_HUE = 9;
	static final int GOLD_SAT = 6;

	private CapePalette()
	{
	}

	public static short recolour(short original)
	{
		int hue = (original >> 10) & 0x3F;
		int sat = (original >> 7) & 0x7;
		int light = original & 0x7F;

		if (sat <= 1)
		{
			return original;
		}
		if (hue >= 7 && hue <= 12)
		{
			return pack(GOLD_HUE, GOLD_SAT, light);
		}
		return pack(BROWN_HUE, BROWN_SAT, Math.max(8, light - 10));
	}

	static short pack(int hue, int sat, int light)
	{
		return (short) ((hue << 10) | (sat << 7) | light);
	}
}
