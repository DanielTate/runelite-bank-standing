package nz.tate.bankstanding.core;

import com.google.gson.Gson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** A save written by 1.0 must load into the current code with nothing lost. */
public class SaveCompatibilityTest
{
	private static final String SAVE_1_0 = "{\"xp\":13034431,"
		+ "\"banks\":{\"ge\":{\"ticks\":60000,\"xp\":13000000,\"x\":3164,\"y\":3487}},"
		+ "\"streakDate\":\"2026-10-14\",\"streakDays\":3,\"todayDate\":\"2026-10-14\",\"todayTicks\":1200,"
		+ "\"keywordPaid\":{\"abc\":1},\"keywordDate\":\"2026-10-14\",\"keywordCount\":1,"
		+ "\"records\":{\"firstSeen\":1,\"totalTicks\":60000,\"longestStandTicks\":7000,\"milestones\":[]},"
		+ "\"collection\":{\"lost-bank-pin\":{\"count\":2,\"firstAt\":5,\"firstRolls\":40}},\"rolls\":99999}";

	@Test
	public void oneZeroSavesLoadWithEverythingKept()
	{
		Progress p = new Gson().fromJson(SAVE_1_0, Progress.class);
		assertEquals(13034431, p.xp);
		assertEquals(60000, p.banks.get("ge").ticks);
		assertEquals(3, p.streakDays);
		assertEquals(99999, p.rolls);
		assertEquals(2, p.collection.get("lost-bank-pin").count);
		assertEquals(7000, p.records.longestStandTicks);
		assertTrue(p.keywordPaid.containsKey("abc"));

		assertNotNull(p.moments);
		assertNotNull(p.daysStood);
		assertEquals(0, p.lastSitRoll);
	}

	@Test
	public void savesSurviveARoundTrip()
	{
		Gson gson = new Gson();
		Progress p = gson.fromJson(SAVE_1_0, Progress.class);
		p.moments.put("odd:midnight", 7L);
		p.daysStood.add("2026-10-15");
		Progress again = gson.fromJson(gson.toJson(p), Progress.class);
		assertEquals(13034431, again.xp);
		assertEquals(Long.valueOf(7), again.moments.get("odd:midnight"));
		assertTrue(again.daysStood.contains("2026-10-15"));
	}

	@Test
	public void theSaveWithTheMostXpWins()
	{
		Progress a = new Progress();
		a.xp = 500;
		a.rolls = 10;
		Progress b = new Progress();
		b.xp = 900;
		b.rolls = 5;
		Progress c = new Progress();
		c.xp = 900;
		c.rolls = 8;
		assertNull(Progress.best(java.util.Collections.emptyList()));
		assertSame(a, Progress.best(java.util.Collections.singletonList(a)));
		assertSame(c, Progress.best(java.util.Arrays.asList(a, b, c)));
		assertSame(c, Progress.best(java.util.Arrays.asList(c, b, a)));
	}

	@Test
	public void mergingKeepsTheMostXpAndEveryFind()
	{
		// This morning: the dev client found two phrases, then a RuneLite with an older copy
		// in memory stood longer and saved over it.
		Progress dev = new Progress();
		dev.xp = 12000;
		dev.rolls = 4800;
		dev.keywordPaid.put("fletch", 100L);
		dev.keywordPaid.put("gf", 200L);
		dev.phraseNotes.put("fletch", new Keywords.Noted("why fletch", "nex", 100L));
		dev.phraseNotes.put("gf", new Keywords.Noted("buying gf", "nex", 200L));
		dev.moments.put("odd:take-a-seat", 50L);
		dev.collection.put("pocket-lint", obtained(9, 10L));
		BankRecord devNex = new BankRecord();
		devNex.ticks = 19000;
		devNex.visits = 3;
		dev.banks.put("nex", devNex);

		Progress stale = new Progress();
		stale.xp = 13576;
		stale.rolls = 5056;
		stale.keywordPaid.put("gf", 5L);
		stale.keywordPaid.put("mass", 300L);
		stale.phraseNotes.put("mass", new Keywords.Noted("nex mass", "nex", 300L));
		stale.moments.put("odd:take-a-seat", 60L);
		stale.collection.put("pocket-lint", obtained(8, 20L));
		BankRecord staleNex = new BankRecord();
		staleNex.ticks = 21291;
		stale.banks.put("nex", staleNex);
		BankRecord ge = new BankRecord();
		ge.ticks = 3458;
		stale.banks.put("ge", ge);

		Progress merged = Progress.merge(java.util.Arrays.asList(dev, stale));
		assertSame(stale, merged);
		assertEquals(13576, merged.xp);
		assertEquals(5056, merged.rolls);
		assertEquals(3, merged.phraseNotes.size());
		assertEquals("why fletch", merged.phraseNotes.get("fletch").said);
		assertEquals(Long.valueOf(200), merged.keywordPaid.get("gf"));
		assertEquals(Long.valueOf(50), merged.moments.get("odd:take-a-seat"));
		assertEquals(9, merged.collection.get("pocket-lint").count);
		assertEquals(10L, merged.collection.get("pocket-lint").firstAt);
		assertEquals(21291, merged.banks.get("nex").ticks);
		assertEquals(3, merged.banks.get("nex").visits);
		assertEquals(3458, merged.banks.get("ge").ticks);
		assertNull(Progress.merge(java.util.Collections.emptyList()));
	}

	private static CollectionLog.Obtained obtained(int count, long firstAt)
	{
		CollectionLog.Obtained obtained = new CollectionLog.Obtained();
		obtained.count = count;
		obtained.firstAt = firstAt;
		return obtained;
	}
}
