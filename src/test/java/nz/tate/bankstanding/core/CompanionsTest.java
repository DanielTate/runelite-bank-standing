package nz.tate.bankstanding.core;

import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CompanionsTest
{
	@Test
	public void petsAreKnownByNameOnce()
	{
		Progress p = new Progress();
		assertEquals("Lazy hellcat", Companions.collect(p, "Lazy hellcat", 1).name);
		assertNull(Companions.collect(p, "Lazy hellcat", 2));
		assertEquals("Cat", Companions.collect(p, "Cat", 3).name);
		assertNull(Companions.collect(p, "Gertrude's cat", 4));
		assertNull(Companions.collect(p, null, 5));
	}

	@Test
	public void puppiesAndDogsAreTheSameBreed()
	{
		Progress p = new Progress();
		assertEquals("Corgi", Companions.collect(p, "Corgi puppy", 1).name);
		assertNull(Companions.collect(p, "Corgi", 2));
		assertEquals("Bernese mountain dog", Companions.collect(p, "Bernese mountain dog", 3).name);
		assertNull(Companions.collect(p, "Pugel", 4));
	}

	@Test
	public void keysAreUnique()
	{
		Set<String> keys = new HashSet<>();
		for (Companions.Group group : Companions.GROUPS)
		{
			for (Companions.Kind kind : group.kinds)
			{
				assertTrue(keys.add(group.momentKey(kind)));
			}
		}
	}
}
