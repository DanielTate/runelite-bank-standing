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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.overlay.components.TooltipComponent;
import nz.tate.bankstanding.core.BankArea;
import nz.tate.bankstanding.core.BankNames;
import nz.tate.bankstanding.core.BankRegions;
import nz.tate.bankstanding.core.CollectionLog;
import nz.tate.bankstanding.core.Companions;
import nz.tate.bankstanding.core.Experience;
import nz.tate.bankstanding.core.GearSets;
import nz.tate.bankstanding.core.Holiday;
import nz.tate.bankstanding.core.Keywords;
import nz.tate.bankstanding.core.Modifiers;
import nz.tate.bankstanding.core.Moments;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.Records;
import nz.tate.bankstanding.core.Spellbook;
import nz.tate.bankstanding.core.StatsView;
import nz.tate.bankstanding.core.Unlocks;

/**
 * The Bank Standing collection log, drawn like the game's own: a steel-framed stone window
 * with tabs along the top, entries down the left, and on the right the selected entry with
 * how much of it you've got, a progress bar, and its items or places.
 */
@Singleton
class CollectionLogOverlay extends GameWindow
{
	static final int DROPS = 0;
	static final int BANKS = 1;
	static final int MILESTONES = 2;
	static final int UNLOCKS = 3;
	static final int MOMENTS = 4;
	static final int GEAR = 5;
	private static final String[] TABS = {"Drops", "Banks", "Milestones", "Unlocks", "Moments", "Gear"};

	private static final int WIDTH = 500;
	private static final int HEIGHT = 320;
	private static final Rectangle CLOSE = new Rectangle(WIDTH - 33, 7, 26, 23);
	private static final int TAB_Y = 32;
	private static final int TAB_HEIGHT = 20;
	private static final int TAB_X = 8;
	private static final int TAB_WIDTH = (WIDTH - 2 * TAB_X) / TABS.length;
	private static final int BODY_TOP = TAB_Y + TAB_HEIGHT + 2;
	private static final int BODY_BOTTOM = HEIGHT - 9;
	private static final int LIST_LEFT = 8;
	private static final int LIST_RIGHT = 190;
	private static final int ENTRY_ROW = 15;
	private static final int RIGHT = LIST_RIGHT + 9;
	private static final int RIGHT_END = WIDTH - 9;
	private static final int SCROLLBAR_X = RIGHT_END - 16;
	private static final int BAR_Y = BODY_TOP + 50;
	private static final int BAR_HEIGHT = 14;
	private static final int CONTENT_TOP = BAR_Y + BAR_HEIGHT + 10;
	private static final int SLOT = 32;
	private static final int SLOT_STEP = 42;
	private static final int LINE = 14;

	private static final Color ORANGE = new Color(0xff, 0x98, 0x1f);
	private static final Color WHITE = new Color(0xff, 0xff, 0xff);
	private static final Color GREY = new Color(0x9f, 0x9f, 0x9f);
	private static final Color RED = new Color(0xff, 0x00, 0x00);
	private static final Color YELLOW = new Color(0xff, 0xff, 0x00);
	private static final Color GREEN = new Color(0x0d, 0xc1, 0x0d);
	private static final Color COUNT = new Color(0xff, 0xff, 0x00);
	private static final Color SEPARATOR = new Color(0x58, 0x50, 0x40);
	private static final Color SELECTED_ROW = new Color(0x5a, 0x4f, 0x3e);
	private static final Color BAR_BACK = new Color(0x2b, 0x25, 0x1d);
	private static final int[] LEVEL_MARKS = {10, 20, 30, 40, 50, 60, 70, 80, 90, 99};
	private static final int[] XP_MARKS = {100_000_000, Experience.MAX_XP};
	private static final Keywords PHRASES = new Keywords(Keywords.PHRASES);
	/** Shown instead of the phrases themselves, which stay secret even once found. */
	private static final String[] PHRASE_HINT = {
		"Said in public chat while bank standing.",
		"Click the journal to read the ones you've found."
	};
	/** The journal icon by each phrase count, which opens the phrase journal. */
	private static final int JOURNAL_ICON = 2967;
	private static final int JOURNAL_SIZE = 16;
	private static final int PHRASES_ENTRY = 3;
	private static final int DRESSED_ENTRY = 4;
	/** Shown for a hidden entry until it's found. */
	private static final String SECRET = "Secret";
	/** How far below the cursor a tooltip sits, as RuneLite's own do. */
	private static final int TOOLTIP_GAP = 24;
	/** The icon a hidden drop shows until it's found. */
	private static final int SECRET_ICON = 6199;
	private static final String[] DRESSED_HINT = {
		"Worn, or played, at the right bank while standing.",
		"Click the journal to read the ones you've found."
	};

	/** One thing in an entry: an item for the grid, or a line of text. */
	private static final class Slot
	{
		final String name;
		final boolean done;
		/** Item icon, or -1 for a line of text. */
		final int icon;
		final boolean gold;
		/** Shown on hover. */
		final String detail;
		final int count;
		/** A picture to show instead of an item icon, or null. */
		BufferedImage image;

		Slot(String name, boolean done, int icon, boolean gold, String detail, int count)
		{
			this.name = name;
			this.done = done;
			this.icon = icon;
			this.gold = gold;
			this.detail = detail;
			this.count = count;
		}
	}

	/** A count shown in place of the slots, like "Anywhere: 3/12". */
	private static final class Row
	{
		final String label;
		final int done;
		final int total;
		/** The journal book and tab its icon opens, or -1 for none. */
		int journalBook = -1;
		int journal = -1;
		/** Whether the journal has new finds on that tab. */
		boolean unread;

		Row(String label, int done, int total)
		{
			this.label = label;
			this.done = done;
			this.total = total;
		}
	}

	/** An entry in the left-hand list. */
	private static final class Entry
	{
		final String name;
		final List<Slot> slots = new ArrayList<>();
		/** An extra line under the count, like "Kills" in the game: label and value, or null. */
		String extraLabel;
		String extraValue;
		/** Counts and lines shown in place of the slots, which then only count toward the total. */
		List<Row> rows;
		String[] hint;
		/** Columns for a text list. */
		int columns = 2;

		Entry(String name)
		{
			this.name = name;
		}

		boolean noted()
		{
			return rows != null || hint != null;
		}

		boolean grid()
		{
			return !slots.isEmpty() && (slots.get(0).icon > 0 || slots.get(0).image != null);
		}

		int done()
		{
			int done = 0;
			for (Slot slot : slots)
			{
				if (slot.done)
				{
					done++;
				}
			}
			return done;
		}
	}

	private final BankStandingPlugin plugin;
	private final RuneLiteConfig runeLiteConfig;
	private final HitsplatOverlay hitsplats;
	/** The hovered slot's tooltip this frame, drawn last so it sits on top of the window. */
	private String tooltip;

	private volatile int tab;
	private volatile int entry;
	private volatile int scroll;
	private volatile int contentRows;
	private volatile int visibleContentRows = 1;
	/** Entries on the shown tab, as last drawn, for clicks on the mouse thread. */
	private volatile int entryCount;
	/** The title's count over every tab, rebuilt at most once a second. Client thread. */
	private String totalCount = "";
	private long totalCountedAt;
	/** The journal icons as last drawn, in window coordinates, and the book and tab each opens. */
	private volatile List<Rectangle> journalButtons = new ArrayList<>();
	private volatile List<int[]> journalTabs = new ArrayList<>();

	@Inject
	CollectionLogOverlay(BankStandingPlugin plugin, Client client, GuideArt art, RuneLiteConfig runeLiteConfig,
		HitsplatOverlay hitsplats)
	{
		super(client, art);
		this.plugin = plugin;
		this.runeLiteConfig = runeLiteConfig;
		this.hitsplats = hitsplats;
	}

	@Override
	boolean available()
	{
		return plugin.getProgress() != null;
	}

	void showTab(int index)
	{
		tab = index;
		entry = 0;
		scroll = 0;
	}

	/** Shows the secret phrases or "Dressed for it" entry, as when coming back from the journal. */
	void showSecrets(boolean dressed)
	{
		showTab(MILESTONES);
		entry = dressed ? DRESSED_ENTRY : PHRASES_ENTRY;
	}

	@Override
	void click(int x, int y)
	{
		if (CLOSE.contains(x, y))
		{
			close();
			return;
		}
		List<Rectangle> buttons = journalButtons;
		List<int[]> tabs = journalTabs;
		for (int i = 0; i < buttons.size() && i < tabs.size(); i++)
		{
			if (buttons.get(i).contains(x, y))
			{
				plugin.openJournal(tabs.get(i)[0], tabs.get(i)[1]);
				return;
			}
		}
		if (y >= TAB_Y && y < TAB_Y + TAB_HEIGHT && x >= TAB_X)
		{
			int index = (x - TAB_X) / TAB_WIDTH;
			if (index < TABS.length)
			{
				showTab(index);
			}
			return;
		}
		if (x >= LIST_LEFT && x < LIST_RIGHT && y >= BODY_TOP)
		{
			int index = (y - BODY_TOP) / ENTRY_ROW;
			if (index < entryCount)
			{
				entry = index;
				scroll = 0;
			}
			return;
		}
		if (x >= SCROLLBAR_X && x < SCROLLBAR_X + GuideArt.ARROW)
		{
			if (y >= CONTENT_TOP && y < CONTENT_TOP + GuideArt.ARROW)
			{
				scrollBy(-1);
			}
			else if (y >= BODY_BOTTOM - GuideArt.ARROW && y < BODY_BOTTOM)
			{
				scrollBy(1);
			}
		}
	}

	@Override
	void wheel(int x, int y, int rotation)
	{
		if (x >= RIGHT)
		{
			scrollBy(rotation);
		}
	}

	private void scrollBy(int rows)
	{
		scroll = Math.max(0, Math.min(Math.max(0, contentRows - visibleContentRows), scroll + rows));
	}

	/** The whole log's {found, total}, as its title shows it. Client thread. */
	int[] total(Progress progress)
	{
		int done = 0;
		int total = 0;
		for (int t = 0; t < TABS.length; t++)
		{
			for (Entry each : entries(progress, t))
			{
				done += each.done();
				total += each.slots.size();
			}
		}
		return new int[]{done, total};
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Progress progress = plugin.getProgress();
		if (!showing() || progress == null)
		{
			return null;
		}
		Rectangle window = place(WIDTH, HEIGHT);
		int x = window.x;
		int y = window.y;

		art.darkBacking(graphics, x, y, WIDTH, HEIGHT);
		art.steelBorder(graphics, x, y, WIDTH, HEIGHT);
		art.draw(graphics, GuideArt.STEEL_CLOSE, x + CLOSE.x, y + CLOSE.y);

		long now = System.currentTimeMillis();
		if (now - totalCountedAt >= 1000)
		{
			int[] counted = total(progress);
			totalCount = counted[0] + "/" + counted[1];
			totalCountedAt = now;
		}
		art.bold.drawShadowedCentred(graphics, "Bank Standing Collection Log - " + totalCount, x + WIDTH / 2, y + 11, ORANGE);

		for (int t = 0; t < TABS.length; t++)
		{
			int tabX = x + TAB_X + t * TAB_WIDTH;
			art.tab(graphics, tabX, y + TAB_Y, TAB_WIDTH, t == tab);
			// Centred in the flat part of the tab, clear of its slanted end.
			int room = TAB_WIDTH - art.tabSlant() - 4;
			art.small.drawShadowedCentred(graphics, TABS[t], tabX + 2 + room / 2,
				art.small.centredTop(y + TAB_Y, TAB_HEIGHT), ORANGE);
		}
		graphics.setColor(SEPARATOR);
		graphics.drawLine(x + TAB_X, y + BODY_TOP - 1, x + WIDTH - TAB_X, y + BODY_TOP - 1);
		graphics.drawLine(x + LIST_RIGHT + 3, y + BODY_TOP, x + LIST_RIGHT + 3, y + BODY_BOTTOM);

		List<Entry> entries = entries(progress);
		entryCount = entries.size();
		int selected = Math.min(entry, entries.size() - 1);
		for (int i = 0; i < entries.size(); i++)
		{
			Entry each = entries.get(i);
			int rowY = y + BODY_TOP + i * ENTRY_ROW;
			if (rowY + ENTRY_ROW > y + BODY_BOTTOM)
			{
				break;
			}
			if (i == selected)
			{
				graphics.setColor(SELECTED_ROW);
				graphics.fillRect(x + LIST_LEFT, rowY, LIST_RIGHT - LIST_LEFT, ENTRY_ROW);
			}
			boolean complete = each.done() == each.slots.size();
			art.plain.drawShadowed(graphics, each.name, x + LIST_LEFT + 4, art.plain.centredTop(rowY, ENTRY_ROW),
				complete ? GREEN : ORANGE);
		}
		tooltip = null;
		if (selected >= 0)
		{
			drawEntry(graphics, entries.get(selected), x, y);
		}
		if (tooltip != null)
		{
			drawTooltip(graphics, tooltip);
		}
		return new Dimension(WIDTH, HEIGHT);
	}

	/**
	 * Draws a tooltip like RuneLite's own, below the cursor and kept on screen. RuneLite draws
	 * its tooltips under always-on-top overlays like this window, so the window draws its own.
	 */
	private void drawTooltip(Graphics2D graphics, String text)
	{
		TooltipComponent component = new TooltipComponent();
		component.setText(text);
		component.setBackgroundColor(runeLiteConfig.overlayBackgroundColor());
		component.setModIcons(client.getModIcons());

		// Measure first, off screen, so the tooltip can be kept inside the canvas.
		BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D measure = scratch.createGraphics();
		Font font = runeLiteConfig.tooltipFont().getFont();
		measure.setFont(font);
		component.setPreferredLocation(new java.awt.Point(0, 0));
		Dimension size = component.render(measure);
		measure.dispose();

		Point mouse = client.getMouseCanvasPosition();
		int tipX = Math.max(0, Math.min(mouse.getX(), client.getCanvasWidth() - size.width));
		int tipY = mouse.getY() + TOOLTIP_GAP;
		if (tipY + size.height > client.getCanvasHeight())
		{
			tipY = Math.max(0, mouse.getY() - TOOLTIP_GAP - size.height);
		}
		component.setPreferredLocation(new java.awt.Point(tipX, tipY));
		Font original = graphics.getFont();
		graphics.setFont(font);
		component.render(graphics);
		graphics.setFont(original);
	}

	private void drawEntry(Graphics2D graphics, Entry shown, int x, int y)
	{
		int left = x + RIGHT;
		art.bold.drawShadowed(graphics, shown.name, left, y + BODY_TOP + 2, ORANGE);
		int done = shown.done();
		int total = shown.slots.size();
		Color countColour = done == 0 ? RED : done == total ? GREEN : YELLOW;
		int after = art.plain.drawShadowed(graphics, "Obtained: ", left, y + BODY_TOP + 20, ORANGE);
		art.plain.drawShadowed(graphics, done + "/" + total, after, y + BODY_TOP + 20, countColour);
		if (shown.extraLabel != null)
		{
			after = art.plain.drawShadowed(graphics, shown.extraLabel + ": ", left, y + BODY_TOP + 34, ORANGE);
			art.plain.drawShadowed(graphics, shown.extraValue, after, y + BODY_TOP + 34, WHITE);
		}

		int barWidth = x + RIGHT_END - left;
		graphics.setColor(BAR_BACK);
		graphics.fillRect(left, y + BAR_Y, barWidth, BAR_HEIGHT);
		int fill = total == 0 ? 0 : (barWidth - 2) * done / total;
		graphics.setColor(done == total ? GREEN.darker() : new Color(0xb0, 0x9c, 0x12));
		graphics.fillRect(left + 1, y + BAR_Y + 1, fill, BAR_HEIGHT - 2);
		graphics.setColor(SEPARATOR);
		graphics.drawRect(left, y + BAR_Y, barWidth - 1, BAR_HEIGHT - 1);
		String percent = (total == 0 ? 0 : done * 100 / total) + "%";
		art.small.drawShadowedCentred(graphics, percent, left + barWidth / 2, y + BAR_Y + 2, WHITE);
		graphics.setColor(SEPARATOR);
		graphics.drawLine(x + LIST_RIGHT + 4, y + CONTENT_TOP - 5, x + RIGHT_END, y + CONTENT_TOP - 5);

		List<Rectangle> buttons = new ArrayList<>();
		List<int[]> tabs = new ArrayList<>();
		if (shown.noted())
		{
			contentRows = 0;
			scroll = 0;
			int lineY = y + CONTENT_TOP + 2;
			Point mouse = client.getMouseCanvasPosition();
			if (shown.rows != null)
			{
				for (Row row : shown.rows)
				{
					int valueX = art.plain.drawShadowed(graphics, row.label + ": ", left, lineY, ORANGE);
					Color colour = row.done == 0 ? RED : row.done == row.total ? GREEN : YELLOW;
					int end = art.plain.drawShadowed(graphics, row.done + "/" + row.total, valueX, lineY, colour);
					if (row.journal >= 0 && row.done > 0)
					{
						Rectangle button = new Rectangle(end + 6, lineY + (art.plain.height() - JOURNAL_SIZE) / 2,
							JOURNAL_SIZE, JOURNAL_SIZE);
						GuideArt.drawIcon(graphics, art.itemIcon(JOURNAL_ICON, false), button.x, button.y, JOURNAL_SIZE);
						if (row.unread)
						{
							GuideArt.star(graphics, button.x + JOURNAL_SIZE, button.y + 1, 4);
						}
						if (button.contains(mouse.getX(), mouse.getY()))
						{
							tooltip = "Read the ones you've found";
						}
						buttons.add(new Rectangle(button.x - x, button.y - y, button.width, button.height));
						tabs.add(new int[]{row.journalBook, row.journal});
					}
					lineY += LINE + 6;
				}
				lineY += LINE / 2;
			}
			if (shown.hint != null)
			{
				for (String line : shown.hint)
				{
					art.small.drawShadowed(graphics, line, left, lineY, GREY);
					lineY += LINE;
				}
			}
			journalButtons = buttons;
			journalTabs = tabs;
			return;
		}
		journalButtons = buttons;
		journalTabs = tabs;

		Point mouse = client.getMouseCanvasPosition();
		boolean grid = shown.grid();
		int perRow = grid ? (SCROLLBAR_X - RIGHT + SLOT_STEP - SLOT) / SLOT_STEP : shown.columns;
		int rowHeight = grid ? SLOT_STEP : LINE;
		contentRows = (total + perRow - 1) / perRow;
		visibleContentRows = Math.max(1, (BODY_BOTTOM - CONTENT_TOP) / rowHeight);
		scrollBy(0);
		int first = scroll;
		int columnWidth = (SCROLLBAR_X - RIGHT - 4) / perRow;
		for (int row = first; row < Math.min(contentRows, first + visibleContentRows); row++)
		{
			for (int column = 0; column < perRow; column++)
			{
				int index = grid ? row * perRow + column : row + column * contentRows;
				if (index >= total)
				{
					continue;
				}
				Slot slot = shown.slots.get(index);
				int slotX = left + column * (grid ? SLOT_STEP : columnWidth);
				int slotY = y + CONTENT_TOP + (row - first) * rowHeight;
				Rectangle hover = grid ? new Rectangle(slotX, slotY, SLOT, SLOT) : new Rectangle(slotX, slotY, columnWidth, LINE);
				if (grid)
				{
					drawItem(graphics, slot, slotX, slotY);
				}
				else
				{
					art.small.drawShadowed(graphics, fit(slot.name, columnWidth - 4), slotX, slotY + 2, slot.done ? WHITE : GREY);
				}
				if (hover.contains(mouse.getX(), mouse.getY()))
				{
					tooltip = slot.name + "</br>" + slot.detail;
				}
			}
		}
		if (contentRows > visibleContentRows)
		{
			int height = visibleContentRows * rowHeight;
			art.scrollbar(graphics, GuideArt.DARK_SCROLLBAR, x + SCROLLBAR_X, y + CONTENT_TOP, height,
				contentRows, visibleContentRows, first);
		}
	}

	private void drawItem(Graphics2D graphics, Slot slot, int x, int y)
	{
		Composite before = graphics.getComposite();
		if (!slot.done)
		{
			// Like the game's log: what you haven't got is faded.
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
		}
		GuideArt.drawIcon(graphics, slot.image != null ? slot.image : art.itemIcon(slot.icon, slot.gold), x, y, SLOT);
		graphics.setComposite(before);
		if (slot.count > 1)
		{
			art.small.drawShadowed(graphics, String.valueOf(slot.count), x, y, COUNT);
		}
	}

	private String fit(String text, int width)
	{
		String shown = text;
		for (int keep = text.length(); art.small.width(shown) > width && keep > 1; keep--)
		{
			shown = text.substring(0, keep - 1) + "...";
		}
		return shown;
	}

	private List<Entry> entries(Progress progress)
	{
		return entries(progress, tab);
	}

	private List<Entry> entries(Progress progress, int tab)
	{
		List<Entry> entries = new ArrayList<>();
		NumberFormat format = NumberFormat.getIntegerInstance();
		if (tab == DROPS)
		{
			for (CollectionLog.Group group : CollectionLog.Group.values())
			{
				Entry each = new Entry(group.title);
				for (CollectionLog.Drop drop : CollectionLog.DROPS)
				{
					if (drop.group != group)
					{
						continue;
					}
					CollectionLog.Obtained obtained = progress.collection.get(drop.key);
					if (drop.hidden && obtained == null)
					{
						each.slots.add(new Slot(SECRET, false, SECRET_ICON, false, "Not found yet", 0));
						continue;
					}
					String detail = obtained == null ? "Not obtained yet" : "Obtained: " + format.format(obtained.count)
						+ "</br>First: " + StatsView.date(obtained.firstAt) + ", after " + format.format(obtained.firstRolls) + " rolls";
					if (obtained == null && drop.holiday != null)
					{
						detail += "</br>Only drops at " + holidayName(drop.holiday);
					}
					each.slots.add(new Slot(drop.name, obtained != null, drop.icon, drop.gold, detail,
						obtained == null ? 0 : obtained.count));
				}
				each.extraLabel = "Rolls";
				each.extraValue = format.format(progress.rolls);
				entries.add(each);
			}
			Entry splats = new Entry("Hitsplats");
			for (Moments.Splat splat : Moments.Splat.values())
			{
				Long at = progress.moments.get(splat.key);
				Slot slot = new Slot(splat.title, at != null, -1, false, at == null ? "Not seen yet" : first(at), 0);
				slot.image = hitsplats.icon(splat);
				splats.slots.add(slot);
			}
			entries.add(splats);
		}
		else if (tab == BANKS)
		{
			for (String region : BankRegions.names())
			{
				Entry banks = new Entry(region);
				for (String key : BankRegions.keys(region))
				{
					boolean visited = progress.banks.containsKey(key);
					String detail = visited ? "Stood here for " + StatsView.duration(progress.banks.get(key).ticks)
						: "Not visited yet";
					banks.slots.add(new Slot(name(key), visited, -1, false, detail, 0));
				}
				entries.add(banks);
			}
		}
		else if (tab == MILESTONES)
		{
			Records records = progress.records;
			Entry levels = new Entry("Levels");
			for (int level : LEVEL_MARKS)
			{
				levels.slots.add(milestone("Level " + level, progress.level() >= level, records.find("level", level)));
			}
			Entry stands = new Entry("Long stands");
			for (int minutes : Records.STAND_MINUTES)
			{
				stands.slots.add(milestone(standLength(minutes) + " in one stand",
					records.longestStandTicks >= minutes * Records.TICKS_PER_MINUTE, records.findStand(minutes)));
			}
			Entry experience = new Entry("Experience");
			for (int mark : XP_MARKS)
			{
				experience.slots.add(milestone(format.format(mark) + " XP", progress.xp >= mark, records.find("xp", mark)));
			}
			Entry phrases = new Entry("Secret phrases");
			phrases.rows = new ArrayList<>();
			for (Map.Entry<Keywords.Group, int[]> group : PHRASES.foundByGroup(progress).entrySet())
			{
				int[] count = group.getValue();
				Row row = new Row(group.getKey().title, count[0], count[1]);
				row.journalBook = JournalOverlay.PHRASES;
				row.journal = group.getKey().ordinal();
				row.unread = PHRASES.unread(progress, group.getKey());
				phrases.rows.add(row);
				for (int i = 0; i < count[1]; i++)
				{
					phrases.slots.add(new Slot("Secret phrase", i < count[0], -1, false, "", 0));
				}
			}
			phrases.hint = PHRASE_HINT;
			Entry dressed = new Entry("Dressed for it");
			dressed.rows = new ArrayList<>();
			for (Map.Entry<String, int[]> group : Modifiers.foundByGroup(progress.moments).entrySet())
			{
				int[] count = group.getValue();
				Row row = new Row(group.getKey(), count[0], count[1]);
				row.journalBook = JournalOverlay.DRESSED;
				row.journal = dressed.rows.size();
				row.unread = dressedUnread(progress, group.getKey());
				dressed.rows.add(row);
				for (int i = 0; i < count[1]; i++)
				{
					dressed.slots.add(new Slot("Secret", i < count[0], -1, false, "", 0));
				}
			}
			dressed.hint = DRESSED_HINT;
			entries.add(levels);
			entries.add(stands);
			entries.add(experience);
			entries.add(phrases);
			entries.add(dressed);
		}
		else if (tab == UNLOCKS)
		{
			int level = progress.level();
			for (Unlocks.Category category : Unlocks.Category.values())
			{
				Entry unlocks = new Entry(category.title);
				unlocks.columns = 1;
				for (Unlocks.Unlock unlock : Unlocks.in(category))
				{
					boolean done = level >= unlock.level;
					unlocks.slots.add(new Slot("Level " + unlock.level + ": " + unlock.text, done, -1, false,
						done ? "Unlocked" : "Unlocks at level " + unlock.level, 0));
				}
				entries.add(unlocks);
			}
		}
		else if (tab == GEAR)
		{
			for (GearSets.GearSet set : GearSets.SETS)
			{
				Entry gear = new Entry(set.title);
				for (GearSets.Tier tier : set.tiers)
				{
					Long at = progress.moments.get(set.momentKey(tier));
					gear.slots.add(new Slot(tier.name, at != null, tier.icon, false, at != null ? first(at)
						: set.banks.isEmpty() ? "Wear the whole outfit while standing at any bank"
						: "Stand at the " + BankNames.displayName(set.banks.get(0), null) + " with one", 0));
				}
				entries.add(gear);
			}
			// Secret until found: stand at the right bank on each spellbook.
			Entry books = new Entry("Spellbooks");
			for (Spellbook book : Spellbook.values())
			{
				Long at = progress.moments.get(book.momentKey());
				books.slots.add(at == null ? new Slot(SECRET, false, SECRET_ICON, false, "Not found yet", 0)
					: new Slot(book.title, true, book.icon, false, first(at), 0));
			}
			entries.add(books);
		}
		else
		{
			Entry holidays = new Entry("Holidays");
			holidays.columns = 1;
			for (Moments.HolidayEntry holiday : Moments.HolidayEntry.values())
			{
				Long at = progress.moments.get(holiday.key());
				holidays.slots.add(new Slot(holiday.title, at != null, -1, false,
					holiday.when + "</br>" + (at == null ? "Not stood through yet" : first(at)), 0));
			}
			entries.add(holidays);
			for (Moments.Category category : Moments.Category.values())
			{
				Entry moments = new Entry(category.title);
				for (Moments.Odd moment : Moments.Odd.values())
				{
					if (moment.category != category)
					{
						continue;
					}
					Long at = progress.moments.get(moment.key);
					String title = moment.hidden && at == null ? SECRET : moment.title;
					moments.slots.add(new Slot(title, at != null, -1, false,
						moment.description + (at == null ? "" : "</br>" + first(at)), 0));
				}
				entries.add(moments);
			}
			for (Companions.Group group : Companions.GROUPS)
			{
				Entry pets = new Entry(group.title);
				for (Companions.Kind kind : group.kinds)
				{
					Long at = progress.moments.get(group.momentKey(kind));
					pets.slots.add(new Slot(kind.name, at != null, kind.icon, false,
						at == null ? "Stand at any bank with one following you" : first(at), 0));
				}
				entries.add(pets);
			}
		}
		return entries;
	}

	/**
	 * The stand length shown in the log, in whole hours: the longest stand is called 6 hours
	 * though it counts at 5:50, since the game logs you out at 6.
	 */
	private static boolean dressedUnread(Progress progress, String group)
	{
		for (Modifiers.Dressed dressed : Modifiers.dressed())
		{
			Long at = dressed.group.equals(group) ? progress.moments.get(dressed.key) : null;
			if (at != null && at >= progress.journalSince && !progress.journalRead.contains(dressed.key))
			{
				return true;
			}
		}
		return false;
	}

	private static String standLength(int minutes)
	{
		int hours = (minutes + 59) / 60;
		return hours + (hours == 1 ? " hour" : " hours");
	}

	private static String holidayName(Holiday holiday)
	{
		for (Moments.HolidayEntry entry : Moments.HolidayEntry.values())
		{
			if (entry.holiday == holiday)
			{
				return entry.title;
			}
		}
		return holiday.name();
	}

	private static String first(long at)
	{
		return at == Moments.BEFORE_TRACKING ? "Before the log tracked it" : "First: " + StatsView.date(at);
	}

	private static Slot milestone(String name, boolean done, Records.Milestone when)
	{
		String detail = !done ? "Not reached yet" : when == null ? "Reached before records began"
			: "Reached " + StatsView.date(when.at);
		return new Slot(name, done, -1, false, detail, 0);
	}

	private static String name(String key)
	{
		for (BankArea area : BankNames.all())
		{
			if (area.getKey().equals(key))
			{
				return area.getName();
			}
		}
		return key;
	}
}
