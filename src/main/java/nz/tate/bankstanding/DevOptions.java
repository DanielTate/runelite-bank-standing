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
 * {@code ./gradlew run} (see build.gradle). Only read when RuneLite runs in developer mode;
 * for players every option is off.
 */
@Singleton
class DevOptions
{
	/** {@code -PxpMultiplier=N}: every drop is worth N times as much. */
	final int xpMultiplier;
	/** {@code -PsetXp=N}: the saved XP is set to N on the first login. */
	final Integer setXp;
	/** {@code -PresetProgress}: the save is wiped on the first login. */
	final boolean resetProgress;
	/** {@code -PrareEvery=N}: every Nth drop shows the special splat. */
	final int rareEvery;
	/** {@code -PcelebrateEvery=N}: the 99 celebration replays every N seconds. */
	final int celebrateEvery;

	@Inject
	DevOptions(@Named("developerMode") boolean developerMode)
	{
		xpMultiplier = developerMode ? Math.max(1, Integer.getInteger("bankstanding.xpMultiplier", 1)) : 1;
		setXp = developerMode ? Integer.getInteger("bankstanding.setXp") : null;
		resetProgress = developerMode && Boolean.getBoolean("bankstanding.resetProgress");
		rareEvery = developerMode ? Integer.getInteger("bankstanding.rareEvery", 0) : 0;
		celebrateEvery = developerMode ? Integer.getInteger("bankstanding.celebrateEvery", 0) : 0;
	}
}
