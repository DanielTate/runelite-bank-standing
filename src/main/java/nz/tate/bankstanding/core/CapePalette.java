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
 * Turns the base cape's colours into Bank Standing gold.
 *
 * <p>Colours are the game's packed HSL shorts: 6 bits hue, 3 bits saturation, 7 bits
 * lightness. Every colour becomes gold; lightness is squeezed into a range that still
 * reads as gold, keeping its order so the cape's shading and pattern survive (dark cloth
 * becomes deep gold, light details become pale gold).
 */
public final class CapePalette
{
	static final int GOLD_HUE = 8;
	static final int GOLD_SAT = 6;
	static final int MIN_LIGHT = 30;
	static final int MAX_LIGHT = 100;

	private CapePalette()
	{
	}

	public static short recolour(short original)
	{
		int light = original & 0x7F;
		return pack(GOLD_HUE, GOLD_SAT, MIN_LIGHT + light * (MAX_LIGHT - MIN_LIGHT) / 127);
	}

	static short pack(int hue, int sat, int light)
	{
		return (short) ((hue << 10) | (sat << 7) | light);
	}
}
