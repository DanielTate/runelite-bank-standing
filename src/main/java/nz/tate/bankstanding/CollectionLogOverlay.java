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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import nz.tate.bankstanding.core.BankArea;
import nz.tate.bankstanding.core.BankNames;
import nz.tate.bankstanding.core.CollectionLog;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.StatsView;

/**
 * The Bank Standing collection log: the rare drops you've had, and every named bank, greyed
 * out until you've stood there. Opened from the side panel or the skill tile's menu.
 */
@Singleton
class CollectionLogOverlay extends ParchmentWindow
{
	static final int DROPS = 0;
	static final int BANKS = 1;
	private static final String[] TABS = {"Drops", "Banks"};
	private static final int SLOT = 32;
	private static final int SLOT_GAP = 10;
	private static final int SLOTS_PER_ROW = (LIST_WIDTH + SLOT_GAP) / (SLOT + SLOT_GAP);
	private static final int BANK_ROW = 16;
	private static final Color COUNT = new Color(255, 255, 0);

	private final BankStandingPlugin plugin;
	private final TooltipManager tooltipManager;
	private final List<BankArea> banks = new ArrayList<>(BankNames.all());

	@Inject
	CollectionLogOverlay(BankStandingPlugin plugin, Client client, GuideArt art, TooltipManager tooltipManager)
	{
		super(client, art);
		this.plugin = plugin;
		this.tooltipManager = tooltipManager;
		banks.sort(Comparator.comparing(BankArea::getName));
	}

	@Override
	boolean available()
	{
		return plugin.getProgress() != null;
	}

	@Override
	String title()
	{
		return "Collection Log";
	}

	@Override
	String subtitle()
	{
		Progress progress = plugin.getProgress();
		if (tab == BANKS)
		{
			return "Visited: " + CollectionLog.banksVisited(progress) + "/" + banks.size();
		}
		return "Obtained: " + progress.collection.size() + "/" + CollectionLog.DROPS.size()
			+ "    Rolls: " + NumberFormat.getIntegerInstance().format(progress.rolls);
	}

	@Override
	String[] tabs()
	{
		return TABS;
	}

	@Override
	int rowHeight()
	{
		return tab == BANKS ? BANK_ROW : SLOT + SLOT_GAP;
	}

	@Override
	int rowCount()
	{
		if (tab == BANKS)
		{
			return (banks.size() + 1) / 2;
		}
		return (CollectionLog.DROPS.size() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
	}

	@Override
	void drawRow(Graphics2D graphics, int row, int x, int y)
	{
		Progress progress = plugin.getProgress();
		if (tab == BANKS)
		{
			drawBanks(graphics, progress, row, x, y);
		}
		else
		{
			drawDrops(graphics, progress, row, x, y);
		}
	}

	private void drawDrops(Graphics2D graphics, Progress progress, int row, int x, int y)
	{
		Point mouse = client.getMouseCanvasPosition();
		graphics.setFont(FontManager.getRunescapeSmallFont());
		for (int column = 0; column < SLOTS_PER_ROW; column++)
		{
			int index = row * SLOTS_PER_ROW + column;
			if (index >= CollectionLog.DROPS.size())
			{
				return;
			}
			CollectionLog.Drop drop = CollectionLog.DROPS.get(index);
			CollectionLog.Obtained obtained = progress.collection.get(drop.key);
			int slotX = x + column * (SLOT + SLOT_GAP);
			Composite before = graphics.getComposite();
			if (obtained == null)
			{
				// Like the real log: what you haven't got is shown faded.
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
			}
			GuideArt.drawIcon(graphics, art.itemIcon(drop.icon, drop.gold), slotX, y, SLOT);
			graphics.setComposite(before);
			if (obtained != null && obtained.count > 1)
			{
				String count = String.valueOf(obtained.count);
				graphics.setColor(Color.BLACK);
				graphics.drawString(count, slotX + 1, y + 10);
				graphics.setColor(COUNT);
				graphics.drawString(count, slotX, y + 9);
			}
			if (new Rectangle(slotX, y, SLOT, SLOT).contains(mouse.getX(), mouse.getY()))
			{
				tooltipManager.add(new Tooltip(tooltip(drop, obtained)));
			}
		}
	}

	private static String tooltip(CollectionLog.Drop drop, CollectionLog.Obtained obtained)
	{
		if (obtained == null)
		{
			return drop.name + "</br>Not obtained yet";
		}
		NumberFormat format = NumberFormat.getIntegerInstance();
		return drop.name + "</br>Obtained: " + format.format(obtained.count)
			+ "</br>First: " + StatsView.date(obtained.firstAt)
			+ ", after " + format.format(obtained.firstRolls) + " rolls";
	}

	private void drawBanks(Graphics2D graphics, Progress progress, int row, int x, int y)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();
		int columnWidth = LIST_WIDTH / 2;
		for (int column = 0; column < 2; column++)
		{
			int index = row + column * rowCount();
			if (index >= banks.size())
			{
				return;
			}
			BankArea bank = banks.get(index);
			graphics.setColor(progress.banks.containsKey(bank.getKey()) ? GuideArt.INK : GuideArt.FADED_INK);
			String full = bank.getName();
			String name = full;
			for (int keep = full.length(); metrics.stringWidth(name) > columnWidth - 6 && keep > 1; keep--)
			{
				name = full.substring(0, keep - 1) + "...";
			}
			graphics.drawString(name, x + column * columnWidth, y + BANK_ROW - 4);
		}
	}
}
