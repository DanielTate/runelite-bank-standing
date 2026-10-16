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

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.Unlocks;

/**
 * The skill guide: click the Bank Standing tile to see what each level unlocks, by category,
 * like clicking a skill in the skills tab.
 */
@Singleton
class SkillGuideOverlay extends ParchmentWindow
{
	private static final int ROW = 40;
	private static final int ICON = 32;
	private static final Unlocks.Category[] CATEGORIES = Unlocks.Category.values();

	private final BankStandingPlugin plugin;
	private final SkillTileOverlay tile;

	@Inject
	SkillGuideOverlay(BankStandingPlugin plugin, Client client, GuideArt art, SkillTileOverlay tile)
	{
		super(client, art);
		this.plugin = plugin;
		this.tile = tile;
		for (Unlocks.Unlock unlock : Unlocks.ALL)
		{
			if (unlock.sprite > 0)
			{
				art.load(unlock.sprite);
			}
		}
	}

	@Override
	boolean available()
	{
		return plugin.getProgress() != null;
	}

	/** A left click on the skill tile opens or closes the guide. */
	@Override
	boolean claimPress(MouseEvent event)
	{
		Progress progress = plugin.getProgress();
		if (progress == null || !SwingUtilities.isLeftMouseButton(event) || !tile.shownBounds().contains(event.getPoint()))
		{
			return false;
		}
		if (isOpen())
		{
			close();
		}
		else
		{
			// Always the first tab, scrolled to the top, like the game's own skill guides.
			showTab(0);
			open();
		}
		return true;
	}

	private Unlocks.Category category()
	{
		return CATEGORIES[Math.min(tab, CATEGORIES.length - 1)];
	}

	@Override
	String title()
	{
		return "Bank Standing";
	}

	@Override
	String subtitle()
	{
		return category().title;
	}

	@Override
	String[] tabs()
	{
		String[] tabs = new String[CATEGORIES.length];
		for (int i = 0; i < tabs.length; i++)
		{
			tabs[i] = CATEGORIES[i].title;
		}
		return tabs;
	}

	@Override
	String link()
	{
		return "Collection log";
	}

	@Override
	void linkClicked()
	{
		plugin.openCollectionLog();
	}

	@Override
	int rowHeight()
	{
		return ROW;
	}

	@Override
	int rowCount()
	{
		return Unlocks.in(category()).size();
	}

	@Override
	void drawRow(Graphics2D graphics, int row, int x, int y)
	{
		List<Unlocks.Unlock> lines = Unlocks.in(category());
		if (row >= lines.size())
		{
			return;
		}
		Unlocks.Unlock unlock = lines.get(row);
		Image icon = unlock.sprite > 0 ? art.sprite(unlock.sprite) : art.itemIcon(unlock.item, unlock.gold);
		GuideArt.drawIcon(graphics, icon, x, y + (ROW - ICON) / 2, ICON);

		Progress progress = plugin.getProgress();
		graphics.setFont(FontManager.getRunescapeFont());
		graphics.setColor(progress != null && progress.level() >= unlock.level ? GuideArt.INK : GuideArt.FADED_INK);
		FontMetrics metrics = graphics.getFontMetrics();
		int baseline = y + (ROW + metrics.getAscent() - metrics.getDescent()) / 2;
		String number = String.valueOf(unlock.level);
		graphics.drawString(number, x + ICON + 30 - metrics.stringWidth(number), baseline);
		graphics.drawString(unlock.text, x + ICON + 40, baseline);
	}
}
