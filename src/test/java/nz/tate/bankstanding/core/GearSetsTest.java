package nz.tate.bankstanding.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GearSetsTest
{
	@Test
	public void toolsCountWornOrCarriedAtTheirBankOnlyOnce()
	{
		Progress p = new Progress();
		int[] inventory = {1359, -1, 6739};
		List<GearSets.Tier> found = GearSets.collect(p, "woodcutting-guild", Collections.singleton(28196), inventory, 1);
		assertEquals(3, found.size());
		assertTrue(GearSets.collect(p, "woodcutting-guild", Collections.singleton(1351), inventory, 2).isEmpty());
		assertTrue(GearSets.collect(p, "ge", Collections.singleton(1349), new int[0], 3).isEmpty());
		assertEquals(1, GearSets.collect(p, "warriors-guild", Collections.singleton(24186), new int[0], 4).size());
	}

	@Test
	public void keysAndIdsAreUnique()
	{
		Set<String> keys = new HashSet<>();
		for (GearSets.GearSet set : GearSets.SETS)
		{
			Set<Integer> ids = new HashSet<>();
			for (GearSets.Tier tier : set.tiers)
			{
				assertTrue(keys.add(set.momentKey(tier)));
				for (int id : tier.ids)
				{
					assertTrue(tier.name + " " + id, ids.add(id));
				}
			}
		}
	}

	@Test
	public void outfitsNeedEveryPieceWorn()
	{
		Set<Integer> mime = new HashSet<>(Arrays.asList(3057, 3058, 3059, 3060, 3061));
		Progress p = new Progress();
		assertTrue(GearSets.collect(p, "ge", new HashSet<>(Arrays.asList(3057, 3058)), new int[]{3059, 3060, 3061}, 0)
			.isEmpty());
		List<GearSets.Tier> found = GearSets.collect(p, "ge", mime, new int[0], 0);
		assertEquals(1, found.size());
		assertEquals("Mime outfit", found.get(0).name);
		Progress royal = new Progress();
		assertEquals("Prince or princess outfit",
			GearSets.collect(royal, "draynor", new HashSet<>(Arrays.asList(6186, 6187)), new int[0], 0).get(0).name);
	}
}
