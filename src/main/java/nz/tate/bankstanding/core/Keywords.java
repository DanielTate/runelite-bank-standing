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
import java.util.Locale;
import java.util.Set;

/**
 * Secret phrases that pay out a bonus roll when said in public chat during a stand.
 *
 * <p>Only hashes are stored, so reading the source doesn't give the phrases away. A phrase
 * must be the whole message. Each pays out at most once an hour, and all of them together
 * at most {@link #DAILY_CAP} times per UTC day. Over a limit, nothing happens at all.
 */
public final class Keywords
{
	static final long COOLDOWN_MILLIS = 60 * 60 * 1000L;
	static final int DAILY_CAP = 10;
	private static final String SALT = "bankstanding:";

	public static final Set<String> HASHES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
		"f1a40f3bea462426df466ec436c4b69acb5f96dc774818a5f748e86d31f05c36",
		"8582cc1f392d6aaeac0e013312daf544b8c319c2c41255662d58cda0a58ba2dc",
		"7433c9bc284c7761c8d6e4d6903f3674fae7f64028627e5c38a5f9ca2007cbfe")));

	private final Set<String> hashes;

	public Keywords(Set<String> hashes)
	{
		this.hashes = hashes;
	}

	/** True if this message is a keyword that may pay out now; records the payout. */
	public boolean claim(Progress p, String message, long nowMillis, LocalDate today)
	{
		String hash = hash(normalise(message));
		if (!hashes.contains(hash))
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
