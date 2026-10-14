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

import java.util.Random;

/**
 * Rolls one Bank Standing XP drop.
 *
 * <p>Drops run from 1 to the max hit, which rises by one every two levels (50 at 99), on a
 * geometric curve: each value is {@link #BASE_RATIO} times as likely as the one below it, so
 * small drops are the most common and a max hit comes up a couple of times an hour at 99.
 * With one roll every 6 ticks and the 5x XP table this lands 99 at about 220 days of idle
 * time, or about 180 with every bonus, which {@code BalanceTest} pins.
 *
 * <p>Modifiers come in as a {@code boost}: a fraction (capped at {@link #MAX_BOOST}) by which
 * the average roll should rise. The roller solves for the ratio that gives exactly that
 * average, so the curve tilts upward without ever producing a number a normal roll
 * couldn't. Each gear modifier also raises the max hit by {@link #HIT_PER_BONUS}, counting
 * at most {@link #MAX_BONUS_HIT} of them.
 */
public final class XpRoller
{
	public static final int ROLL_INTERVAL_TICKS = 6;
	public static final double BASE_RATIO = 0.93;
	public static final double MAX_BOOST = 0.22;
	public static final int MAX_BONUS_HIT = 3;
	public static final int HIT_PER_BONUS = 4;
	public static final int TOP_MAX_HIT = 50;

	/** Ratio used for the chat keyword bonus roll, which leans toward the high end. */
	public static final double KEYWORD_RATIO = 0.98;

	private final Random random;

	public XpRoller(Random random)
	{
		this.random = random;
	}

	/** 1 at level 1, rising by one every two levels to {@link #TOP_MAX_HIT} at 99. */
	public static int baseMaxHit(int level)
	{
		int clamped = Math.max(1, Math.min(Experience.MAX_LEVEL, level));
		return 1 + (clamped - 1) / 2;
	}

	/** {@code bonusHit} counts gear bonuses; each adds {@link #HIT_PER_BONUS}. */
	public static int maxHit(int level, int bonusHit)
	{
		return baseMaxHit(level) + Math.max(0, Math.min(MAX_BONUS_HIT, bonusHit)) * HIT_PER_BONUS;
	}

	public int roll(int level, double boost, int bonusHit)
	{
		int max = maxHit(level, bonusHit);
		double target = meanRoll(baseMaxHit(level), BASE_RATIO) * (1 + clampBoost(boost));
		return sample(max, ratioForMean(max, target));
	}

	public int keywordRoll(int level, int bonusHit)
	{
		return sample(maxHit(level, bonusHit), KEYWORD_RATIO);
	}

	int sample(int max, double ratio)
	{
		double total = 0;
		double weight = 1;
		for (int v = 1; v <= max; v++)
		{
			total += weight;
			weight *= ratio;
		}

		double pick = random.nextDouble() * total;
		weight = 1;
		for (int v = 1; v < max; v++)
		{
			if (pick < weight)
			{
				return v;
			}
			pick -= weight;
			weight *= ratio;
		}
		return max;
	}

	/** The average drop for a given max hit and ratio. */
	public static double meanRoll(int max, double ratio)
	{
		double total = 0;
		double weighted = 0;
		double weight = 1;
		for (int v = 1; v <= max; v++)
		{
			total += weight;
			weighted += v * weight;
			weight *= ratio;
		}
		return weighted / total;
	}

	/** The ratio whose average roll over 1..max is closest to {@code target}. */
	static double ratioForMean(int max, double target)
	{
		double lo = 0;
		double hi = 1;
		for (int i = 0; i < 40; i++)
		{
			double mid = (lo + hi) / 2;
			if (meanRoll(max, mid) < target)
			{
				lo = mid;
			}
			else
			{
				hi = mid;
			}
		}
		return (lo + hi) / 2;
	}

	private static double clampBoost(double boost)
	{
		return Math.max(0, Math.min(MAX_BOOST, boost));
	}
}
