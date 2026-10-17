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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import nz.tate.bankstanding.core.Progress;

/**
 * Saves progress per OSRS account, in RuneLite's profile config, which RuneLite syncs between
 * computers. Saves go to the account the progress was loaded from, so a save made while logging
 * out can't land on another account.
 *
 * <p>RuneLite only pulls in other computers' settings when it starts, so two RuneLites open at
 * once would each save over the other's progress. To keep that from losing anything for good,
 * every RuneLite also saves its own copy under a key of its own, and loading picks whichever
 * copy has the most XP. The main key is still written, for older versions of the plugin.
 */
@Slf4j
@Singleton
class ProgressStore
{
	private static final String KEY = "progress";
	private static final String COPY = KEY + ".";

	private final ConfigManager configManager;
	private final Gson gson;
	/** This RuneLite's own copy, new each time it starts. */
	private final String copyKey = COPY + UUID.randomUUID().toString().substring(0, 8);
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

		List<Progress> saves = new ArrayList<>();
		add(saves, KEY, configManager.getConfiguration(BankStandingConfig.GROUP, profile(), KEY));
		List<String> copies = copies();
		for (String copy : copies)
		{
			add(saves, copy, configManager.getConfiguration(BankStandingConfig.GROUP, profile(), copy));
		}
		// Saves from before the first release used RuneLite's own per-account keys.
		add(saves, "the first save", configManager.getRSProfileConfiguration(BankStandingConfig.GROUP, KEY));

		Progress best = Progress.best(saves);
		if (best == null)
		{
			return new Progress();
		}
		if (saves.size() > 1)
		{
			log.info("Found {} Bank Standing saves for this account; using the one with {} xp", saves.size(), best.xp);
		}
		// Keep the best one, then tidy away the other copies: any RuneLite still open writes
		// its copy again on its next save.
		save(best);
		for (String copy : copies)
		{
			configManager.unsetConfiguration(BankStandingConfig.GROUP, profile(), copy);
		}
		return best;
	}

	private void add(List<Progress> saves, String key, String json)
	{
		if (json == null)
		{
			return;
		}
		try
		{
			Progress progress = gson.fromJson(json, Progress.class);
			if (progress != null)
			{
				saves.add(progress);
			}
		}
		catch (RuntimeException e)
		{
			// Keep the unreadable save beside the others, since the next save replaces it.
			String backup = KEY + "-unreadable-" + System.currentTimeMillis();
			configManager.setConfiguration(BankStandingConfig.GROUP, profile(), backup, json);
			log.warn("Unreadable Bank Standing save ({}), skipping it; it's kept as {}", key, backup, e);
		}
	}

	/** Every RuneLite's own copy for this account, by key, apart from this one's. */
	private List<String> copies()
	{
		List<String> copies = new ArrayList<>();
		for (String found : configManager.getRSProfileConfigurationKeys(BankStandingConfig.GROUP, profile(), COPY))
		{
			// RuneLite hands the key back with part of the profile on the front.
			int at = found.lastIndexOf(COPY);
			String copy = at < 0 ? null : found.substring(at);
			if (copy != null && copy.length() > COPY.length() && copy.indexOf('.', COPY.length()) < 0 && !copy.equals(copyKey))
			{
				copies.add(copy);
			}
		}
		return copies;
	}

	void save(Progress progress)
	{
		if (profileKey != null)
		{
			String json = gson.toJson(progress);
			configManager.setConfiguration(BankStandingConfig.GROUP, profile(), KEY, json);
			configManager.setConfiguration(BankStandingConfig.GROUP, profile(), copyKey, json);
		}
	}

	private String profile()
	{
		return ConfigManager.RSPROFILE_GROUP + "." + profileKey;
	}
}
