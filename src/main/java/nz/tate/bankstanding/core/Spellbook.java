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
 * The four spellbooks, as the game numbers them, each with the bank it belongs at. Standing
 * there on that spellbook collects it for the log and adds to your max hit.
 */
public enum Spellbook
{
	STANDARD("Standard spellbook", "yanille", 1381),
	ANCIENT("Ancient Magicks", "nex", 4675),
	LUNAR("Lunar spellbook", "lunar-isle", 9084),
	ARCEUUS("Arceuus spellbook", "arceuus", 25818);

	public final String title;
	/** The bank it belongs at. */
	public final String bankKey;
	/** An item to show for it in the log. */
	public final int icon;

	Spellbook(String title, String bankKey, int icon)
	{
		this.title = title;
		this.bankKey = bankKey;
		this.icon = icon;
	}

	/** The spellbook for the game's number (the spellbook varbit), or null if unknown. */
	public static Spellbook of(int varbit)
	{
		Spellbook[] all = values();
		return varbit >= 0 && varbit < all.length ? all[varbit] : null;
	}

	/** The spellbook this moment key collects, or null if it isn't a spellbook's. */
	public static Spellbook forMomentKey(String key)
	{
		for (Spellbook book : values())
		{
			if (book.momentKey().equals(key))
			{
				return book;
			}
		}
		return null;
	}

	/** The moment key for collecting it in the log. */
	public String momentKey()
	{
		return "spellbook:" + name().toLowerCase(java.util.Locale.ENGLISH);
	}
}
