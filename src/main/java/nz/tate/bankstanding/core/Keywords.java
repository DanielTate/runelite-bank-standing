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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Secret phrases that pay out a bonus roll when said in public chat during a stand.
 *
 * <p>Only hashes are stored, so reading the source doesn't give the phrases away. A phrase
 * must be the whole message. Some only work during a {@link Holiday}, some only at certain
 * banks, some only while wearing a particular item. Each pays out at most once an hour, and all of them together at most
 * {@link #DAILY_CAP} times per UTC day. Over a limit, nothing happens at all.
 */
public final class Keywords
{
	static final long COOLDOWN_MILLIS = 60 * 60 * 1000L;
	static final int DAILY_CAP = 10;
	private static final String SALT = "bankstanding:";

	/** What a phrase is tied to, which decides the colour of its hitsplat. */
	public enum Kind
	{
		ANYWHERE,
		LOCATION,
		DATE,
		DATE_AND_LOCATION
	}

	/** How the collection log groups phrases, without saying which is which. */
	public enum Group
	{
		ANYWHERE("Anywhere"),
		BANK("At certain banks"),
		HOLIDAY("On holidays"),
		WORN("While wearing something");

		public final String title;

		Group(String title)
		{
			this.title = title;
		}
	}

	/**
	 * One phrase: its hash, the holiday it needs (or null), the banks it works at (or any) and
	 * items one of which must be worn (or none).
	 */
	public static final class Phrase
	{
		final String hash;
		final Holiday holiday;
		final Set<String> banks;
		Set<Integer> items = Collections.emptySet();

		Phrase(String hash, Holiday holiday, String... banks)
		{
			this.hash = hash;
			this.holiday = holiday;
			this.banks = new HashSet<>(Arrays.asList(banks));
		}

		Phrase wearing(int itemId)
		{
			return wearing(Collections.singleton(itemId));
		}

		/** Works while wearing any one of these. */
		Phrase wearing(Set<Integer> itemIds)
		{
			items = itemIds;
			return this;
		}

		Group group()
		{
			if (!items.isEmpty())
			{
				return Group.WORN;
			}
			if (holiday != null)
			{
				return Group.HOLIDAY;
			}
			return banks.isEmpty() ? Group.ANYWHERE : Group.BANK;
		}

		Kind kind()
		{
			if (holiday != null)
			{
				return banks.isEmpty() ? Kind.DATE : Kind.DATE_AND_LOCATION;
			}
			return banks.isEmpty() ? Kind.ANYWHERE : Kind.LOCATION;
		}

		boolean works(String bankKey, LocalDate today, Set<Integer> worn)
		{
			return (holiday == null || holiday.isOn(today))
				&& (banks.isEmpty() || banks.contains(bankKey))
				&& (items.isEmpty() || items.stream().anyMatch(worn::contains));
		}
	}

	public static final List<Phrase> PHRASES = Collections.unmodifiableList(Arrays.asList(
		phrase("f1a40f3bea462426df466ec436c4b69acb5f96dc774818a5f748e86d31f05c36", null),
		phrase("8582cc1f392d6aaeac0e013312daf544b8c319c2c41255662d58cda0a58ba2dc", null),
		phrase("7433c9bc284c7761c8d6e4d6903f3674fae7f64028627e5c38a5f9ca2007cbfe", null),
		phrase("bbe0aad4ae97389a0b84d70db00eaf256dff739ddc79a16e39edbf2f954ed514", null),
		phrase("cf6ef486e7cd330199eb4e66e5807dcf0d352c3839b9d6b52bd830af6b267344", null),
		phrase("2fec7a5e00d259d7e53c86831bce919927b3cec74883f2c662ab2cc9b6618410", null),
		phrase("5d577393364cce868773ec818f847718f20b4833ba1f33f1f65773184608ced1", null),
		phrase("9a44fcd68b778e1295c42a76193363e3c4761fe04b9ba19baa4430f1df531f55", null),
		phrase("a21ebd2e0315c46fc5463e1b937f43e65f06b0a40cba3a0584316e1fe1359470", null),
		phrase("ac2f3a05b1ea84f3762ceb96b2d68a673d8be3cb72afeecb8a89442ebf71ba9d", null, "edgeville"),
		phrase("77826aa3d7a7e56968d8503f656c6a9ac60cd86bc0f3cfe0eb276682a61bc534", null, "edgeville", "ferox"),
		phrase("8524ea71afe382572d7664a2618bdf54aa3dde61b1b179f11b8db754baf8ce58", null, "ge"),
		phrase("a14bad2d39452bd846063ae26c959cdaede02255eafa12931d04e7212286914c", null, "catherby", "fishing-guild"),
		phrase("16df1eee6d654260c78cfc7f12fb1db54b3f66365edc255fdf8f14d233a19e52", null, "seers", "woodcutting-guild"),
		phrase("e32e60741e6dd9240bcbe1a3c5bf6761874f685acd18c7e7f4db7cfd8fe68b2f", null, "castle-wars"),
		phrase("9733234380c3a2dc87947049ebf56c63a99bbeac1ab8425ff4c4dc11f82a9e92", null, "castle-wars"),
		phrase("c4a0d90caad82e8049130fb6119919735c427f11a92290bd8c5c3d4c1eb195fa", null, "pest-control"),
		phrase("8697169fbd4b86db578967aefdcc78398210b076a44a41a56320854b3603efaf", null, "wintertodt"),
		phrase("4189b476622c54b6fe056610a4c5af836e2c5df34623c323891a39e7bdeb5a6f", null, "tzhaar", "mor-ul-rek"),
		phrase("f38b7bfa7e5367c11d1897c8cf83926ac8d2179bb2092c5748cfa054b0e0247a", null, "nex"),
		phrase("1396b9bd8364b0213f860607ac7e8452034e76ee84566c9b8bd5170e3b1aabbc", null, "draynor"),
		phrase("f657cbfa248eea28340ed09b957a994224089cf881001304209b2c53d7631b3b", null, "falador-east", "falador-west"),
		phrase("bc5b8b12ad7d193e94f0ea34e7fc68acf66363e158281205249abdd2c7e31a0b", Holiday.CHRISTMAS),
		phrase("e53175cdbfa24033b9c0ca04d50b21e9a27e69e393131f99d36821ac3e3c4468", Holiday.CHRISTMAS),
		phrase("ded9c2514a150d66d902a8a9b7f2e64a1ad58ab800d2b5826a6fb394572440d7", Holiday.NEW_YEAR),
		phrase("f60c7c1aee29d4af1095b0e0d9ea255eea0e4f00441e615e6ebf980d7bb6bd3e", Holiday.HALLOWEEN),
		phrase("c0ea0a42b798341610c61a96387fe959101865073975175c217a3fb5900ef57f", Holiday.HALLOWEEN),
		phrase("76cace42c135a2f71be350a5aef28d6d8c3ee8f26ed6dde1038f436615e3167f", Holiday.HALLOWEEN, "canifis"),
		phrase("dd182d91665152a0b62bd9cf21eaa040f5f94a6146e4e142fc5c57bdfe745b18", Holiday.EASTER),
		phrase("18bdf57c428088df99a1ba9394e57d5e59a4ed5730da547a5104e377c63f230b", Holiday.EASTER),
		phrase("7d21a448f37a0ea658451f5ab3615a6cffdaf95b09fe9951ec302bbb08c8c729", Holiday.BIRTHDAY),
		phrase("af75ea8c8362c6e7eabe097bd74d8255717794b0068fe65b12c4e7f59de3e14f", null).wearing(20220),
		phrase("e3fb3409ba47e141aa21300d018b10bc931fa1f558111883e8451f57915b57a5", null).wearing(20223),
		phrase("6df044d1db7a19d6be624522a9d1fe5f64b30fab753653c638239eb6e7d74a5f", null).wearing(20226),
		phrase("ea83ea768ce98829e7b63f0ed5fa943e33fd817edb6e1edb0d1c70cc1f2747dd", null).wearing(20229),
		phrase("9960b6e4a7f69bf19af8696187d742730fb623654f2de72d52544661764a15d5", null).wearing(20232),
		phrase("8bcda4597a0c3eb5ce3d68398e5efd6ba0947202827c02a2d55f5acec33169f0", null).wearing(20235),
		phrase("179c7af78a9f76e8e88bb78bceee38b97f09638af3aac4ee7153f13542f7ca17", null, "emirs-arena"),
		phrase("8ec5fe51a033ee4d54485176fae4127eb4fc529cefd6cd3ef02c8e51567f418a", null, "varrock-west"),
		phrase("a2e2b8d40cff52ff850240516883a9c54b96f43db6dde9b348de8f3b4519db0b", null, "al-kharid"),
		phrase("96ecfbb4e884f6dddf4bf99ac9ad5b5594f2c4a3baa884642772079976d278b9", null, "barbarian-outpost"),
		phrase("f8ddc232597a0ebdff26558479b02ca3228333188b34af736f17709ad7780831", null, "motherlode"),
		phrase("5a9f55a99b6eb91e923325fdb1c72d9c2262259214a09a30e822c74a14845662", null, "warriors-guild"),
		phrase("779c14e0107a88268492094728daeb4ed2775ef1719c7438907c260f543511f8", null, "ge"),
		phrase("19e87ac01e491238d2016f222af2aa6f9db27ed66ed7644a116bedda25caa3a1", null, "chambers-of-xeric", "quidamortem"),
		phrase("dc46e45ff7ccb2ad7714c4f915eb9d955022df684c7f1ee4e9fe89ac4bfc82bf", null, "chambers-of-xeric", "quidamortem"),
		phrase("ddb98e95e5a47eb9b1d41d14ea91b9ae7dc0e932e5692be9f1edffb2b6b664cd", null, "ver-sinhaza"),
		phrase("6ce941d72f8e833e2079369d6600b9fff05833f816290423c3e48d3bbfec7809", null, "ver-sinhaza"),
		phrase("850e714a3448c52c59a8bdb45bd179037693d9986520235797c494c10991daa7", null, "tombs-of-amascut"),
		phrase("f13ae54eea13e0e253f5f348da9a5664bab1a44dd06b838aa219f4d94e1b044b", null, "tombs-of-amascut"),
		phrase("ca904fb5586f1ddc8d04fff3af828406901c012a67db2570fc28521e55c435f1", null, "nex"),
		phrase("b26b0db49a4aa305caccede90cf130d0f9b7f2a0cfa8a3a55d1a6e3365c49fc5", null, "mor-ul-rek"),
		phrase("a663942136c915e44f6124caeb456b4ab962a95c44f306f9f58ade4e073c1ef1", null, "colosseum"),
		phrase("99e3a9deff78b72552ef595147e906cedfb776227da22e0e1684ec72653b98d5", null, "mount-karuulm"),
		phrase("e3e02e2377cc3955ac2c849cf5929fbafdd70de00c9a8f0d53d30230a7529e36", null, "gauntlet"),
		phrase("06a68e00656bf8952ff8bc8b6f4126405597f82bfc51990a38add85449908529", null, "zul-andra"),
		phrase("0d953af17326894f280fa5eb38d18646e163a676a45327c0d8a00b7d20a4b1e7", null).wearing(Items.PARTYHATS),
		phrase("db2fa77eecd113939abf4fc8dcadeabe7dfd3832b7ac26ab09d5f72983f4a1fc", null).wearing(Items.RING_OF_WEALTH),
		phrase("7592dd9d37397223258aaa0f39dc4e438b8842507cccfc35d7d5d9e3d64e73c2", null, "draynor").wearing(Items.FAKE_BEARD),
		phrase("66ca12f2ac9b12bb0efd21c07567aedd8b2d044b39a159f51c79c5f1e5977955", null).wearing(Items.CABBAGE),
		phrase("82f0f54adfac04445a83131e1264c0069d421852669c747642741d8b118c52a6", Holiday.CHRISTMAS),
		phrase("fbe48d6dbdbf043b98c23d3e75a8e6aa34499d5a6aabaef155a308637a9a01c9", Holiday.HALLOWEEN),
		phrase("760a6b0373e1cf4cc69ebc8f1f638db9e7077f40311c2672f5ee18db7d86f59a", Holiday.NEW_YEAR),
		phrase("3204353185e09be163269cd65f3cef13a5639663e7db3f9c5d08e020ea731d47", Holiday.FALADOR_MASSACRE, "falador-east", "falador-west"),
		phrase("436bd2d2e747da47493a2eaf0efedb6d0a956f3c0e202a100ada18d2419dfb60", null),
		phrase("6a1c0eb86ee2469dc74b65cc219b607ab4ae5c2f2d67885eb99a732b9c69e45f", null),
		phrase("cf2ddc9d7f63e6592fd310d0aad96c3a10322b4300e946475345f8d96cb250dc", null),
		phrase("83f86bce36c272019f2f328367d8657c48d3f12636e8538152a2ef524427dd98", null, "falador-east", "falador-west"),
		phrase("542bb96d81a99c4780661a8543dbe90dd871c47af6816985f3111b6c22d262bb", null, "lumbridge"),
		phrase("8872e5b4f35036452a60775ba4fd85806b2ca778f98320fcbac03f3ddd90386a", null, "lumbridge"),
		phrase("e474ef2ffc9262541161589eed9e1c97fb55a710205ac47f98ca85a69ea78085", null, "tutorial-island"),
		phrase("5e0a4cbe0a0e23367aba94a6d33a807408b4357e109aeaf703e67ae9ca16b0d4", null, "varrock-east"),
		phrase("69b543f7ec87f2d82b859fa24b6a25768de55da61875e716e2f1568a6d4496c1", null, "seers"),
		phrase("d1766f3897ea8a8c6542d81952a50c51283ab9900fc4dab1ec9522531b05c5dc", null, "catherby"),
		phrase("9eaa1f638170b511933fa40a80d500a218e55d7119f7694ab684de1bcf8c2f41", null, "ardougne-south"),
		phrase("3c1f1edb15355c820e24e03209c49c3c8e2d5001f06da56b74ec7437f2dde944", null, "draynor"),
		phrase("6535163e94f8cf6a4da75a055379da8dd4bcf29c8432fe9cb4af5338fd9b14b1", Holiday.HALLOWEEN, "draynor"),
		phrase("812612409f5e9da77909da67192341fc742286e9c2466d055addf39f1cc7ec71", null).wearing(Items.ids(1333, 23330, 23332, 23334)),
		phrase("229d295af8b3509c6c6f1ea327f7c5a0bd43cf49fa49bc9c10ad992cdc02dfce", null).wearing(Items.ids(1149)),
		phrase("50b100fb48ec767a128ba8fc0a8d81b38d5abf797c77dc1cbc06c32afb2a812b", Holiday.CHRISTMAS),
		phrase("b8a38d70d0a2069a96bfc6568521f9c995c47ed78c1eafa089bb026437befde6", null, "port-sarim", "port-khazard", "port-roberts", "port-phasmatys", "port-piscarilius", "deepfin-point", "great-conch", "great-conch-dock", "onyx-crest", "charred-island", "sunbleak-island", "bank-boat", "buccaneers-haven", "red-rock", "sangvesti", "wyrmscraig", "pandemonium", "aldarin-dock", "fortis-cothon", "corsair-cove"),
		phrase("20dd4c9aba28915fa2accbbd607fc0f3d245e495eac0d2b6418457ec75b77401", null, "port-sarim", "port-khazard", "port-roberts", "port-phasmatys", "port-piscarilius", "deepfin-point", "great-conch", "great-conch-dock", "onyx-crest", "charred-island", "sunbleak-island", "bank-boat", "buccaneers-haven", "red-rock", "sangvesti", "wyrmscraig", "pandemonium", "aldarin-dock", "fortis-cothon", "corsair-cove"),
		phrase("ed5ba4c5e2c9c1ee8793b4020063e45b5071d4e9cf1b30e02079571d7b101122", null, "port-sarim", "port-khazard", "port-roberts", "port-phasmatys", "port-piscarilius", "deepfin-point", "great-conch", "great-conch-dock", "onyx-crest", "charred-island", "sunbleak-island", "bank-boat", "buccaneers-haven", "red-rock", "sangvesti", "wyrmscraig", "pandemonium", "aldarin-dock", "fortis-cothon", "corsair-cove"),
		phrase("6debdbb29808c215b8c6531c30a2fea3da9db4fd28b94c155204a3dc745c22b4", null, "port-sarim", "port-khazard", "port-roberts", "port-phasmatys", "port-piscarilius", "deepfin-point", "great-conch", "great-conch-dock", "onyx-crest", "charred-island", "sunbleak-island", "bank-boat", "buccaneers-haven", "red-rock", "sangvesti", "wyrmscraig", "pandemonium", "aldarin-dock", "fortis-cothon", "corsair-cove"),
		phrase("8c61d90145a80b410ac163f091541bfea4187abff81a80dabe9fb1678ce01d79", null, "bank-boat"),
		phrase("9caa71839e6742168e157b77ec46ada1e37de363f163b8634c8ebde3971671c1", null, "buccaneers-haven"),
		phrase("57be3b1ed4e12d90ee053e2df0c8bbae0131d68defa69ee1dbd32f1c34fc7802", null, "great-conch", "great-conch-dock"),
		phrase("2258435f3419ffa2a7168164d2cff26fde4fb8e8ef3908d01d5057a2212fe497", null, "fortis", "fortis-west"),
		phrase("45fe52eaad7ed84448a2daf802a85df0ca9135a93e80633220b5d6a98bf303db", null, "fortis", "fortis-west"),
		phrase("12030676ecf692440fccaf63d9db5ec1087f023243a2d2fbda1f0532b13a12e8", null, "hunters-guild"),
		phrase("070e0bcd0dde1eeb7ae4d4a2c1404ebc3bbaa4058e4c5dcc1f45c2ab6c3240b9", null, "cam-torum"),
		phrase("d8db0a40f5dfc1678d9d2746ce1eef22a81fb3b0e8d04dbdb11450309f273726", null, "aldarin", "aldarin-dock"),
		phrase("895c6a330f2f4df62f63e812f8029ed4eee3f2e3935228956f88b085f5ef5b41", null, "arceuus"),
		phrase("3ca1d15527c5270298ea3d3a97bc372e1c00d811a958fdd49ccb74e1665339da", null, "hosidius"),
		phrase("6b9e9e9cbc740cf2950474b239ad379196029de1b04643bedcc802406e66740c", null, "unkah"),
		phrase("c83d9127c6d86c8141b647f46a7b1d9c487f3ae3a31c88b5adc49cbb81e3faa2", null, "guardians-of-the-rift"),
		phrase("1a2112315793c6122fb25b7621f9f9dda05d30f223668423dc6126e6b631d34f", null, "hallowed-sepulchre"),
		phrase("bbb94d6f7a44186b95243d552788af23dcd972d665a3048bf5f573639f5f4bfe", null, "prifddinas-north", "prifddinas-south")
	));

	private final List<Phrase> phrases;

	public Keywords(List<Phrase> phrases)
	{
		this.phrases = phrases;
	}

	public int count()
	{
		return phrases.size();
	}

	/** How many of the phrases have ever paid out, without saying which. */
	public int found(Progress p)
	{
		int found = 0;
		for (Phrase phrase : phrases)
		{
			if (p.keywordPaid.containsKey(phrase.hash))
			{
				found++;
			}
		}
		return found;
	}

	/** Found and total for each group, as {found, total}. */
	public Map<Group, int[]> foundByGroup(Progress p)
	{
		Map<Group, int[]> counts = new EnumMap<>(Group.class);
		for (Group group : Group.values())
		{
			counts.put(group, new int[2]);
		}
		for (Phrase phrase : phrases)
		{
			int[] count = counts.get(phrase.group());
			count[1]++;
			if (p.keywordPaid.containsKey(phrase.hash))
			{
				count[0]++;
			}
		}
		return counts;
	}

	/** The kinds of phrase that have ever paid out. */
	public Set<Kind> foundKinds(Progress p)
	{
		Set<Kind> kinds = EnumSet.noneOf(Kind.class);
		for (Phrase phrase : phrases)
		{
			if (p.keywordPaid.containsKey(phrase.hash))
			{
				kinds.add(phrase.kind());
			}
		}
		return kinds;
	}

	static Phrase phrase(String hash, Holiday holiday, String... banks)
	{
		return new Phrase(hash, holiday, banks);
	}

	/**
	 * The kind of phrase this message is, if it may pay out here and now (and records the
	 * payout); otherwise null.
	 */
	public Kind claim(Progress p, String message, long nowMillis, LocalDate today, String bankKey, Set<Integer> worn)
	{
		String hash = hash(normalise(message));
		Phrase match = null;
		for (Phrase phrase : phrases)
		{
			if (phrase.hash.equals(hash) && phrase.works(bankKey, today, worn))
			{
				match = phrase;
				break;
			}
		}
		if (match == null)
		{
			return null;
		}

		Long last = p.keywordPaid.get(hash);
		if (last != null && nowMillis - last < COOLDOWN_MILLIS)
		{
			return null;
		}

		String day = today.toString();
		if (!day.equals(p.keywordDate))
		{
			p.keywordDate = day;
			p.keywordCount = 0;
		}
		if (p.keywordCount >= DAILY_CAP)
		{
			return null;
		}

		p.keywordCount++;
		p.keywordPaid.put(hash, nowMillis);
		return match.kind();
	}

	static String normalise(String message)
	{
		return message.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ENGLISH);
	}

	static String hash(String phrase)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-256").digest((SALT + phrase).getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(64);
			for (byte b : digest)
			{
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
