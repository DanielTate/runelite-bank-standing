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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BankStandingConfig.GROUP)
public interface BankStandingConfig extends Config
{
	String GROUP = "bankstanding";

	@ConfigItem(
		keyName = "showCape",
		name = "Show 99 cape",
		description = "At level 99, wear the Bank Standing cape while you bank stand. Only you can see it."
	)
	default boolean showCape()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRug",
		name = "Show rug",
		description = "From level 10, stand on a rug while you bank stand. Only you can see it."
	)
	default boolean showRug()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSeat",
		name = "Show seat",
		description = "From level 60, sit while you bank stand. Only you can see it."
	)
	default boolean showSeat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPet",
		name = "Show pet",
		description = "Once you have the Bank Standing pet, it sits beside you while you bank stand. Only you can see it."
	)
	default boolean showPet()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fidgets",
		name = "Fidget",
		description = "From level 25, your character fidgets now and then while you bank stand."
	)
	default boolean fidgets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankerChat",
		name = "Bankers talk",
		description = "From level 30, nearby bankers say something now and then while you bank stand."
	)
	default boolean bankerChat()
	{
		return true;
	}
}
