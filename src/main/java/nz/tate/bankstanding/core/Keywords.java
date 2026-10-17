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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Secret phrases that pay out a bonus roll when said in public chat during a stand.
 *
 * <p>Only hashes are stored, so reading the source doesn't give the phrases away. A phrase
 * must be the whole message. Some only work during a {@link Holiday}, some only at certain
 * banks, some only while wearing a particular item. Each pays out at most once an hour, and all of them together at most
 * {@link #DAILY_CAP} times per day. Over a limit, nothing happens at all.
 */
public final class Keywords
{
	static final long COOLDOWN_MILLIS = 60 * 60 * 1000L;
	static final int DAILY_CAP = 10;
	private static final String SALT = "bankstanding:";
	private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[\\s!?.,~]+$");

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
		WORN("While wearing something"),
		SPELLBOOK("On a spellbook");

		public final String title;

		Group(String title)
		{
			this.title = title;
		}
	}

	/** A found phrase as the journal shows it: what was said, where and when it was first found. */
	public static final class Noted
	{
		/** What was said, or null if it was found before the journal kept notes. */
		public String said;
		public String bank;
		public long at;
		/** Whether it's been seen in the journal; new ones get a star until then. */
		public boolean read;

		Noted()
		{
		}

		Noted(String said, String bank, long at)
		{
			this.said = said;
			this.bank = bank;
			this.at = at;
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
		/** The hash is of what comes before your clan's name, which must end the message. */
		boolean beforeClanName;
		/** The spellbook you must be on, or null for any. */
		Spellbook spellbook;

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

		/** The message must end with your clan's name; the hash is of the rest. */
		Phrase beforeClanName()
		{
			beforeClanName = true;
			return this;
		}

		/** Works only while on this spellbook. */
		Phrase onSpellbook(Spellbook book)
		{
			spellbook = book;
			return this;
		}

		/** Works while wearing any one of these. */
		Phrase wearing(Set<Integer> itemIds)
		{
			items = itemIds;
			return this;
		}

		Group group()
		{
			if (spellbook != null)
			{
				return Group.SPELLBOOK;
			}
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

		boolean works(String bankKey, LocalDate today, Set<Integer> worn, Spellbook book)
		{
			return (spellbook == null || spellbook == book)
				&& (holiday == null || holiday.isOn(today))
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
		phrase("b8a38d70d0a2069a96bfc6568521f9c995c47ed78c1eafa089bb026437befde6", null, "port-sarim", "port-khazard", "port-roberts", "port-phasmatys", "port-piscarilius", "deepfin-point", "great-conch", "great-conch-dock", "onyx-crest", "charred-island", "sunbleak-island", "bank-boat", "buccaneers-haven", "red-rock", "wyrmscraig", "pandemonium", "aldarin-dock", "fortis-cothon", "corsair-cove"),
		phrase("20dd4c9aba28915fa2accbbd607fc0f3d245e495eac0d2b6418457ec75b77401", null, "port-sarim", "port-khazard", "port-roberts", "port-phasmatys", "port-piscarilius", "deepfin-point", "great-conch", "great-conch-dock", "onyx-crest", "charred-island", "sunbleak-island", "bank-boat", "buccaneers-haven", "red-rock", "wyrmscraig", "pandemonium", "aldarin-dock", "fortis-cothon", "corsair-cove"),
		phrase("ed5ba4c5e2c9c1ee8793b4020063e45b5071d4e9cf1b30e02079571d7b101122", null, "port-sarim", "port-khazard", "port-roberts", "port-phasmatys", "port-piscarilius", "deepfin-point", "great-conch", "great-conch-dock", "onyx-crest", "charred-island", "sunbleak-island", "bank-boat", "buccaneers-haven", "red-rock", "wyrmscraig", "pandemonium", "aldarin-dock", "fortis-cothon", "corsair-cove"),
		phrase("6debdbb29808c215b8c6531c30a2fea3da9db4fd28b94c155204a3dc745c22b4", null, "port-sarim", "port-khazard", "port-roberts", "port-phasmatys", "port-piscarilius", "deepfin-point", "great-conch", "great-conch-dock", "onyx-crest", "charred-island", "sunbleak-island", "bank-boat", "buccaneers-haven", "red-rock", "wyrmscraig", "pandemonium", "aldarin-dock", "fortis-cothon", "corsair-cove"),
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
		phrase("bbb94d6f7a44186b95243d552788af23dcd972d665a3048bf5f573639f5f4bfe", null, "prifddinas-north", "prifddinas-south"),
		phrase("337e35b8b2c7aa2f4cc5e6faab267ad986ad86b0811c4db0e0c4dfe2d37fb2d9", null, "clan-hall").beforeClanName(),
		phrase("8031271ac7f29f23fa9c3cd680add24eb47d0d0ca1ec3eea7c6ee9455acb9369", null),
		phrase("ace437f10302bcdc359a0ae1587af9f2491df0f5bf1570660cf54017cad05beb", null, "ge"),
		phrase("176bd9faa4e540a087649f9d38592776211a62a4c2fc826a20b9e28e7b01f4e5", null),
		phrase("b10923ad2252afd273d62a9bc11c3f5a00df908094ad4f809bb79713713367c1", null),
		phrase("92c5e8703cce56fc34c35c9c4407c98d933faa4a83c10c731447cf1b954ffc4e", null),
		phrase("e8d1a628cdaab15bdcf693e7e490897a1b8876e1f1093f643fe37b56d8d68b96", null),
		phrase("198594e8e5aa48c66eff8704da0a1bcb40e515bd2948c394f3663dd153f53df1", null),
		phrase("afd47fd44b3876addf2d23abe9d5c006007671bf5d85f5e26d35ed1b8264f211", null),
		phrase("3a68701175e9e93c1d1a94fff4d9db6d4e71abb8554e5df66c09579bc420241a", null),
		phrase("d168c9de5db8be51b844dacb35c92ad1de58c7438d436e622c4562e05fc57506", null),
		phrase("7200d0b296a360a4ffb19cf8b203eff017b3344e22de45d1e3eb91b7f5458513", null),
		phrase("00de770518035f9477444110f9e03d76abcba26fbd26bb847e9b64084c1ed798", null),
		phrase("8614ade2aaa8199506975759a6e1571a082be1feb2465fc295332c392e2765df", null),
		phrase("cc96235486063307f664180960eb0ed6ebf621d7e7645a8f41220c2b0f4736d7", null),
		phrase("9d053cc3513f722ecf97e4489d083a44e908afd32370eb794e52673ef3586ca8", null),
		phrase("9694e89e01e35af81e2de063e3956961b655c0e8db3868c4842f60c8f1b0fdec", null),
		phrase("fbd079044f005bcd70fb7e77528d46b4c4dc86ab2d7e0c39aaf78ed7f3e7bc5d", null),
		phrase("f5466683fcb9144c463832063cf9ce90d1202247d3b72bddd999c4f2909bb776", null),
		phrase("09d11b38532e4cb55a54882147353a3cda545f6f4d6782fe5ed7500414d95540", null),
		phrase("703c76fba9274a492df0b92d7b70be87ab107442de24d1f342047d769a8c1e22", null),
		phrase("33b5eab62d3881df625f090ec9410bb2dcbc2558ae9af68777dedf05a2056142", null),
		phrase("15b1808ed60aa90ea535e8ad70ea8e4f76a17e43f6e905984981f60be790ef3d", null),
		phrase("175c83e80eb1353d8e198a13a72634cdc95903f3be94f82b2e017db3993ebc1f", null),
		phrase("730d72282314be2fc7aef6302e5bb48d4bcc92c202ad1d02b5c671d0c1f682de", null),
		phrase("b739b80f581c62d09e6d6dd7937a1bfcaf0c0790d22675adda06abda592e2d4d", null),
		phrase("acbbd8c2d78f349880a80b68a88a8a56a8051dfb1e07eb029991821d7d75c4fd", null, "edgeville"),
		phrase("4a344616a1f18bdd2205e77919c6cc8317f382466ce579933744dee41351f992", null, "edgeville", "ferox"),
		phrase("2cb4e43c8644e397af1cad2bef0d49d4fd4b33d6dc60f60235fa063fc81d001e", null, "ferox"),
		phrase("c9b598c0010c75a46803cfe21d45c9f48ee6c8d096e5de812d8f65ec0f6525ca", null, "al-kharid"),
		phrase("dbbe28c4f7dc4e2b69999c8e9c11bca61d5fa822293079e4bed8ddeeb561f599", null, "al-kharid"),
		phrase("e33ac3f82480aa6b98da09543085681f361b6a52b010ad4256a7b417ed0f0bd6", null, "port-sarim"),
		phrase("82ead4ddab988de1f64e04cc367b318817320febec50c23329b3c74d77ad6ca3", null, "yanille"),
		phrase("1e90a17911cd190bd83798f1f594ee935e860e172fb77cd97cd07bae086ec895", null, "gnome-stronghold-south", "gnome-stronghold-north"),
		phrase("e6bb54a3ca9cec8e16cffcafd36f68522fef10be86af8d8580a3268198df5df8", null, "ardougne-north", "ardougne-south"),
		phrase("f9f7750f7733da4159d5391a793c4bf68cc4dfa42a131c53e49b15d08329b9bd", null, "zanaris"),
		phrase("13e9457824036a7a9a09cd87a56c973623d9b7fbb362680e829a0ddfffcb70cb", null, "mount-karuulm"),
		phrase("bd1d4e87381e59a7be8cb051556c84a13ed862f87be1a972b2bd5163db4c84eb", null, "keldagrim"),
		phrase("8baabc2573439230e7e767487c5a11471a7bad12c87c6312004eeb602444a80a", null, "keldagrim"),
		phrase("59b9b402dca374e09c7a080ac99d37bf1d82b1467615945eb784232342a82221", null, "darkmeyer"),
		phrase("3d8605356e61215eb129c8099164be21309fabc9a7b9b7a1299e5e61a26d9657", null, "lunar-isle"),
		phrase("9bc2426343a76f5a19adaa740d044c957a26dcf95b6969c5823d59cdfcfcfc73", null, "neitiznot"),
		phrase("fc093eead0cdc2bf99f7b570bd38abe98043232486b3f398c64ba8482665baa6", null, "port-phasmatys"),
		phrase("c2c863110166d56259ae80abbdef0d89371f222e635c46a68d19a5fb15104522", null, "pest-control"),
		phrase("60acae7cb90a3244be7d8adc151bea1592e6a7d6134cfd6b6d8ccef8a7dcf0f6", null, "castle-wars"),
		phrase("98ab22b86bf13f5515ea978001fc02406314a65f4a2715de6efa4258c1bead9d", null, "piscatoris"),
		phrase("89592e6e426db64ea7ae6f5918152ebe74c7279a2c8ab90ad6616a5954486887", null, "volcanic-mine"),
		phrase("d18263a9f371fcd36af79d90acf9f94821e43a7233c4297f8096f29737f06ac5", null, "blast-furnace"),
		phrase("6ccfa7e76cef0d90858d4708e99c04cb34cbe429d944c72de27e67351953bd78", null, "rogues-den"),
		phrase("e58b826f91d2bc86ff07a54788fcc8ba725ba82db6d163f1b747f2bde4c0fba8", null, "farming-guild", "farming-guild-upper"),
		phrase("5f6308c184e3020b51fb4de6b08b3e589b0c8bc7fc9896c3636596cb34e10100", null, "falador-east", "falador-west"),
		phrase("ada0ebca23366d45e367ef726a5ff1d4b48ba389e8141ade69936a6730836fce", null, "shantay-pass"),
		phrase("f651f28436291d40910fd3532a7064010d867af62f4bb9bfb385d0f934187b88", Holiday.CHRISTMAS),
		phrase("9afd166f983d8c4f6805a76ae8bfed885a87140e014b37b1e920085e43e2f7d3", Holiday.HALLOWEEN),
		phrase("0b8f09c42f05ea949250ef9c1b033d9261355de0d9a21232bbec078721ef0f0d", Holiday.EASTER),
		phrase("3340a2d27861a131bfce83203c1536802cbfd265d6b2b3dfe46106851620343e", Holiday.BIRTHDAY),
		phrase("c92b74a3399e45ed4a767227c8b9bd4ecc5679fd485d0c013318ee9d8694f0b9", Holiday.FALADOR_MASSACRE, "falador-east", "falador-west"),
		phrase("5ad2ce896582e2c3d68e5c14f3898bef8ca2bdcbb64265b8b58ffbbf47a88663", null).wearing(Items.ABYSSAL_WHIPS),
		phrase("6637965d85ade42e336a7aad92c733b28491588e8779509533ec3a466a1cfb73", null).wearing(Items.TWISTED_BOWS),
		phrase("1c23358fd07d8681e10ac6d603bac3b2262ab81cf81003f59623ee5df13ccd9b", null).wearing(Items.MAX_CAPES),
		phrase("7d2bb49cabc5d76cbc34e7f5c39ca3d259a594a93f5b19ac93d5ba15c728cf34", null).wearing(Items.RUBBER_CHICKENS),
		// 1.3
		phrase("82aab639c777a87c68ddc45c2c9710507f52226d59fb05d2f1a19141035b9c07", null, "ver-sinhaza"),
		phrase("37a0775f7eee473a3a81f9a8e475119ce469e7212908f345ae0662e67ab24f47", null, "ver-sinhaza"),
		phrase("04172c579fccd033e51def482a7f14df3540b458d6b85454def259be2a9ff7f1", null, "ver-sinhaza"),
		phrase("7e7b512bfb74b1715016546388b9d7ee1982ed3d345f051b68454ec679a18c6f", null, "ver-sinhaza"),
		phrase("b66afe43d9e7d17e6882ae0a69580295da9970694b096c209e3b6502a0baea8b", null, "ver-sinhaza"),
		phrase("6f2565e9d15a315b42476c65fc8c2750cf9acbb6c89bcc01d617ed83658aceff", null, "ver-sinhaza"),
		phrase("4f96ae74ee7b046a5eae7adff971b3fc168990a5617bd6f2f169694460da83bd", null, "ver-sinhaza"),
		phrase("ce18590a7cb64986602ff0ae89fc5b385241307fe6874d97cf2297dd5cca5531", null, "ver-sinhaza"),
		phrase("04f01c98faabde2e8ff7a0ea257a22aa566a18c16868c90adca3f7a6394fc06c", null, "ver-sinhaza"),
		phrase("de95905fd65bd49e64b5d5fe6e03d1b126e294614a674a0c32b12b04a37a7a50", Holiday.HALLOWEEN, "ver-sinhaza"),
		phrase("108faa160ad49baed951e32a4e9cf661ccc558dc25f2179f976fceff0d9b5be9", Holiday.HALLOWEEN, "ver-sinhaza"),
		phrase("fdee7f7b634dae21861cd67042a9e7aa7e58b547534816d50f59bd0e977bb03f", Holiday.HALLOWEEN, "ver-sinhaza"),
		phrase("807c723e4c1cbe6957e85d63641bb0870064e6620787d1275fb89e1dacb22532", Holiday.HALLOWEEN, "ver-sinhaza"),
		phrase("a7ca0c85a8d3299b12fe3b391bfc7bb0b6e94a8869fc1b161901e3ac6805b386", null, "edgeville-pvp", "lumbridge-pvp", "falador-pvp", "camelot-pvp"),
		phrase("ac1e20f4292e3521b488a043a19a41abab1b4f0616f0f0caa071d884fca0620f", null, "cooking-guild"),
		phrase("9700358e71afdcb12f1d7894ae938876d9ceb3e203000b722042a2f0b72596b9", null, "crafting-guild"),
		phrase("b12fe0542d798d94fd216e09c77962403ed775955778ca200915d324ad8d8a0f", null, "mining-guild"),
		phrase("04096a52bc3e60f4a97c53295be34a5d2003e24fe4d67d89870cee203d0e89a9", null, "camdozaal"),
		phrase("5f6f5407b5602a7394b44804d349f918762c06b353efe05d23bea6ec30dc3154", null, "legends-guild"),
		phrase("f5cfd45a83a17e90365b492b283875e7ffa4654f2b80168a0987d56c8e3290cd", null, "myths-guild"),
		phrase("6540fad1f0221267e9b8228943f754d5ddb4799795cccd8f541740d465898665", null, "ourania"),
		phrase("a0d534abbd5a8366a5fea79b1e8475b39a32d447ab81341733085593efed9ce5", null, "jatizso"),
		phrase("64a3df551509c0c3c23bd6a50c303825ba3b83b3b391d543f39c938af90f43c7", null, "etceteria"),
		phrase("888fe0ed60ccfc0fe076de01c47e16ac3d3606190218f149ef69017b83cfe1cd", null, "mage-arena"),
		phrase("a6a6c5cd063b99dedebc7fa8e8bf7d09ea05411ff00b36e2e17178e561fb2468", null, "daimons-crater"),
		phrase("14ede58ff54cf0ae1c2e2304b25b9489b5fbcde4e4f0e3fe4f0424bee517da2c", null, "burgh-de-rott"),
		phrase("145233fda6e670fb624552dc50f6dace85e790a0e758c8d57273f82a43016577", null, "tarns-lair"),
		phrase("9aa57db0897b54d9659b986eb268b8a5fa467708bb4382d656d0e0ecadecf263", null, "sophanem"),
		phrase("865a8ebef74f76b6c8a8d86681174ce697e5c063bd32a9c1505ffcfdcf34f06c", null, "nardah"),
		phrase("32660a0c4bdd5e4896e5a9a8ecd8ac0a063166bf2c1680f228f84f2f11a0b2fc", null, "mage-training-arena"),
		phrase("06d4fcd1deec8cd60a502e7804060ddfab190f154928336da7a1d82c99f1742c", null, "giants-foundry"),
		phrase("d4f55be99d52bed1312c2b5f42f4202a647e5701b027a444fe47ba6ca6a33cbc", null, "culinaromancers-chest"),
		phrase("61e8b92eb3cfd7e87a75056d3563d58ae3d82c998c8a5fac390865a5b7b5bff1", null, "shilo"),
		phrase("ee5cdedf8e0280eb7e0aad282e6158946d251fb15cbdd24a741398f3a4735f4d", null, "lletya"),
		phrase("5c26e1f12a29f4611c4b7b16e1818a3990ad86dfdc8848e8b0552712c8b4605a", null, "gwenith"),
		phrase("d2b9b8602d4e6f4ed4701c902526c3ddd2003a27168321d714e507d725ec9ed4", null, "lovakengj"),
		phrase("d93fc7590f8457bab5fa8606b8a86aab676c5d72c6c24bb6cb3ab0c5aae69458", null, "lovakengj-mine", "blast-mine"),
		phrase("b97bf6ebcf594e98c9ccfd67e2303bce3253dda31e55e8b354fc11bf9b5d4045", null, "sulphur-mine"),
		phrase("2273030871d7a23ed0fdeb46b0cc8e967d9bd6a4af79fd66f05e7e275f2ccd50", null, "saltpetre"),
		phrase("dc9d6be3e838c632f81e03c22ee259b11f6ba538baa30ba9f5c3b87027290d52", null, "charcoal-camp"),
		phrase("af48a91683334baccebc46a82c369632886d0d2458109ffb424431afcd950563", null, "shayzien", "shayzien-encampment"),
		phrase("066ebc89b5eef2f32b6f9b8f2361a8f9880930eebc6128103e4d0701070b4f21", null, "lands-end"),
		phrase("73509a1e1d43fb2d7a67ae136d4d38f34d4eed2c892ed48f3b32a094e3846886", null, "kourend-castle"),
		phrase("6bbdafe703a2ef7a37e708b9de83cf6607243a341460bfc8b6a89bc007008310", null, "hosidius-kitchen"),
		phrase("0fce27f3fb7ad2ff8666b75054974f06da0941b11b6a5158df2e6e6a82ddf0d9", null, "vinery"),
		phrase("82aca3c009a82e4657cd62a380314629368e2d3c5232fe9f2039e01d80f5decc", null, "ent-dungeon"),
		phrase("57a636d09857b5bd98b1d81678cc2c7c93cbd8052b200b5229bc38f7533f3444", null, "alchemical-society"),
		phrase("b8d6a2abdba264ed9a6964f2723cd615cb153c1466d0cea9c1ab558c2a625773", null, "tal-teklan"),
		phrase("31e00ece555cec0427fe5b474dc77cec5fdaf2110ef2e44368aa7c97b8c0ab7b", null, "mistrock"),
		phrase("051179fad68ba341774a5f09898e5717dc91f101f070d7574d4a3523ea89f6e3", null, "nemus-retreat"),
		phrase("59e83692c8ba7224a38f0a72f58f207e398f6fb2b4ddf29441dde34ccecb4850", null, "auburnvale"),
		phrase("59031771565a5673da577b7e309021a3cf6a6c246e6a2f74fa9a80a4b0286c19", null, "quetzacalli-gorge"),
		phrase("8b682ddd0c3686947728bc55e774fce3edbd03dd6bd78e4713a8424ddc25d256", null, "darkfrost"),
		phrase("d9a00467e5924a9dda8671a63ea9636b8a30ccd636eb46454fc9fb7359a3a901", null, "deepfin-mine-west", "deepfin-mine-east"),
		phrase("60a128847f6d2bd649102b9ceeace82e85008ec8769187e9cf93cffd7b61d37d", null, "the-node"),
		phrase("627eca1ab9ab26a820722b56fa12456527665aaede2224f085b7e475996e0dbb", null, "isle-of-souls"),
		phrase("65ff5089b578cef6b4b5b8a5d435c8f42254fc7abb2fd8636c7c324547111320", null, "ape-atoll"),
		phrase("0869b15a206eb896070a51fc34ebbbc15d78a641e7db21529f984edf4875e8c6", null, "mos-le-harmless"),
		phrase("b7bd2f32fe8999b4082ca52556fb484f000f10b53d2b9ebf91f128e697d607de", null, "trouble-brewing"),
		phrase("962cdf87c5ee7fb25b825afcd0e0b3a8cac3c2fa64d6b37608e465a5c033117e", null, "fossil-island", "fossil-island-wreck"),
		phrase("f5a4ef62f1a3dbd13b0f50ba1bc835c80c3543ae5d00394dfc0e91839c6f5acf", null, "dorgesh-kaan"),
		phrase("a0d8bf96e3f587829bb98039445f6477d7f0f58c1445ea68ad80a67716a127b3", null, "sunbleak-island"),
		phrase("1f2f8666c06424f6354950bc5bb4639fc90d5e4cb3c7c770c2ffc8f1e0a012f1", null, "wyrmscraig"),
		phrase("01ad2799e23e0139cd5c7d514265e4baa80241eeecf29c612386b23cec808695", null, "charred-island"),
		phrase("7b64318cb555d2c3d82be5906e6b0320b88b1b02f4ff07c4dd6817e7176b7714", null, "onyx-crest"),
		phrase("3006e42f016ba1f42da1f919b42b26debd621118e66a90f5fa57b053412e55d6", null, "red-rock"),
		phrase("5229da6dfdf10d8c0331e9824a6b89ee2515fe10385ae9975678d511ace32069", null, "sangvesti"),
		phrase("f8a534afa5e4647ce36998b1258dcd642c33cfc469e021fb535e48408d82c126", null, "sangvesti"),
		phrase("e92ab2d2a0f28cdd306606f97df41aa07809975c9798735dfd0dcf4b6747a296", null, "sangvesti"),
		phrase("0c51faa004908f86963c537f0ef8fa79e21d08fc33cb9fbbe1ba2a2940875508", null, "varrock-west", "varrock-east"),
		phrase("0a6141dbd9d322e5a52fd24a308426649db8ecfa721360ab7f94868b4658ef6f", null, "port-sarim"),
		phrase("1690002816a11c516cdd108b9ca85205110f5b7b3bb7b9fcfbd3fc83622787f9", null, "warriors-guild"),
		phrase("d24799e7bd7ac8e2a14fa6d7db25db2bc311d93d697343434f3691f8c36d5c2b", null, "rogues-den"),
		phrase("303ce09c7da477d48aded08d27e6799d9c0bfeada953c118ddd82fc726d8ad10", null, "motherlode"),
		phrase("1b3b3e41aa424f229bfd5a89fde4cd7152618b02a9a955d64c23c87e7b6d3d1a", null, "yanille"),
		phrase("28550a2c6cd288322926b8427811cfd269ab830b4b54d59bedc2cb65342375c4", null, "gnome-stronghold-north", "gnome-stronghold-south"),
		phrase("081ecb406e81af5da6274ea03aec4c759ffe9f69426fd368371b1f99caf2bcb3", null, "barbarian-outpost"),
		phrase("7b6c730dc5d4e7f6c3937b48979a3d7476d20a4b3c5cff9918489d8a802b17fc", null, "ardougne-north", "ardougne-south"),
		phrase("d6dcd30d485ef091151bc0eb27733811d5e148c1d1299a3d26c6b129232e5b32", null, "lunar-isle"),
		phrase("aa01aa0931d27ee0a2f51fddd993224fb1181120b2d79de9e0d0dd5cb79907df", null, "canifis"),
		phrase("3b0bdba7017f741d165ef05989d69d25f84cdc560b3994a7e74c2e5648295927", null, "darkmeyer"),
		phrase("511216c4e643cd9b5129443999ac928498647c4ce9ea5c15bb7ef28a2848d3ee", null, "zanaris"),
		phrase("48d3e470ab5f0fd293fb07b8cb50bb6c65637924cbb7485e4deb2008c52e5e35", null, "woodcutting-guild"),
		phrase("78100a858327b966668aa87fcb575f448c35bce458564cba5190b3b915647d3d", null, "edgeville-pvp", "lumbridge-pvp", "falador-pvp", "camelot-pvp"),
		phrase("f9083aa69cfa26da2c6a0208a79b81ddfb95d84bd27cde842edcc1c1d8999c6e", null, "edgeville-pvp", "lumbridge-pvp", "falador-pvp", "camelot-pvp"),
		phrase("4e34598b370857254c0c299c515d36e6fcb4ed22e9d027aeded874f294f73ef8", null, "cooking-guild"),
		phrase("13d52917986157eab4cc63b879e2a2c0e8391f6cfd6fb0558d4dd56650afe271", null, "cooking-guild"),
		phrase("768701a225551cdb94f0fc1cbb8ed5ab53055c926ed14514ebf6b33ed367f164", null, "crafting-guild"),
		phrase("e879da0ec6e6acf81632264f14e3ffcca06867dedc6f3111c137b59aaa749e6c", null, "crafting-guild"),
		phrase("bf625e59e7df7f242bdc6c16ab1c02d91c4eb174aeec0d85d01a30f4b6e5dd8f", null, "mining-guild"),
		phrase("2a176ff2ba81dd1225f3a27ac9b2028b7c91192b9023c894503eb94822a67496", null, "mining-guild"),
		phrase("9232afdfac82164d53a4ea106fe18a06a7bf5459bacaeecbf1d0589c4475bb2e", null, "camdozaal"),
		phrase("9adb027c1a90069635296621eda3e6d9b8bc6112887d2055fdedd09006da80d6", null, "camdozaal"),
		phrase("259e15878ce6bf52b971f09285ba9bae29bbeaf2fea73d3c8abdf4085e387f5c", null, "fishing-guild"),
		phrase("30ea05ecd538b7f319a9831a370e8a9fec0b5193d3ad44ec16f5fe0de8261f8f", null, "fishing-guild"),
		phrase("3293ff15e332bb4af99793dd6e301fd36c02dbf9f231845e881280cac79c24c7", null, "legends-guild"),
		phrase("680338097ec9e98b9067c403d43f2e98b50b5bb35e22170e9120532dc02e4371", null, "legends-guild"),
		phrase("64743c477ef5c0568ec0bdeb3c42177005c378ae11135354dab94eef93b9ca0f", null, "piscatoris"),
		phrase("09c3d2653efeca2ac228fb70834ea000c91554693d54fc32a5dfa6a6730a218c", null, "piscatoris"),
		phrase("da647af888fb36f0993261059d6a4a49634e8aabd05fd9129b8654b90471db99", null, "myths-guild"),
		phrase("51c618ccf2443d6292b1d4588823f7396e1706cd8d2ba856a540a14d4b8953f9", null, "myths-guild"),
		phrase("1b831aaa39ac02ce31a284a0c936307612ea8fc21b528ce31686b98c96b0b103", null, "ourania"),
		phrase("4e59a99085370d2d33d3bcf53485938c21b6eb43e3232f9277336d0073829353", null, "ourania"),
		phrase("a35b03447a90da1e06c908b9b6ba7a51212b77531536f3dd5a485fab2de7d5da", null, "neitiznot"),
		phrase("3d2057952e68fc443d0754beeb1b83640927dae93a6a82e6751f258ad4ef61fd", null, "neitiznot"),
		phrase("5f00246314ba1df56f20dc45acdd1318020d7db90a214ee3f69b9a15fd8004f1", null, "jatizso"),
		phrase("ec889c7cba151dc6287924b947cad76b42b65d223e2bd2f74def714d76416533", null, "jatizso"),
		phrase("fbc8feec7f500d0ef533e22814a39e83e34daa7b5622b35067a7d53b43792f9c", null, "etceteria"),
		phrase("056c0cd66fefc18466efcbe3250bc54ee6e2de11624f77225700e5a0cb613a66", null, "etceteria"),
		phrase("b7d84443b31f04f79f927a531dc0960caa72557d6f15e81e52874008bfe52b4b", null, "blast-furnace"),
		phrase("2047413cd2386c6865d53f38dbe3a713bbc1741c56f4158dc7af6b7a3fb763b1", null, "blast-furnace"),
		phrase("94887a778bb9813a1cc65fa4c90c2d4527f5f2234aeeda2bbe319634b501ccfb", null, "mage-arena"),
		phrase("73e92e6a175161db9d87669709846cceb0d88917b4fa43b3e583b5fa7e69c3a0", null, "mage-arena"),
		phrase("5b377b598b406235fdef9058237445174001f28f8f5a4313e55159cf1ff0affd", null, "daimons-crater"),
		phrase("28d223c05de6ef3f9d2b75b2d087c00a35057f8fa597f54bfc86b176119477d3", null, "daimons-crater"),
		phrase("944f7d8fb3ab6c279f466f38488f6e299e7a9e04e87e498a1c200710ad2d805f", null, "burgh-de-rott"),
		phrase("ba5949463d66c2d346a886536d626c50f10d101da2eb2041a74ed37c33a4a595", null, "burgh-de-rott"),
		phrase("649da92f6bde9ac4ec890c67b4fe5fd3cb7a10fd6ba84be2a26c802b0cd65019", null, "tarns-lair"),
		phrase("f56894d0e1bb23cc46a59c068373dfc8cc95f341f3360665c35fbb7024fbc2b7", null, "tarns-lair"),
		phrase("d05cbb8a753a5582636e25431fb426192105c626b9ade4db57f45d6cdf3ab090", null, "hallowed-sepulchre"),
		phrase("9e07e28c41ff319b84f06f1ef3e1018d6805ccb561b7a5ea051b8dce1abff75a", null, "hallowed-sepulchre"),
		phrase("dc9d8b3b67ac01553f68d2d371edc50b1063ceb54aa777d6205e3d7fe1361076", null, "guardians-of-the-rift"),
		phrase("c137aa58f89495139615b866294cad55c15396fa9dffcc718a13247929234727", null, "guardians-of-the-rift"),
		phrase("25f264f160733870a91569c818472438344ccaff9cf49304a5ce924573d1072d", null, "shantay-pass"),
		phrase("1322a88268b83ddcfc2e3205d642abec4a5565a8a66f8f702ec0f55ec445d81e", null, "shantay-pass"),
		phrase("f4611510927089f89600af6eec9089b32100397a929d3a87763304ed12120824", null, "sophanem"),
		phrase("69fb166c3d393638e8d4421c5fbf736ad7c89a7dbddfc9f83bcff41c0144d9d5", null, "sophanem"),
		phrase("420a69dfaf1749f9868da3901816211af15c1fa903500d690e66a9f83eac5add", null, "nardah"),
		phrase("210038874c23b4d402938d80ec4047045b1da1c30c67f3c44f20a7e3c121d77e", null, "nardah"),
		phrase("01828304a03ae351910fd6ac12bb0bfd09c7ab0607698b70a116af016ead2835", null, "unkah"),
		phrase("8c020e60f8e4e8dcf1916eadb35cebddf1011db56776e65bf7458c294f0f0e9f", null, "unkah"),
		phrase("4a470d5c8072f9976a5aae2b98d7b88ae851915d11db0ae5e44b4008a24f000d", null, "emirs-arena"),
		phrase("eb8e6ff290c37bfaefb8616c392bb946c24db00a1d6529cc74732c05c91723fa", null, "emirs-arena"),
		phrase("3d91559872377c7f14f792e16a5d4b75a2cfc9c8897073258c735bf62183d070", null, "mage-training-arena"),
		phrase("077df3d9de40ddd0c1480faa23d04f62e09ac9a62284054a52ff1fdae8b48bc4", null, "mage-training-arena"),
		phrase("e6b8bf717b884b89ff997173bf3ced27c12188c8abf4f19b03cb6fefb4d42661", null, "giants-foundry"),
		phrase("a48f02665d299eea17cca66f6d6e09779bcbad32731cbbca9e65c763955d6748", null, "giants-foundry"),
		phrase("b44a99bf1be7803833636ee740956a0470e14f216ea46e9ec98299c190497de1", null, "culinaromancers-chest"),
		phrase("ba322d83e7df5d4e55dd95be1e2f175dd0d3df929f29825b2d08b2fe56d08328", null, "culinaromancers-chest"),
		phrase("49dafa3f1aa06eb20cf05cd8aa40b555a40894943ef64daf27d63a84a03709ff", null, "shilo"),
		phrase("b5a503d396d2cccf9760a9acea6664da367f6b876460dca863aa4a7067bbac14", null, "shilo"),
		phrase("f4b631c9702025ebb01f693981e178b7ea11e303d37f03ed8b62db7c9c1661e4", null, "tzhaar"),
		phrase("c656f3105517886a9ce8d3bae7165e1ee50d1f5fb004715a9c658460b9b381cc", null, "tzhaar"),
		phrase("4717002e95881423a03fc0a1f70217ac9d5dd6b418d174ab618e42e834d92c47", null, "lletya"),
		phrase("2c5da8eb23d7f05cb62f0922bc8edd8175b2e52ba2d264781fd4a62b8322aa96", null, "lletya"),
		phrase("e66486c2053f275da1cddc1f031c3c397ed6703f02dfb112fdd581073fd80844", null, "prifddinas-north", "prifddinas-south"),
		phrase("1fe9d2c54dba25682e727ee65e62af902218949a30e5cc1bf2e036441cb218a2", null, "prifddinas-north", "prifddinas-south"),
		phrase("3cc4cd68449fbcb367c4310e43fd6cabead4940c4517c21fede484cf56e68de0", null, "gauntlet"),
		phrase("3307591b2dac24718ed36ec6f7b26e73bc708a0fe35a41333a7d4e5c2510d914", null, "gauntlet"),
		phrase("a40436024035d0042aaf7e6147a4cda3812aa6cd144a60e39b0b282680a72259", null, "gwenith"),
		phrase("02d70fba71ba0a083d6bbb85510f42910f2459c8fae9579a37fe07d03b1f2ff6", null, "gwenith"),
		phrase("42b7191abe476826190af51f5e055abc4a3a2a007b1dbf6190c6c7caa60d8e17", null, "zul-andra"),
		phrase("7f101bcc8e70a0f1856770095d395ae2169d1ebfba44385996947ba2eb649b98", null, "zul-andra"),
		phrase("84b4b8336c6f27f20175d6632876a764ef44d5c7b621da58025b1003f0fb0e49", null, "farming-guild", "farming-guild-upper"),
		phrase("b2f45ca89e3e6ae3f6aeab467f69d8bf22d37ef1bc10b886046f7c4a6353337a", null, "farming-guild", "farming-guild-upper"),
		phrase("fff592a858e9d172e2c5dfe017c7095d847857c20d210f6815abd62c9fbe22a6", null, "lovakengj"),
		phrase("7c433f8d04561797091a726735badeaf4eb8d0d2fb8f6e4b5eafe7a1ce81e994", null, "lovakengj"),
		phrase("37cf0a13395e0745da39533bea9dc1cd4d0dc810c058e51d0f677aa4eba4e68a", null, "lovakengj-mine", "blast-mine"),
		phrase("94c6962c5124fa281d2cf606834df6fafab4f9639ff47d36fe70517d3b3a0c3f", null, "lovakengj-mine", "blast-mine"),
		phrase("7fcb028205e676fd15e30664ab906400aa3dff30a3aef8a99df9d58315868b14", null, "sulphur-mine"),
		phrase("7453149b4169c588a81b7338edaef89cb8d9f668790a266d07a33e3de51528b1", null, "sulphur-mine"),
		phrase("6270b7a8e088ac76b0804b120c08ab1bc71a13aff3237804bd3fa89415ad9842", null, "shayzien", "shayzien-encampment"),
		phrase("d3817c3dcefee102e82867398997161f9f665f0e7a566672c8dbaeacf2fb77d8", null, "shayzien", "shayzien-encampment"),
		phrase("1b6c0cb150d1c18a4e2d8c58ac95eeec5dfe792094a395afc4143a05002b648b", null, "lands-end"),
		phrase("80dbfaff1b619a9e472d15489632f97d13aa2e9b9b3bbbdc461f20989822c87c", null, "lands-end"),
		phrase("beb16c78ca42674403981034c16e3f83818356b3fcb9cc448e208832fa257401", null, "ent-dungeon"),
		phrase("4676a3d082fd73d777047f32b0ce79853d0fbc077b7d0fe611bd3bb026e302c3", null, "ent-dungeon"),
		phrase("3c2ffa2930daeda77470d694e984613d4c2f24c8f82b470f974a709279e9faca", null, "kourend-castle"),
		phrase("84113ed9451e17b33812339b5c07878cf59b714ec2119deead26400845a682bb", null, "kourend-castle"),
		phrase("2d5e9cecba10257edf088e91f05eefc2f94eceab0483082670fdc6e498e3b862", null, "arceuus"),
		phrase("d0439fdf6e9cde03c7b953f11c94fc254dca7a72d5fc0f8d5a93216fc251fbd7", null, "arceuus"),
		phrase("be24eec6c31cd03624984bdd7b0add91126d5bf08933a48b10a7266ef77b0959", null, "wintertodt"),
		phrase("a151a9ab690568b51c88fda16130daa765bb38312f40e8189f63139f1ac867d6", null, "wintertodt"),
		phrase("0879edcf91ec2f0d3aa5b5357d9eb6281f622dfbda18029dc159047a1e418e07", null, "hosidius"),
		phrase("55e90f715e0c64b3355050bbc654c86a3fca81568dbe59b9016205b254dbe6ea", null, "hosidius"),
		phrase("b71d13bb14ef8408282488df6d5348b3ed8672671e24a4a412e5239b8f792a06", null, "hosidius-kitchen"),
		phrase("f5aa3452d02c269cd2ba52311a55b6131d53feaa4d8433df3ceac7e190f5add1", null, "hosidius-kitchen"),
		phrase("c4b32eb34b607e77aaac1bd819f3e188a487b428b1d884931c0ef687617e3ef4", null, "saltpetre"),
		phrase("050a4265d92d2ba146b09dc9aab514b8882bb5709555049759acfe3a74e54ec5", null, "saltpetre"),
		phrase("1d3420bdcbfcdfbec4bbd0e8b958fbde91fa83c597397bb2f8f0229ddc100b77", null, "charcoal-camp"),
		phrase("857e40214b673fee92b4714030f056110c742b9f27913e074d57cf4e229e04b4", null, "charcoal-camp"),
		phrase("912d4dafb5b0e49f9c6943801d30260b0174bad493d8185996665a32930378fa", null, "vinery"),
		phrase("b93849d77d11a09e88f7efd26da364f8ab7fb2787f48383dab43cd52e0d876f8", null, "vinery"),
		phrase("b417d3490d466cad23a3be1866e61934ab3625945151100194f985f4222eaa28", null, "tal-teklan"),
		phrase("32efc6a70d1389dafa97d989bdba040d1655aff6c87b3b5ad84ea43667542359", null, "tal-teklan"),
		phrase("73809205b514ba0fb3e23e1f18bf6b3885e4d0ed94bc9b62ebd3c23e657a4dea", null, "mistrock"),
		phrase("bebf743a1f8d13f52d43ed371c6e3f9374624d2941414b5c7139c49ab8b3a9ba", null, "mistrock"),
		phrase("97c15af5b95a09786cc8a2fc1b660908f4459edce3d89a15c20d5b503dc784e5", null, "nemus-retreat"),
		phrase("a5dacfb441c677475275d4b0c2b92d5e5663c56764258a31e2dc3e4289ea7a16", null, "nemus-retreat"),
		phrase("7f0bb162572edafce7dea75d998ac3807ceccf8eaa487e1e528b34273fa8a0d0", null, "aldarin"),
		phrase("1ad02ab7d4d62c1a0a7dd1102c7a8c45aeedd763d849ef67c4fdf3abbb366611", null, "aldarin"),
		phrase("676f9678f41dc0a5e70658027d579932c02d6ed5df8c931d5aaef9276783cf47", null, "alchemical-society"),
		phrase("9ebbf716d4fce5aa020e726fa961e83cedbf4be759faf78eb5a31ff18309ad8b", null, "alchemical-society"),
		phrase("14e195eabe9f40aa2fd49d10cbc43b2239a99b73aa82b00c0a8a6eaa255746d5", null, "auburnvale"),
		phrase("ecbb012dda9423f5d2899d95c73191ff5fca5ea190406922c3a4802483b8f4a3", null, "auburnvale"),
		phrase("fcfa8271259c04eef4ce9109082ccb8efeef2d259ce7ccf996644b6672922644", null, "cam-torum"),
		phrase("761f90b8843ab412cd69d221430be0e48b8a4d7c519e6cce7bab1b817208f626", null, "cam-torum"),
		phrase("c0e5e9088d85d8f820331b6743effcdaaa42df5d243b107cc59ba3b3b210d095", null, "quetzacalli-gorge"),
		phrase("96bd7b03367d87ca0119f24b9f3b7f486fabb84fe28b007a6e698b9c8d9f6e2d", null, "quetzacalli-gorge"),
		phrase("2d21bc17c58cb3212f75018109955477298f92f82afeffbec7709a617e20566d", null, "darkfrost"),
		phrase("10e832b769b0761ec0e78a2ff2a37f4c9bec5b43cea411d135bc1a7b364e2e08", null, "darkfrost"),
		phrase("6c17e9faf0d1f7f685e3fe31671fa51a4ccca20c7abb96cf50c19ac3f1a9e02e", null, "hunters-guild"),
		phrase("6cc676b72a6978c3be8521080027ca6996bae4dd84c986c9d9579734228e0e94", null, "hunters-guild"),
		phrase("16960d26c7009e446bd6fc7e9eddaf04ce0bd949d803794b8d8aa345157ccb16", null, "colosseum"),
		phrase("3fd6d47a54053f1087f193ac6e684e93fc55833858b51c9ab09e79a7b369747d", null, "colosseum"),
		phrase("b751e5c16e0981e53f368f303d6008e6b311aa3278b9881909f660f90fc381e1", null, "isle-of-souls"),
		phrase("1e3f262fff63dc4258211ed1ff115308df0c5deba04bc78d712881dddf5bdbdc", null, "isle-of-souls"),
		phrase("8618b20afff3ebf5b952576c5f91dc03514bea16f963b6d5e02ffef9eaa8549c", null, "deepfin-mine-west", "deepfin-mine-east"),
		phrase("8fcd02cfa19ec3831a0b2ca9d6a54378b3e8e0d42242b2913b3567f1eae9581a", null, "deepfin-mine-west", "deepfin-mine-east"),
		phrase("ad2c76ffc38a8ddd1449f5b9f057bb0fc4b1b907491fd75e3d02b076bb4bda55", null, "the-node"),
		phrase("c8219354ac8219db783e7b09d691c16dccfd12006360ce932ec4f8d0b46a314d", null, "the-node"),
		phrase("0c4df861430e1ad37214dff6db9575cc19fa1fdec91f85abe1cfd7b8b6f0f9aa", null, "ape-atoll"),
		phrase("2556edc17328c794601e89cf200ab395708bdfd38f8ddf7131409832f8c29077", null, "ape-atoll"),
		phrase("3971ec928f66a294fbe5e8881dcd6b1e007ec90f90ed51de0eeb65b108a1e6ae", null, "mos-le-harmless"),
		phrase("67ceb0408f9b5eb8d14d7ae121e79ccda511dd06280970d1b3ec966010e8c4e2", null, "mos-le-harmless"),
		phrase("b5c93f1e8d6f24970f31367554b489b73d7ddb971120bf53a3fd13537f6774e1", null, "trouble-brewing"),
		phrase("e994174e3ba1c681e0f7c53dd3f66babcfdbd4f19c7e977bb6799d84a7e1eb59", null, "trouble-brewing"),
		phrase("dcddc0b4715a391d32024b2e3813b225d77ca9c332fd30519d3d108558ad1be5", null, "fossil-island", "fossil-island-wreck"),
		phrase("cff023a21db7c38a2e2e0f0c2e483158423cc36ca92e7de81ddbb7a230c4fe52", null, "fossil-island", "fossil-island-wreck"),
		phrase("0f690a66e4ef297680b799ddff1cfa7a09c8a8d67e70df7a4fe7604756c01bb4", null, "volcanic-mine"),
		phrase("579625951bbaa58d118564ea3c87114fd1b6f618508264b7f7cdb3be1bde1daf", null, "volcanic-mine"),
		phrase("f2c5e4a4f41c225a60fbebc501b66e81a8fb81b7e51e6f4f60e63411e2d17ddd", null, "clan-hall"),
		phrase("cfff0cd6e2db8dbc6e6e72aeb54b3eb4f5b75a8f62b1317a94f2a1c1633de9b1", null, "clan-hall"),
		phrase("ff8664d6d688eafe05d20b4e4db25a0062b100a7cdbf75e69d1ad32fa05be6d6", null, "dorgesh-kaan"),
		phrase("a5ef95cce991da3edcb86c6766eaf09af5d41f395dad895ed0daa790a1f5c175", null, "dorgesh-kaan"),
		phrase("b3af40609f83e4b660eac5c046b99a4073512b61afb951ce04d017a50218b19e", Holiday.NEW_YEAR),
		phrase("84b8d1acda8deec0f640f4fac69f1b564386f1bc93098dea9b3d05e1bc6fae26", Holiday.BIRTHDAY),
		phrase("85b97fb3beb5929dc7330a9cce82165e7b57e4aadd3ec4d9693c165c316a863a", Holiday.EASTER),
		phrase("f827388dff718b39d5f7ecfeee97b387077631f917bae9e68ea0e835b839cb38", null, "edgeville").onSpellbook(Spellbook.ANCIENT),
		phrase("f7ff38a86f0e7de60d3972cf10d006f29760becab5d3254c168fa0d9c1a0676e", null).onSpellbook(Spellbook.LUNAR)
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

	/**
	 * The phrases in this group you've found, oldest first. Ones found before the journal
	 * kept notes have no text, since only their hashes are known.
	 */
	public List<Noted> found(Progress p, Group group)
	{
		List<Noted> found = new ArrayList<>();
		for (Phrase phrase : phrases)
		{
			Long paid = p.keywordPaid.get(phrase.hash);
			if (phrase.group() != group || paid == null)
			{
				continue;
			}
			Noted noted = p.phraseNotes.get(phrase.hash);
			if (noted == null)
			{
				noted = new Noted(null, null, paid);
				noted.read = true;
			}
			found.add(noted);
		}
		found.sort(Comparator.comparingLong(noted -> noted.at));
		return found;
	}

	/** Whether any found phrase in this group is still unread in the journal. */
	public boolean unread(Progress p, Group group)
	{
		for (Noted noted : found(p, group))
		{
			if (!noted.read)
			{
				return true;
			}
		}
		return false;
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
		return claim(p, message, nowMillis, today, bankKey, worn, null);
	}

	/** As above, with the name of your clan, or null if you aren't in one. */
	public Kind claim(Progress p, String message, long nowMillis, LocalDate today, String bankKey, Set<Integer> worn,
		String clanName)
	{
		return claim(p, message, nowMillis, today, bankKey, worn, clanName, null);
	}

	/** As above, with the spellbook you're on, or null if unknown. */
	public Kind claim(Progress p, String message, long nowMillis, LocalDate today, String bankKey, Set<Integer> worn,
		String clanName, Spellbook book)
	{
		// Typed like a person would, "why fletch?" counts too: trailing punctuation is dropped
		// when the message as typed doesn't match.
		String typed = normalise(message);
		String said = typed;
		Phrase match = match(typed, clanName, bankKey, today, worn, book);
		if (match == null)
		{
			said = TRAILING_PUNCTUATION.matcher(typed).replaceAll("");
			match = said.equals(typed) ? null : match(said, clanName, bankKey, today, worn, book);
		}
		if (match == null)
		{
			return null;
		}
		String hash = match.hash;

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
		p.phraseNotes.putIfAbsent(hash, new Noted(said, bankKey, nowMillis));
		return match.kind();
	}

	/** The phrase this whole message is, if it works here and now; otherwise null. */
	private Phrase match(String said, String clanName, String bankKey, LocalDate today, Set<Integer> worn, Spellbook book)
	{
		String hash = hash(said);
		String clanHash = null;
		String clan = clanName == null ? "" : normalise(clanName);
		if (!clan.isEmpty() && said.endsWith(" " + clan))
		{
			clanHash = hash(said.substring(0, said.length() - clan.length() - 1).trim());
		}
		for (Phrase phrase : phrases)
		{
			String compared = phrase.beforeClanName ? clanHash : hash;
			if (phrase.hash.equals(compared) && phrase.works(bankKey, today, worn, book))
			{
				return phrase;
			}
		}
		return null;
	}

	static String normalise(String message)
	{
		return message.replace('\u00A0', ' ').trim().replaceAll("\\s+", " ").toLowerCase(Locale.ENGLISH);
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
