package nz.tate.bankstanding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PoisonTest
{
	@Test
	public void poisonedAndToxicItems()
	{
		assertTrue(BankStandingPlugin.isPoisonous("Dragon dagger(p++)"));
		assertTrue(BankStandingPlugin.isPoisonous("Abyssal tentacle (kp)"));
		assertTrue(BankStandingPlugin.isPoisonous("Toxic blowpipe"));
		assertTrue(BankStandingPlugin.isPoisonous("Serpentine helm"));
		assertFalse(BankStandingPlugin.isPoisonous("Dragon dagger"));
		assertFalse(BankStandingPlugin.isPoisonous(null));
	}

	@Test
	public void clueTiersCountScrollsOnly()
	{
		assertEquals("elite", BankStandingPlugin.clueTier("Clue scroll (elite)"));
		assertNull(BankStandingPlugin.clueTier("Clue box"));
		assertNull(BankStandingPlugin.clueTier("Reward casket (easy)"));
		assertNull(BankStandingPlugin.clueTier(null));
	}
}
