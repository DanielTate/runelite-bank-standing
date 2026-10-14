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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

	/**
	 * One phrase: its hash, the holiday it needs (or null), the banks it works at (or any) and
	 * an item that must be worn (or 0).
	 */
	public static final class Phrase
	{
		final String hash;
		final Holiday holiday;
		final Set<String> banks;
		int item;

		Phrase(String hash, Holiday holiday, String... banks)
		{
			this.hash = hash;
			this.holiday = holiday;
			this.banks = new HashSet<>(Arrays.asList(banks));
		}

		Phrase wearing(int itemId)
		{
			item = itemId;
			return this;
		}

		boolean works(String bankKey, LocalDate today, Set<Integer> worn)
		{
			return (holiday == null || holiday.isOn(today))
				&& (banks.isEmpty() || banks.contains(bankKey))
				&& (item == 0 || worn.contains(item));
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
		phrase("8bcda4597a0c3eb5ce3d68398e5efd6ba0947202827c02a2d55f5acec33169f0", null).wearing(20235)
	));

	private final List<Phrase> phrases;

	public Keywords(List<Phrase> phrases)
	{
		this.phrases = phrases;
	}

	static Phrase phrase(String hash, Holiday holiday, String... banks)
	{
		return new Phrase(hash, holiday, banks);
	}

	/** True if this message is a phrase that may pay out here and now; records the payout. */
	public boolean claim(Progress p, String message, long nowMillis, LocalDate today, String bankKey, Set<Integer> worn)
	{
		String hash = hash(normalise(message));
		boolean matches = false;
		for (Phrase phrase : phrases)
		{
			if (phrase.hash.equals(hash) && phrase.works(bankKey, today, worn))
			{
				matches = true;
				break;
			}
		}
		if (!matches)
		{
			return false;
		}

		Long last = p.keywordPaid.get(hash);
		if (last != null && nowMillis - last < COOLDOWN_MILLIS)
		{
			return false;
		}

		String day = today.toString();
		if (!day.equals(p.keywordDate))
		{
			p.keywordDate = day;
			p.keywordCount = 0;
		}
		if (p.keywordCount >= DAILY_CAP)
		{
			return false;
		}

		p.keywordCount++;
		p.keywordPaid.put(hash, nowMillis);
		return true;
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
