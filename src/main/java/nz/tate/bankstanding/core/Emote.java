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

/** Emotes that matter while bank standing. Only one counts at a time. */
public enum Emote
{
	NONE,
	LEAN,
	YAWN,
	FLEX,
	SKILLCAPE;

	private static final int MIME_LEAN = 1129;
	private static final int YAWN_ANIM = 2111;
	private static final int FLEX_ANIM = 8917;
	private static final int FLEX_LOOP = 12064;
	private static final int[] SKILLCAPE_ANIMS = {
		4937, 4939, 4941, 4943, 4947, 4949, 4951, 4953, 4955, 4957, 4959, 4961, 4963, 4965, 4967,
		4969, 4971, 4973, 4975, 4977, 4979, 4981, 5158, 13074, 13077
	};

	/** The emote this animation belongs to, or NONE. */
	public static Emote fromAnimation(int animation)
	{
		if (animation == MIME_LEAN)
		{
			return LEAN;
		}
		if (animation == YAWN_ANIM)
		{
			return YAWN;
		}
		if (animation == FLEX_ANIM || animation == FLEX_LOOP)
		{
			return FLEX;
		}
		for (int id : SKILLCAPE_ANIMS)
		{
			if (id == animation)
			{
				return SKILLCAPE;
			}
		}
		return NONE;
	}
}
