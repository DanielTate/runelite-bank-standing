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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import nz.tate.bankstanding.core.Progress;

/**
 * Saves progress per OSRS account, in RuneLite's profile config. Saves go to the account the
 * progress was loaded from, so a save made while logging out can't land on another account.
 */
@Slf4j
@Singleton
class ProgressStore
{
	private static final String KEY = "progress";

	private final ConfigManager configManager;
	private final Gson gson;
	private String profileKey;

	@Inject
	ProgressStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	/** Null until the game has told RuneLite which account is logged in. */
	Progress load()
	{
		profileKey = configManager.getRSProfileKey();
		if (profileKey == null)
		{
			return null;
		}

		String json = configManager.getConfiguration(BankStandingConfig.GROUP, profile(), KEY);
		if (json == null)
		{
			return new Progress();
		}
		try
		{
			Progress progress = gson.fromJson(json, Progress.class);
			return progress != null ? progress : new Progress();
		}
		catch (JsonParseException e)
		{
			log.warn("Unreadable Bank Standing progress, starting fresh", e);
			return new Progress();
		}
	}

	void save(Progress progress)
	{
		if (profileKey != null)
		{
			configManager.setConfiguration(BankStandingConfig.GROUP, profile(), KEY, gson.toJson(progress));
		}
	}

	private String profile()
	{
		return ConfigManager.RSPROFILE_GROUP + "." + profileKey;
	}
}
