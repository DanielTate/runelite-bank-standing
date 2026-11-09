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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import nz.tate.bankstanding.core.StatsView;

/** The side panel's small graphs. Results only, like the rest of the panel. */
final class PanelGraphs
{
	static final Color DEFAULT_ACCENT = new Color(214, 176, 48);
	/** How opaque the shading under the session graph's line is. */
	private static final int GRAPH_FILL_ALPHA = 60;
	/** Calendar squares brighter than this take dark text; dimmer ones take light. */
	private static final int DARK_TEXT_BRIGHTNESS = 100;
	/** Graphs sit on the panel's darker cards, so their empty parts are a step lighter. */
	private static final Color EMPTY = new Color(52, 52, 52);
	private static final Color PLOT = ColorScheme.DARK_GRAY_COLOR;
	private static final Color GRID = new Color(50, 50, 50);

	/** The chosen accent. Everything reads it while painting, so a repaint is all a change needs. */
	private static volatile Color accent = DEFAULT_ACCENT;

	private PanelGraphs()
	{
	}

	static Color accent()
	{
		return accent;
	}

	static void setAccent(Color colour)
	{
		accent = colour != null ? colour : DEFAULT_ACCENT;
	}

	/** A label in the accent colour. */
	static final class AccentLabel extends JLabel
	{
		AccentLabel()
		{
		}

		AccentLabel(String text, int alignment)
		{
			super(text, alignment);
		}

		@Override
		public Color getForeground()
		{
			return accent();
		}
	}

	/**
	 * A panel with slightly rounded corners: a fill, an optional 1px edge, and an optional
	 * 2px accent stripe down the left side.
	 */
	static final class Rounded extends JPanel
	{
		/** The corner radius: just enough to soften the corners. */
		private static final int RADIUS = 2;
		private final Color fill;
		private final Color edge;
		private final boolean stripe;

		Rounded(java.awt.LayoutManager layout, Color fill, Color edge, boolean stripe)
		{
			super(layout);
			this.fill = fill;
			this.edge = edge;
			this.stripe = stripe;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int arc = RADIUS * 2;
			g2.setColor(fill);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
			if (stripe)
			{
				java.awt.Shape clip = g2.getClip();
				g2.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
				g2.setColor(accent());
				g2.fillRect(0, 0, 2, getHeight());
				g2.setClip(clip);
			}
			if (edge != null)
			{
				g2.setColor(edge);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
			}
			g2.dispose();
		}
	}

	/** This session's XP, one point per minute of standing. */
	static final class SessionXp extends JComponent
	{
		private static final int HEIGHT = 56;
		private static final int PAD = 4;
		private long[] samples = new long[0];

		SessionXp()
		{
			setPreferredSize(new Dimension(0, HEIGHT));
			setToolTipText("");
		}

		void setSamples(long[] samples)
		{
			this.samples = samples;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(PLOT);
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.setColor(GRID);
			for (int line = 1; line < 4; line++)
			{
				int y = PAD + (getHeight() - 2 * PAD) * line / 4;
				g2.drawLine(PAD, y, getWidth() - PAD, y);
			}
			if (samples.length < 2)
			{
				g2.setFont(FontManager.getRunescapeSmallFont());
				g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				String text = "Stand for a minute to start the graph";
				FontMetrics metrics = g2.getFontMetrics();
				g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
					(getHeight() + metrics.getAscent()) / 2 - 1);
				g2.dispose();
				return;
			}

			long max = Math.max(1, samples[samples.length - 1]);
			for (long sample : samples)
			{
				max = Math.max(max, sample);
			}
			int n = samples.length;
			int[] xs = new int[n + 2];
			int[] ys = new int[n + 2];
			for (int i = 0; i < n; i++)
			{
				xs[i] = x(i);
				ys[i] = PAD + (int) Math.round((getHeight() - 2 * PAD) * (1 - (double) samples[i] / max));
			}
			xs[n] = xs[n - 1];
			ys[n] = getHeight() - PAD;
			xs[n + 1] = xs[0];
			ys[n + 1] = getHeight() - PAD;
			Color line = accent();
			g2.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(), GRAPH_FILL_ALPHA));
			g2.fillPolygon(new Polygon(xs, ys, n + 2));
			g2.setColor(line);
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawPolyline(xs, ys, n);
			g2.dispose();
		}

		private int x(int i)
		{
			return PAD + (int) Math.round((getWidth() - 2 * PAD) * (double) i / (samples.length - 1));
		}

		@Override
		public String getToolTipText(MouseEvent e)
		{
			if (samples.length < 2)
			{
				return null;
			}
			double at = (double) (e.getX() - PAD) / Math.max(1, getWidth() - 2 * PAD);
			int i = (int) Math.round(Math.max(0, Math.min(1, at)) * (samples.length - 1));
			return String.format(Locale.ENGLISH, "%s in: %,d xp", StatsView.duration(i * 100L), samples[i]);
		}
	}

	/** A slim rounded bar: an accent fill, or {@code colour} if given, over a darker track. */
	private abstract static class Bar extends JComponent
	{
		private final int height;
		private final Color colour;

		Bar(int height)
		{
			this(height, null);
		}

		Bar(int height, Color colour)
		{
			this.height = height;
			this.colour = colour;
			setPreferredSize(new Dimension(0, height));
			setMinimumSize(new Dimension(0, height));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		}

		/** 0 to 1. */
		abstract double fill();

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(EMPTY);
			g2.fillRoundRect(0, 0, getWidth(), height, height, height);
			int width = (int) Math.round(getWidth() * Math.max(0, Math.min(1, fill())));
			if (width > 0)
			{
				g2.setColor(colour != null ? colour : accent());
				g2.fillRoundRect(0, 0, Math.max(width, height), height, height, height);
			}
			g2.dispose();
		}
	}

	/** Progress through the current level. */
	static final class LevelBar extends Bar
	{
		private int percent;

		LevelBar()
		{
			super(8);
		}

		void setPercent(int percent)
		{
			this.percent = percent;
			repaint();
		}

		@Override
		double fill()
		{
			return percent / 100.0;
		}
	}

	private static final Color SILVER = new Color(192, 196, 204);
	private static final Color BRONZE = new Color(205, 133, 63);
	/** First place stays gold whatever the accent is. */
	private static final Color MEDAL_GOLD = DEFAULT_ACCENT;

	/** Gold, silver and bronze for the top three banks; null for the rest. */
	static Color medal(int rank)
	{
		switch (rank)
		{
			case 1:
				return MEDAL_GOLD;
			case 2:
				return SILVER;
			case 3:
				return BRONZE;
			default:
				return null;
		}
	}

	/** A bank's place in the list: a medal-coloured disc for the top three, a grey one after. */
	static final class RankBadge extends JComponent
	{
		private static final int SIZE = 18;
		private final int rank;

		RankBadge(int rank)
		{
			this.rank = rank;
			setPreferredSize(new Dimension(SIZE, SIZE));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color medal = medal(rank);
			g2.setColor(medal != null ? medal : EMPTY);
			g2.fillOval(0, 0, SIZE - 1, SIZE - 1);
			g2.setFont(FontManager.getRunescapeSmallFont());
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			String text = String.valueOf(rank);
			// Centred on the digits' drawn pixels: the font's own metrics leave room for
			// accents and descenders, which would sit the number off-centre.
			g2.setColor(medal != null ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
			centre(g2, text, 0, 0, SIZE - 1, SIZE - 1);
			g2.dispose();
		}
	}

	/** A bank's time against the top bank's, in its medal colour (the accent for the rest). */
	static final class ShareBar extends Bar
	{
		private final double share;

		ShareBar(double share, Color colour)
		{
			super(4, colour);
			this.share = share;
		}

		@Override
		double fill()
		{
			return share;
		}
	}

	/**
	 * Days stood as a month calendar: a square per day, Monday first, shaded from dim to the
	 * full accent by how long you stood, with a Less/More key underneath. Always six rows tall, so the
	 * panel doesn't jump as you page through months.
	 */
	static final class MonthCalendar extends JComponent
	{
		/** The square size before the width is known. */
		private static final int DEFAULT_CELL = 22;
		private static final int GAP = 3;
		private static final int WEEKDAY_ROW = 14;
		private static final int KEY_HEIGHT = 20;
		private static final int ROWS = 6;
		private static final String[] WEEKDAYS = {"M", "T", "W", "T", "F", "S", "S"};
		private static final Color FUTURE_TEXT = new Color(70, 70, 70);
		private static final DateTimeFormatter TOOLTIP_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
		private StatsView.Days days;
		private YearMonth month;
		/** The day picked for details, or null. */
		private LocalDate selected;
		private java.util.function.Consumer<LocalDate> onSelect = day -> {};

		MonthCalendar()
		{
			setToolTipText("");
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					LocalDate day = dayUnder(e);
					if (day == null || !days.stood(day))
					{
						return;
					}
					select(day.equals(selected) ? null : day);
					onSelect.accept(selected);
				}
			});
			// The squares grow to fill the width, so the height follows it: ask for a new
			// layout whenever the width changes the height needed.
			addComponentListener(new java.awt.event.ComponentAdapter()
			{
				@Override
				public void componentResized(java.awt.event.ComponentEvent e)
				{
					if (getHeight() != heightFor(cell()))
					{
						revalidate();
					}
				}
			});
		}

		private static int heightFor(int cell)
		{
			return WEEKDAY_ROW + ROWS * (cell + GAP) - GAP + KEY_HEIGHT;
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(7 * DEFAULT_CELL + 6 * GAP, heightFor(getWidth() > 0 ? cell() : DEFAULT_CELL));
		}

		void show(StatsView.Days days, YearMonth month)
		{
			this.days = days;
			this.month = month;
			repaint();
		}

		/** Runs with the picked day, or null when the pick is cleared. */
		void onSelect(java.util.function.Consumer<LocalDate> onSelect)
		{
			this.onSelect = onSelect;
		}

		LocalDate selected()
		{
			return selected;
		}

		void select(LocalDate day)
		{
			selected = day;
			repaint();
		}

		/** Seven squares and their gaps fill the width. */
		private int cell()
		{
			return Math.max(12, (getWidth() - 6 * GAP) / 7);
		}

		/** Centres the grid in whatever the squares' whole-pixel sizes leave over. */
		private int left()
		{
			return Math.max(0, (getWidth() - (7 * cell() + 6 * GAP)) / 2);
		}

		/** Whether a day number on this square should be dark, so it reads on any accent. */
		static boolean darkTextOn(Color square)
		{
			return 0.299 * square.getRed() + 0.587 * square.getGreen() + 0.114 * square.getBlue() > DARK_TEXT_BRIGHTNESS;
		}

		/** Empty, then the accent at rising strength over the empty colour. */
		static Color[] shades(Color accent)
		{
			double[] strength = {0.3, 0.5, 0.75, 1.0};
			Color[] shades = new Color[StatsView.CALENDAR_SHADES + 1];
			shades[0] = EMPTY;
			for (int i = 1; i < shades.length; i++)
			{
				double s = strength[i - 1];
				shades[i] = new Color(
					(int) Math.round(EMPTY.getRed() + (accent.getRed() - EMPTY.getRed()) * s),
					(int) Math.round(EMPTY.getGreen() + (accent.getGreen() - EMPTY.getGreen()) * s),
					(int) Math.round(EMPTY.getBlue() + (accent.getBlue() - EMPTY.getBlue()) * s));
			}
			return shades;
		}

		/** The date in a square of the grid, or null for a square outside the month. */
		private LocalDate dayAt(int row, int column)
		{
			int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
			int day = row * 7 + column - offset + 1;
			return day >= 1 && day <= month.lengthOfMonth() ? month.atDay(day) : null;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			if (days == null || month == null)
			{
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g2.setFont(FontManager.getRunescapeSmallFont());
			int cell = cell();
			int left = left();
			Color[] shades = shades(accent());

			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			for (int column = 0; column < 7; column++)
			{
				centre(g2, WEEKDAYS[column], left + column * (cell + GAP), 0, cell, WEEKDAY_ROW - 3);
			}

			for (int row = 0; row < ROWS; row++)
			{
				for (int column = 0; column < 7; column++)
				{
					LocalDate date = dayAt(row, column);
					if (date == null)
					{
						continue;
					}
					int x = left + column * (cell + GAP);
					int y = WEEKDAY_ROW + row * (cell + GAP);
					boolean future = date.isAfter(days.today);
					int shade = days.shade(date);
					if (!future)
					{
						g2.setColor(shades[shade]);
						g2.fillRect(x, y, cell, cell);
					}
					else
					{
						g2.setColor(new Color(44, 44, 44));
						g2.drawRect(x, y, cell - 1, cell - 1);
					}
					if (date.equals(selected))
					{
						g2.setColor(Color.WHITE);
						g2.setStroke(new BasicStroke(2));
						g2.drawRect(x + 1, y + 1, cell - 2, cell - 2);
						g2.setStroke(new BasicStroke(1));
					}
					else if (date.equals(days.today))
					{
						g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
						g2.drawRect(x, y, cell - 1, cell - 1);
					}
					g2.setColor(future ? FUTURE_TEXT : darkTextOn(shades[shade]) ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
					centre(g2, String.valueOf(date.getDayOfMonth()), x, y, cell, cell);
				}
			}

			// Key, bottom right: Less, a square of each shade, More.
			int keyCell = 9;
			int top = getHeight() - keyCell - 2;
			int right = left + 7 * cell + 6 * GAP;
			FontMetrics metrics = g2.getFontMetrics();
			int x = right - metrics.stringWidth("More");
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			centre(g2, "More", x, top, metrics.stringWidth("More"), keyCell);
			x -= 4;
			for (int shade = shades.length - 1; shade >= 0; shade--)
			{
				x -= keyCell;
				g2.setColor(shades[shade]);
				g2.fillRect(x, top, keyCell, keyCell);
				x -= 2;
			}
			x -= 2 + metrics.stringWidth("Less");
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			centre(g2, "Less", x, top, metrics.stringWidth("Less"), keyCell);
			g2.dispose();
		}

		@Override
		public String getToolTipText(MouseEvent e)
		{
			LocalDate date = dayUnder(e);
			if (date == null)
			{
				return null;
			}
			String text = date.format(TOOLTIP_DATE);
			if (!days.stood(date))
			{
				return text + ": not stood";
			}
			long ticks = days.ticks(date);
			return text + (ticks > 0 ? ": " + StatsView.duration(ticks) + " standing" : ": stood") + " \u00B7 click for banks";
		}

		/** The day under the mouse, or null for a gap, a square outside the month, or a future day. */
		private LocalDate dayUnder(MouseEvent e)
		{
			if (days == null || month == null)
			{
				return null;
			}
			int cell = cell();
			int x = e.getX() - left();
			int y = e.getY() - WEEKDAY_ROW;
			if (x < 0 || y < 0 || x % (cell + GAP) >= cell || y % (cell + GAP) >= cell)
			{
				return null;
			}
			int column = x / (cell + GAP);
			int row = y / (cell + GAP);
			if (column >= 7 || row >= ROWS)
			{
				return null;
			}
			LocalDate date = dayAt(row, column);
			return date == null || date.isAfter(days.today) ? null : date;
		}
	}

	/** Draws text centred on its drawn pixels within a box. */
	static void centre(Graphics2D g2, String text, int x, int y, int width, int height)
	{
		Rectangle ink = g2.getFont().createGlyphVector(g2.getFontRenderContext(), text)
			.getPixelBounds(g2.getFontRenderContext(), 0, 0);
		g2.drawString(text, x + (width - ink.width) / 2 - ink.x, y + (height - ink.height) / 2 - ink.y);
	}
}
