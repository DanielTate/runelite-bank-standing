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
package nz.tate.bankstanding;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Testing shortcuts for the development client, set with Gradle properties on
 * {@code ./gradlew run} (see build.gradle) or changed from the {@link DevPanel}. Only read
 * when RuneLite runs in developer mode; for players every option is off.
 */
@Singleton
class DevOptions
{
	/** {@code -PxpMultiplier=N}: every drop is worth N times as much. */
	int xpMultiplier;
	/** {@code -PsetXp=N}: the saved XP is set to N on the first login. */
	final Integer setXp;
	/** {@code -PresetProgress}: the save is wiped on the first login. */
	final boolean resetProgress;
	/** {@code -PrareEvery=N}: every Nth drop shows the special splat. */
	final int rareEvery;
	/** {@code -PcelebrateEvery=N}: the 99 celebration replays every N seconds. */
	final int celebrateEvery;
	final boolean developerMode;
	/** Shows the 99 cape at any level (dev panel only). */
	boolean previewCape;
	/** Shows the rug, seat, fidgets and bankers as if standing, anywhere (dev panel only). */
	boolean pretendStanding;
	/** Shows every kind of modifier box at once (dev panel only). */
	boolean previewModifiers;
	/** Shows the pet as if you owned it (dev panel only). */
	boolean previewPet;
	/** The skillcape the 99 cape is built from, or 0 for the default (dev panel only). */
	int capeBase;
	/** {@code -PpreviewSplats=1,2,3}: each drop shows the next of these sprites, named in chat. */
	final int[] previewSplats;

	@Inject
	DevOptions(@Named("developerMode") boolean developerMode)
	{
		this.developerMode = developerMode;
		xpMultiplier = developerMode ? Math.max(1, Integer.getInteger("bankstanding.xpMultiplier", 1)) : 1;
		setXp = developerMode ? Integer.getInteger("bankstanding.setXp") : null;
		resetProgress = developerMode && Boolean.getBoolean("bankstanding.resetProgress");
		rareEvery = developerMode ? Integer.getInteger("bankstanding.rareEvery", 0) : 0;
		celebrateEvery = developerMode ? Integer.getInteger("bankstanding.celebrateEvery", 0) : 0;
		previewSplats = developerMode ? ids(System.getProperty("bankstanding.previewSplats", "")) : new int[0];
	}

	private static int[] ids(String list)
	{
		return java.util.Arrays.stream(list.split(","))
			.map(String::trim)
			.filter(id -> !id.isEmpty())
			.mapToInt(Integer::parseInt)
			.toArray();
	}
}
