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

public class BankNamesTest
{
	@Test
	public void namesAKnownBank()
	{
		assertEquals("varrock-west", BankNames.keyFor(3186, 3436));
		assertEquals("Varrock West", BankNames.displayName("varrock-west", new BankRecord()));
	}

	@Test
	public void picksTheCloserOfTwoNearbyBanks()
	{
		assertEquals("cooking-guild", BankNames.keyFor(3144, 3444));
		assertEquals("ge", BankNames.keyFor(3165, 3488));
	}

	@Test
	public void unknownBanksFallBackToTheirRegionAndCoordinates()
	{
		assertEquals("region-" + ((1000 >> 6) << 8 | (1000 >> 6)), BankNames.keyFor(1000, 1000));
		BankRecord record = new BankRecord();
		record.x = 1000;
		record.y = 1000;
		assertEquals("Unknown bank (1000, 1000)", BankNames.displayName("region-3855", record));
	}

	@Test
	public void regionRecordsMoveOntoANameThatNowCoversThem()
	{
		Progress progress = new Progress();
		progress.bank("region-11601", 2904, 5204).ticks = 40;
		progress.bank("nex", 2903, 5205).ticks = 10;
		progress.bank("region-3855", 1000, 1000).ticks = 5;

		progress.renameBanks();

		assertEquals(50, progress.banks.get("nex").ticks);
		assertEquals(5, progress.banks.get("region-3855").ticks);
		assertEquals(2, progress.banks.size());
	}
}
