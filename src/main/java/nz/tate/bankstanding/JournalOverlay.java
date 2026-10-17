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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import nz.tate.bankstanding.core.BankNames;
import nz.tate.bankstanding.core.BankRecord;
import nz.tate.bankstanding.core.Keywords;
import nz.tate.bankstanding.core.Modifiers;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.StatsView;

/**
 * The journal: what you've found of the collection log's secrets, written down on parchment
 * like the skill guide. It has two books, opened from the log: the secret phrases, and what
 * you've been "Dressed for". Each book's groups are its tabs, and only found things are listed.
 */
@Singleton
class JournalOverlay extends ParchmentWindow
{
	static final int PHRASES = 0;
	static final int DRESSED = 1;

	private static final int ROW = 30;
	private static final int ICON = 26;
	private static final Keywords.Group[] PHRASE_GROUPS = Keywords.Group.values();
	/** Shorter names for the sidebar, in the order of {@link Keywords.Group}. */
	private static final String[] PHRASE_TABS = {"Anywhere", "At banks", "On holidays", "When worn", "Spellbooks"};
	private static final String[] DRESSED_GROUPS = {"Outfits", "Diary items", "Full inventories", "Pets at home"};
	private static final String[] DRESSED_TABS = {"Outfits", "Diary items", "Inventories", "Pets at home"};
	private static final Keywords KEYWORDS = new Keywords(Keywords.PHRASES);

	/** One line of the journal. */
	private static final class Line
	{
		final String title;
		final String detail;
		final int icon;
		final boolean unread;
		final boolean faded;
		/** When it was found, for the order. */
		long at;

		Line(String title, String detail, int icon, boolean unread, boolean faded)
		{
			this.title = title;
			this.detail = detail;
			this.icon = icon;
			this.unread = unread;
			this.faded = faded;
		}
	}

	private final BankStandingPlugin plugin;
	private volatile int book;
	/** The shown tab's lines and counts, as last drawn. Client thread. */
	private List<Line> lines = Collections.emptyList();
	private int found;
	private int total;
	/** The book and tab being read, or -1; its new finds count as read once you leave it. Client thread. */
	private int readingBook = -1;
	private int readingTab = -1;

	@Inject
	JournalOverlay(BankStandingPlugin plugin, Client client, GuideArt art)
	{
		super(client, art);
		this.plugin = plugin;
	}

	@Override
	boolean available()
	{
		return plugin.getProgress() != null;
	}

	/** Opens one book on one of its tabs. */
	void show(int book, int tab)
	{
		this.book = book;
		showTab(tab);
		open();
	}

	int book()
	{
		return book;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Stars stay while you read, and go once you've moved to another tab or closed it.
		boolean showing = showing();
		int nowBook = showing ? book : -1;
		int nowTab = showing ? tab : -1;
		Progress progress = plugin.getProgress();
		if (readingTab >= 0 && (readingBook != nowBook || readingTab != nowTab) && progress != null)
		{
			markRead(progress, readingBook, readingTab);
		}
		readingBook = nowBook;
		readingTab = nowTab;
		if (showing && progress != null)
		{
			lines = book == PHRASES ? phraseLines(progress) : dressedLines(progress);
		}
		return super.render(graphics);
	}

	private void markRead(Progress progress, int book, int tab)
	{
		if (book == PHRASES)
		{
			for (Keywords.Noted noted : KEYWORDS.found(progress, PHRASE_GROUPS[Math.min(tab, PHRASE_GROUPS.length - 1)]))
			{
				noted.read = true;
			}
			return;
		}
		String group = DRESSED_GROUPS[Math.min(tab, DRESSED_GROUPS.length - 1)];
		for (Modifiers.Dressed dressed : Modifiers.dressed())
		{
			if (dressed.group.equals(group) && progress.moments.containsKey(dressed.key))
			{
				progress.journalRead.add(dressed.key);
			}
		}
	}

	private List<Line> phraseLines(Progress progress)
	{
		Keywords.Group group = PHRASE_GROUPS[Math.min(tab, PHRASE_GROUPS.length - 1)];
		int[] count = KEYWORDS.foundByGroup(progress).get(group);
		found = count[0];
		total = count[1];
		List<Line> shown = new ArrayList<>();
		for (Keywords.Noted noted : KEYWORDS.found(progress, group))
		{
			if (noted.said == null)
			{
				shown.add(new Line("(Found before the journal kept notes)",
					"Say it again to write it down. Last said: " + StatsView.date(noted.at), -1, false, true));
			}
			else
			{
				shown.add(new Line("\"" + noted.said + "\"", where(progress, noted.bank) + StatsView.date(noted.at), -1,
					!noted.read, false));
			}
		}
		return shown;
	}

	private List<Line> dressedLines(Progress progress)
	{
		String group = DRESSED_GROUPS[Math.min(tab, DRESSED_GROUPS.length - 1)];
		found = 0;
		total = 0;
		List<Line> shown = new ArrayList<>();
		for (Modifiers.Dressed dressed : Modifiers.dressed())
		{
			if (!dressed.group.equals(group))
			{
				continue;
			}
			total++;
			Long at = progress.moments.get(dressed.key);
			if (at == null)
			{
				continue;
			}
			found++;
			boolean unread = at >= progress.journalSince && !progress.journalRead.contains(dressed.key);
			String bank = BankNames.displayName(dressed.bankKey, progress.banks.get(dressed.bankKey));
			String title = dressed.pet != null ? capitalise(dressed.pet) : bank;
			String detail = (dressed.pet != null ? bank + ", " : "") + StatsView.date(at);
			Line line = new Line(title, detail, dressed.icon, unread, false);
			line.at = at;
			shown.add(line);
		}
		shown.sort((a, b) -> Long.compare(a.at, b.at));
		return shown;
	}

	private static String capitalise(String text)
	{
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	private static String where(Progress progress, String bank)
	{
		BankRecord record = bank == null ? null : progress.banks.get(bank);
		return record == null ? "" : BankNames.displayName(bank, record) + ", ";
	}

	@Override
	String title()
	{
		return book == PHRASES ? "Phrase Journal" : "Dressed for it";
	}

	@Override
	String subtitle()
	{
		String group = book == PHRASES ? PHRASE_GROUPS[Math.min(tab, PHRASE_GROUPS.length - 1)].title
			: DRESSED_GROUPS[Math.min(tab, DRESSED_GROUPS.length - 1)];
		return group + ": " + found + "/" + total + " found";
	}

	@Override
	String[] tabs()
	{
		return book == PHRASES ? PHRASE_TABS : DRESSED_TABS;
	}

	@Override
	String link()
	{
		return "Collection log";
	}

	@Override
	void linkClicked()
	{
		plugin.openJournalSectionInLog(book);
	}

	@Override
	int rowHeight()
	{
		return ROW;
	}

	@Override
	int rowCount()
	{
		// One more for the closing line, unless every one is found.
		return lines.size() + (found < total ? 1 : 0);
	}

	@Override
	void drawRow(Graphics2D graphics, int row, int x, int y)
	{
		List<Line> shown = lines;
		if (row >= shown.size())
		{
			String end = shown.isEmpty() ? "Nothing found here yet." : "There's more to find...";
			art.plain.draw(graphics, end, x, y + 8, GuideArt.FADED_INK);
			return;
		}
		Line line = shown.get(row);
		int textX = x;
		if (line.icon > 0)
		{
			GuideArt.drawIcon(graphics, art.itemIcon(line.icon, false), x, y + (ROW - ICON) / 2, ICON);
			textX += ICON + 6;
		}
		int end = art.bold.draw(graphics, line.title, textX, y + 2, line.faded ? GuideArt.FADED_INK : GuideArt.INK);
		if (line.unread)
		{
			GuideArt.star(graphics, end + 8, y + 2 + art.bold.height() / 2, 5);
		}
		art.small.draw(graphics, line.detail, textX, y + 16, GuideArt.FADED_INK);
	}
}
