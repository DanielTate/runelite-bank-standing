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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import nz.tate.bankstanding.core.BankOfTheDay;
import nz.tate.bankstanding.core.Experience;
import nz.tate.bankstanding.core.StatsView;

/**
 * Level, XP and the per-bank table, plus the credits. It only shows results, never the
 * reasons behind them. Updated from the plugin through {@link #show}, on the Swing thread.
 *
 * <p>Each section is a card: a darker box with a heading row, then name/value rows.
 */
class BankStandingPanel extends PluginPanel
{
	private static final String CREDITS = "Thanks to the MERC clan, Shalli, FkXP, someusert8, Cnorthor, Zerek, Dennis and "
		+ "Hochep, and to WyattSociety and GarlikStixx on Twitch.";
	/** Must match runelite-plugin.properties; a test checks it does. */
	static final String VERSION = "1.10";
	static final String CHANGELOG_URL = "https://github.com/DanielTate/runelite-bank-standing/blob/main/CHANGELOG.md";

	private static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color DIVIDER = ColorScheme.DARK_GRAY_COLOR;
	/** A 1px edge around each card, a step lighter than the page, so the cards stand out. */
	private static final Color CARD_EDGE = new Color(52, 52, 52);
	private static final int GAP = 8;
	/** How many banks show before "Show all". */
	private static final int TOP_BANKS = 3;
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

	private final JLabel status = new JLabel();
	private final JLabel title = new PanelGraphs.AccentLabel();
	private final JLabel bigLevel = new PanelGraphs.AccentLabel("", SwingConstants.RIGHT);
	private final JLabel bigLevelCaption = new JLabel("Level", SwingConstants.RIGHT);
	private final JButton capeEmote = new JButton("Cape emote");
	private final JButton collectionLog = new JButton("Collection log");
	private final JButton skillGuide = new JButton("Skill guide");
	private final JCheckBox showSkillTile = new JCheckBox("Show skill tile");
	private final JButton shareImage = new JButton("Copy as image");
	/** Whatever wants the panel's picture: copied and saved by the plugin. */
	private final Consumer<BufferedImage> onShareImage;
	/** The header and the cards, in order, for the picture; the buttons are left out. */
	private final java.util.List<JComponent> pictured = new java.util.ArrayList<>();
	private final JPanel bankOfDayCard;
	private final JLabel bankOfDay = new PanelGraphs.AccentLabel();
	private final JLabel bankOfDayRegion = small("");
	private final JLabel bankOfDayLeft = small("");
	private final JPanel wastedCard;
	private final JLabel standingShare = new PanelGraphs.AccentLabel();
	private final JLabel wastedShare = small("");
	private final PanelGraphs.SplitBar wastedBar = new PanelGraphs.SplitBar();
	private final JLabel xpWasted = new PanelGraphs.AccentLabel();
	private final JLabel wastedSince = small("");
	private final JPanel progressCard;
	private final JLabel totalXp = value();
	private final JLabel toNext = value();
	private final JPanel timeToLevelRow;
	private final JLabel timeToLevel = value();
	private final JLabel nextLevel = muted("");
	private final JLabel percent = value();
	private final PanelGraphs.LevelBar levelBar = new PanelGraphs.LevelBar();
	private final JPanel sessionCard;
	private final JLabel sessionXp = value();
	private final JLabel sessionTime = value();
	private final JLabel xpPerHour = value();
	private final JLabel todayTime = value();
	private final PanelGraphs.SessionXp sessionGraph = new PanelGraphs.SessionXp();
	private final JPanel daysCard;
	private final JLabel daysCount = muted("");
	private final PanelGraphs.MonthCalendar calendar = new PanelGraphs.MonthCalendar();
	private final JLabel monthName = new JLabel("", SwingConstants.CENTER);
	private final JButton previousMonth = arrow("\u2039", "Previous month");
	private final JButton nextMonth = arrow("\u203A", "Next month");
	/** The latest days stood, and the month being shown; null until the first {@link #show}. */
	private StatsView.Days days;
	private YearMonth shownMonth;
	/** Under the calendar: the banks stood at on the picked day. */
	private final JPanel dayDetail = column(5);
	private final JPanel recordTiles = new JPanel(new GridLayout(0, 2, 6, 6));
	private final JPanel recordsCard;
	private final JPanel recordRows = rows();
	private final JPanel banksCard;
	private final JLabel banksCount = muted("");
	private final JPanel bankRows = new JPanel();
	/** Under the top banks: expands the list to every bank, or folds it back. */
	private final JLabel moreBanks = new PanelGraphs.AccentLabel("", SwingConstants.CENTER);
	private boolean allBanks;
	private java.util.List<StatsView.BankRow> banks = java.util.Collections.emptyList();

	/**
	 * {@code changelogUnseen} is whether this version's changelog hasn't been opened yet;
	 * {@code onChangelogSeen} runs when it is. {@code onShowSkillTile} gets the checkbox's new
	 * state whenever it's clicked. {@code onResetSession} starts "This session" again from zero.
	 */
	BankStandingPanel(Icons icons, Runnable onCapeEmote, Runnable onCollectionLog, Runnable onSkillGuide,
		boolean skillTileShown, Consumer<Boolean> onShowSkillTile, boolean changelogUnseen, Runnable onChangelogSeen,
		Runnable onResetSession, Consumer<BufferedImage> onShareImage)
	{
		this.onShareImage = onShareImage;
		setLayout(new BorderLayout(0, GAP));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header: the icon, then the name, title and "What's new" stacked beside it, and the
		// level large on the right.
		JLabel icon = new JLabel(new ImageIcon(icons.full()));
		icon.setVerticalAlignment(SwingConstants.TOP);
		JLabel name = new JLabel("Bank Standing");
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JLabel whatsNew = new JLabel(whatsNewText(changelogUnseen));
		whatsNew.setFont(FontManager.getRunescapeSmallFont());
		whatsNew.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		whatsNew.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		whatsNew.setToolTipText("Open the changelog");
		whatsNew.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				LinkBrowser.browse(CHANGELOG_URL);
				whatsNew.setText(whatsNewText(false));
				onChangelogSeen.run();
			}
		});
		JPanel names = column(1);
		names.add(name);
		names.add(title);
		names.add(status);
		names.add(whatsNew);
		JPanel identity = new JPanel(new BorderLayout(GAP, 0));
		identity.setOpaque(false);
		identity.add(icon, BorderLayout.WEST);
		identity.add(names, BorderLayout.CENTER);
		bigLevel.setFont(FontManager.getRunescapeBoldFont().deriveFont(28f));
		bigLevelCaption.setFont(FontManager.getRunescapeSmallFont());
		bigLevelCaption.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel levelBox = new JPanel(new BorderLayout());
		levelBox.setOpaque(false);
		levelBox.add(bigLevelCaption, BorderLayout.NORTH);
		levelBox.add(bigLevel, BorderLayout.CENTER);
		JPanel header = new JPanel(new BorderLayout(GAP, 0));
		header.setOpaque(false);
		header.add(identity, BorderLayout.CENTER);
		header.add(levelBox, BorderLayout.EAST);

		for (JButton button : new JButton[]{capeEmote, collectionLog, skillGuide, shareImage})
		{
			button.setFocusPainted(false);
			button.setMargin(new java.awt.Insets(4, 2, 4, 2));
		}
		capeEmote.addActionListener(e -> onCapeEmote.run());
		collectionLog.addActionListener(e -> onCollectionLog.run());
		skillGuide.addActionListener(e -> onSkillGuide.run());
		shareImage.setToolTipText("Copy a picture of this panel to share, and save it in your screenshots folder");
		shareImage.addActionListener(e -> shareImage());
		showSkillTile.setOpaque(false);
		showSkillTile.setFocusPainted(false);
		showSkillTile.setSelected(skillTileShown);
		showSkillTile.addActionListener(e -> onShowSkillTile.accept(showSkillTile.isSelected()));
		JPanel pair = new JPanel(new GridLayout(1, 2, 4, 0));
		pair.setOpaque(false);
		pair.add(skillGuide);
		pair.add(collectionLog);
		JPanel actions = column(4);
		actions.add(pair);
		actions.add(stretch(capeEmote));
		actions.add(stretch(shareImage));
		actions.add(showSkillTile);

		// Bank of the day: the bank large, its region and when it changes under it.
		bankOfDay.setFont(FontManager.getRunescapeBoldFont());
		JPanel bankOfDayBody = column(2);
		bankOfDayBody.add(bankOfDay);
		bankOfDayBody.add(line(bankOfDayRegion, bankOfDayLeft));
		JLabel bonus = small(BankOfTheDay.BONUS);
		bankOfDayCard = card("Bank of the day", bonus, bankOfDayBody, null);
		bankOfDayCard.setToolTipText("Every player has the same bank. It changes at midnight UTC.");

		// Progress: XP rows, then the bar with the next level and percent above it.
		JPanel progressRows = rows();
		progressRows.add(row("XP", totalXp));
		progressRows.add(row("To next level", toNext));
		timeToLevelRow = row("Time to level", timeToLevel);
		progressRows.add(timeToLevelRow);
		JPanel barLabels = new JPanel(new BorderLayout());
		barLabels.setOpaque(false);
		barLabels.add(nextLevel, BorderLayout.WEST);
		barLabels.add(percent, BorderLayout.EAST);
		JPanel bar = column(4);
		bar.add(barLabels);
		bar.add(levelBar);
		progressCard = card("Progress", null, progressRows, bar);

		JPanel sessionRows = rows();
		sessionRows.add(row("XP", sessionXp));
		sessionRows.add(row("Time", sessionTime));
		sessionRows.add(row("XP per hour", xpPerHour));
		sessionRows.add(row("Standing today", todayTime));
		JButton resetSession = new JButton("Reset");
		resetSession.setFocusPainted(false);
		resetSession.setFont(FontManager.getRunescapeSmallFont());
		resetSession.setMargin(new java.awt.Insets(0, 6, 0, 6));
		resetSession.setToolTipText("Start this session's XP, time and XP per hour again from zero");
		resetSession.addActionListener(e -> onResetSession.run());
		sessionCard = card("This session", resetSession, sessionRows, sessionGraph);

		monthName.setFont(FontManager.getRunescapeBoldFont());
		monthName.setForeground(Color.WHITE);
		previousMonth.addActionListener(e -> showMonth(shownMonth.minusMonths(1)));
		nextMonth.addActionListener(e -> showMonth(shownMonth.plusMonths(1)));
		JPanel monthBar = new JPanel(new BorderLayout(GAP, 0));
		monthBar.setOpaque(false);
		monthBar.add(previousMonth, BorderLayout.WEST);
		monthBar.add(monthName, BorderLayout.CENTER);
		monthBar.add(nextMonth, BorderLayout.EAST);
		JPanel month = new JPanel(new BorderLayout(0, 6));
		month.setOpaque(false);
		month.add(monthBar, BorderLayout.NORTH);
		month.add(calendar, BorderLayout.CENTER);
		month.add(dayDetail, BorderLayout.SOUTH);
		dayDetail.setVisible(false);
		calendar.onSelect(day -> showDay());
		daysCount.setFont(FontManager.getRunescapeSmallFont());
		daysCard = card("Days stood", daysCount, month, null);
		recordTiles.setOpaque(false);
		recordsCard = card("Records", null, recordTiles, recordRows);

		// XP wasted: the green/red split with percentages, then the big number.
		standingShare.setFont(FontManager.getRunescapeSmallFont());
		xpWasted.setFont(FontManager.getRunescapeBoldFont().deriveFont(24f));
		JPanel wastedBody = column(4);
		wastedBody.add(wastedBar);
		wastedBody.add(line(standingShare, wastedShare));
		wastedBody.add(xpWasted);
		wastedBody.add(wastedSince);
		JLabel efficiency = small("Efficiency");
		wastedCard = card("XP wasted", efficiency, wastedBody, null);
		wastedCard.setToolTipText("The Bank Standing XP you'd have earned if you'd spent your other logged-in time at a bank");

		bankRows.setLayout(new BoxLayout(bankRows, BoxLayout.Y_AXIS));
		bankRows.setOpaque(false);
		moreBanks.setFont(FontManager.getRunescapeSmallFont());
		moreBanks.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		moreBanks.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER), new EmptyBorder(6, 0, 0, 0)));
		moreBanks.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				allBanks = !allBanks;
				showBanks();
			}
		});
		banksCard = card("Banks", banksCount, bankRows, moreBanks);

		pictured.add(header);
		pictured.add(bankOfDayCard);
		pictured.add(progressCard);
		pictured.add(sessionCard);
		pictured.add(daysCard);
		pictured.add(recordsCard);
		pictured.add(wastedCard);
		pictured.add(banksCard);

		JPanel sections = column(GAP);
		sections.add(header);
		sections.add(actions);
		sections.add(bankOfDayCard);
		sections.add(progressCard);
		sections.add(sessionCard);
		sections.add(daysCard);
		sections.add(recordsCard);
		sections.add(wastedCard);
		sections.add(banksCard);
		add(sections, BorderLayout.NORTH);

		// A text area, not a label: it wraps by the width the text really draws at.
		JTextArea credits = new JTextArea(CREDITS);
		credits.setLineWrap(true);
		credits.setWrapStyleWord(true);
		credits.setEditable(false);
		credits.setFocusable(false);
		credits.setOpaque(false);
		credits.setBorder(null);
		credits.setFont(FontManager.getRunescapeSmallFont());
		credits.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		// Its height is worked out for the width it has, so give it the panel's width up front.
		credits.setSize(PANEL_WIDTH - 20, Short.MAX_VALUE);
		JPanel footer = new JPanel(new BorderLayout(0, 4));
		footer.setOpaque(false);
		footer.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.DARKER_GRAY_COLOR),
			new EmptyBorder(GAP, 0, 0, 0)));
		footer.add(credits, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);

		showLoggedOut();
	}

	/**
	 * The changelog link: "What's new in v1.8" in gold until this version's changelog has
	 * been opened, then a quiet "v1.8 changelog".
	 */
	private static String whatsNewText(boolean unseen)
	{
		return unseen ? "<html><font color='#d6b030'>What's new in v" + VERSION + "</font></html>"
			: "<html>v" + VERSION + " changelog</html>";
	}

	/** Twice the size, so it stays sharp when shared. */
	private static final int PICTURE_SCALE = 2;

	/**
	 * The header and every card that's showing, stacked as they are on screen, with a small
	 * caption at the bottom, and hands it on to be copied and saved.
	 */
	private void shareImage()
	{
		int width = 0;
		int height = GAP;
		java.util.List<JComponent> shown = new java.util.ArrayList<>();
		for (JComponent part : pictured)
		{
			if (part.isVisible() && part.getHeight() > 0)
			{
				shown.add(part);
				width = Math.max(width, part.getWidth());
				height += part.getHeight() + GAP;
			}
		}
		if (shown.isEmpty())
		{
			return;
		}
		JLabel caption = small("Bank Standing \u00B7 " + LocalDate.now().format(DAY));
		Dimension captionSize = caption.getPreferredSize();
		caption.setSize(captionSize);
		height += captionSize.height + GAP;
		width += 2 * GAP;

		BufferedImage image = new BufferedImage(width * PICTURE_SCALE, height * PICTURE_SCALE, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = image.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.scale(PICTURE_SCALE, PICTURE_SCALE);
		g.setColor(ColorScheme.DARK_GRAY_COLOR);
		g.fillRect(0, 0, width, height);
		int y = GAP;
		for (JComponent part : shown)
		{
			java.awt.Graphics2D at = (java.awt.Graphics2D) g.create(GAP, y, part.getWidth(), part.getHeight());
			part.printAll(at);
			at.dispose();
			y += part.getHeight() + GAP;
		}
		java.awt.Graphics2D at = (java.awt.Graphics2D) g.create(width - GAP - captionSize.width, y, captionSize.width, captionSize.height);
		caption.print(at);
		at.dispose();
		g.dispose();

		onShareImage.accept(image);
		shareImage.setText("Copied!");
		javax.swing.Timer reset = new javax.swing.Timer(2000, e -> shareImage.setText("Copy as image"));
		reset.setRepeats(false);
		reset.start();
	}

	/** Keeps the checkbox in step when the setting changes from the config panel. */
	void setSkillTileShown(boolean shown)
	{
		showSkillTile.setSelected(shown);
	}

	void showLoggedOut()
	{
		status.setText("Log in to see your progress.");
		status.setVisible(true);
		title.setVisible(false);
		bigLevel.setVisible(false);
		bigLevelCaption.setVisible(false);
		capeEmote.setVisible(false);
		collectionLog.setVisible(false);
		skillGuide.setVisible(false);
		shareImage.setVisible(false);
		bankOfDayCard.setVisible(false);
		wastedCard.setVisible(false);
		progressCard.setVisible(false);
		sessionCard.setVisible(false);
		daysCard.setVisible(false);
		recordsCard.setVisible(false);
		banksCard.setVisible(false);
		bankRows.removeAll();
		revalidate();
		repaint();
	}

	void show(StatsView view)
	{
		status.setVisible(false);
		title.setText(view.title);
		title.setVisible(true);
		bigLevel.setText(String.valueOf(view.level));
		bigLevel.setVisible(true);
		bigLevelCaption.setVisible(true);
		capeEmote.setVisible(view.level >= Experience.MAX_LEVEL);
		collectionLog.setVisible(true);
		skillGuide.setVisible(true);
		shareImage.setVisible(true);
		bankOfDayCard.setVisible(true);
		bankOfDay.setText(view.bankOfDay);
		bankOfDayRegion.setText(view.bankOfDayRegion == null ? "" : view.bankOfDayRegion);
		bankOfDayLeft.setText("Changes in " + view.bankOfDayLeft);
		wastedCard.setVisible(view.showXpWasted);
		wastedBar.setStanding(view.standingPercent);
		standingShare.setText(view.standingPercent < 0 ? "Standing" : view.standingPercent + "% standing");
		wastedShare.setText(view.standingPercent < 0 ? "Wasted" : 100 - view.standingPercent + "% wasted");
		xpWasted.setText(view.xpWasted);
		wastedSince.setText(view.wastedSince == null ? "" : "Since " + view.wastedSince);
		progressCard.setVisible(true);
		sessionCard.setVisible(true);
		daysCard.setVisible(true);
		recordsCard.setVisible(true);
		banksCard.setVisible(true);

		// The four headline records as tiles; the start date and milestones as rows under them.
		recordTiles.removeAll();
		recordRows.removeAll();
		for (String[] record : view.records)
		{
			if (TILED_RECORDS.contains(record[0]))
			{
				recordTiles.add(tile(record[0].equals("Best XP per hour") ? "Best XP/hr" : record[0], record[1]));
				continue;
			}
			JLabel value = record[0].startsWith("Reached") ? accentValue() : value();
			value.setText(record[1]);
			recordRows.add(row(record[0], value));
		}
		sessionXp.setText(view.sessionXp);
		sessionTime.setText(view.sessionTime);
		xpPerHour.setText(view.xpPerHour);
		todayTime.setText(view.todayTime);
		sessionGraph.setSamples(view.sessionXpSamples);
		days = view.days;
		YearMonth thisMonth = YearMonth.from(days.today);
		showMonth(shownMonth == null || shownMonth.isAfter(thisMonth) ? thisMonth : shownMonth);

		totalXp.setText(view.totalXp);
		toNext.setText(view.toNextLevel);
		timeToLevelRow.setVisible(view.timeToLevel != null);
		timeToLevel.setText(view.timeToLevel);
		boolean maxed = view.level >= Experience.MAX_LEVEL;
		nextLevel.setText(maxed ? "Maxed" : "Level " + (view.level + 1));
		percent.setText(view.percent + "%");
		levelBar.setPercent(view.percent);

		banks = view.banks;
		banksCount.setText(banks.size() == 1 ? "1 bank" : banks.size() + " banks");
		showBanks();
		revalidate();
		repaint();
	}

	/** The top banks, or every bank once "Show all" is clicked, with the toggle under them. */
	private void showBanks()
	{
		bankRows.removeAll();
		if (banks.isEmpty())
		{
			bankRows.add(muted("Stand near any bank for 30 seconds."));
		}
		int shown = allBanks ? banks.size() : Math.min(TOP_BANKS, banks.size());
		for (int i = 0; i < shown; i++)
		{
			bankRows.add(bankRow(banks.get(i), i > 0));
		}
		moreBanks.setVisible(banks.size() > TOP_BANKS);
		moreBanks.setText(allBanks ? "Show top " + TOP_BANKS + " \u25B4" : "Show all " + banks.size() + " banks \u25BE");
		bankRows.revalidate();
		bankRows.repaint();
	}

	/** Pages the calendar to a month, kept between the first month stood and this month. */
	private void showMonth(YearMonth month)
	{
		if (days == null)
		{
			return;
		}
		YearMonth thisMonth = YearMonth.from(days.today);
		YearMonth first = days.first;
		shownMonth = month.isBefore(first) ? first : month.isAfter(thisMonth) ? thisMonth : month;
		monthName.setText(shownMonth.format(MONTH));
		previousMonth.setEnabled(shownMonth.isAfter(first));
		nextMonth.setEnabled(shownMonth.isBefore(thisMonth));
		calendar.show(days, shownMonth);
		LocalDate picked = calendar.selected();
		if (picked != null && !YearMonth.from(picked).equals(shownMonth))
		{
			calendar.select(null);
		}
		showDay();
		int stood = days.daysStoodIn(shownMonth);
		long ticks = days.ticksIn(shownMonth);
		daysCount.setText((stood == 1 ? "1 day" : stood + " days") + (ticks > 0 ? " \u00B7 " + StatsView.duration(ticks) : ""));
	}

	private static final java.util.Set<String> TILED_RECORDS = new java.util.HashSet<>(java.util.Arrays.asList(
		"Total standing", "Longest stand", "Best day", "Best XP per hour"));
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);

	/** A record as a tile: the value large, its name small underneath. */
	private static JPanel tile(String name, String value)
	{
		JPanel tile = new PanelGraphs.Rounded(new BorderLayout(0, 2), ColorScheme.DARK_GRAY_COLOR, null, true);
		tile.setBorder(new EmptyBorder(6, 9, 6, 4));
		JLabel big = new JLabel(value);
		big.setFont(FontManager.getRunescapeBoldFont());
		big.setForeground(Color.WHITE);
		JLabel label = new JLabel(name);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setMinimumSize(new Dimension(0, 0));
		tile.add(big, BorderLayout.NORTH);
		tile.add(label, BorderLayout.SOUTH);
		return tile;
	}

	/** Fills the panel under the calendar with the picked day's banks, or hides it. */
	private void showDay()
	{
		LocalDate day = calendar.selected();
		dayDetail.removeAll();
		dayDetail.setVisible(day != null && days != null);
		if (day == null || days == null)
		{
			dayDetail.revalidate();
			return;
		}
		dayDetail.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER), new EmptyBorder(GAP, 0, 0, 0)));
		JLabel date = new JLabel(day.format(DAY));
		date.setFont(FontManager.getRunescapeBoldFont());
		date.setForeground(Color.WHITE);
		date.setMinimumSize(new Dimension(0, 0));
		long ticks = days.ticks(day);
		JLabel total = accentValue();
		total.setText(ticks > 0 ? StatsView.duration(ticks) : "");
		dayDetail.add(line(date, total));
		java.util.List<StatsView.DayBank> banks = days.banks(day);
		if (banks.isEmpty())
		{
			dayDetail.add(small("Which banks wasn't recorded before 1.8."));
		}
		for (StatsView.DayBank bank : banks)
		{
			JLabel name = new JLabel(bank.name);
			name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			name.setMinimumSize(new Dimension(0, 0));
			JLabel time = value();
			time.setText(bank.time);
			JPanel entry = column(3);
			entry.add(line(name, time));
			entry.add(new PanelGraphs.ShareBar(bank.share, null));
			dayDetail.add(entry);
		}
		dayDetail.revalidate();
		dayDetail.repaint();
	}

	/** A small month-paging button. */
	private static JButton arrow(String text, String tooltip)
	{
		JButton button = new JButton(text);
		button.setFocusPainted(false);
		button.setFont(FontManager.getRunescapeBoldFont());
		button.setMargin(new java.awt.Insets(0, 6, 0, 6));
		button.setToolTipText(tooltip);
		return button;
	}

	/**
	 * A bank, as a leaderboard entry: a rank badge, then the name and time, the region and
	 * share of all bank time, a bar against the top bank, and the XP and visits.
	 */
	private static JPanel bankRow(StatsView.BankRow row, boolean divider)
	{
		JPanel panel = new JPanel(new BorderLayout(GAP, 0));
		panel.setOpaque(false);
		panel.setBorder(divider
			? BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER), new EmptyBorder(7, 0, 7, 0))
			: new EmptyBorder(0, 0, 7, 0));

		Color medal = PanelGraphs.medal(row.rank);
		JPanel badgeColumn = new JPanel(new BorderLayout());
		badgeColumn.setOpaque(false);
		badgeColumn.add(new PanelGraphs.RankBadge(row.rank), BorderLayout.NORTH);

		JLabel name = new JLabel(row.name);
		name.setForeground(Color.WHITE);
		name.setMinimumSize(new Dimension(0, 0));
		JLabel time = value();
		time.setText(row.time);
		if (medal != null)
		{
			time.setForeground(medal);
		}
		JPanel first = line(name, time);

		JLabel region = small(row.region != null ? row.region : "Unlisted bank");
		JLabel percent = small(row.percent + " of time");
		JPanel second = line(region, percent);

		PanelGraphs.ShareBar share = new PanelGraphs.ShareBar(row.share, medal);

		JLabel xp = small(row.xp + " xp");
		JLabel visits = small(row.visits != null ? row.visits : "");
		JPanel fourth = line(xp, visits);

		JPanel details = column(3);
		details.add(first);
		details.add(second);
		details.add(share);
		details.add(fourth);

		panel.add(badgeColumn, BorderLayout.WEST);
		panel.add(details, BorderLayout.CENTER);
		if (row.firstStood != null)
		{
			panel.setToolTipText("First stood here " + row.firstStood);
		}
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	/** Two labels on one line, one at each end; the left one gives way if space runs out. */
	private static JPanel line(JLabel left, JLabel right)
	{
		JPanel line = new JPanel(new BorderLayout(GAP, 0));
		line.setOpaque(false);
		line.add(left, BorderLayout.CENTER);
		right.setHorizontalAlignment(JLabel.RIGHT);
		line.add(right, BorderLayout.EAST);
		line.setMaximumSize(new Dimension(Integer.MAX_VALUE, line.getPreferredSize().height));
		return line;
	}

	private static JLabel small(String text)
	{
		JLabel label = muted(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setMinimumSize(new Dimension(0, 0));
		return label;
	}

	/** A section: a darker box with a heading (and something on the right of it), a body, and an optional footer. */
	private static JPanel card(String heading, JComponent accessory, JComponent body, JComponent footer)
	{
		JPanel card = new PanelGraphs.Rounded(new BorderLayout(0, GAP), CARD, CARD_EDGE, false);
		card.setBorder(new EmptyBorder(GAP + 1, GAP + 1, GAP + 1, GAP + 1));
		JPanel top = new JPanel(new BorderLayout(GAP, 0));
		top.setOpaque(false);
		top.add(heading(heading), BorderLayout.WEST);
		if (accessory != null)
		{
			top.add(accessory, BorderLayout.EAST);
		}
		card.add(top, BorderLayout.NORTH);
		card.add(body, BorderLayout.CENTER);
		if (footer != null)
		{
			card.add(footer, BorderLayout.SOUTH);
		}
		return card;
	}

	/** Name on the left, value on the right; the name gives way first if space runs out. */
	private static JPanel row(String name, JLabel value)
	{
		JPanel row = new JPanel(new BorderLayout(GAP, 0));
		row.setOpaque(false);
		row.add(name(name), BorderLayout.CENTER);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		row.add(value, BorderLayout.EAST);
		return row;
	}

	/** Name/value rows, one under another; hidden rows take no space. */
	private static JPanel rows()
	{
		return column(3);
	}

	/** A top-to-bottom stack that leaves no gap for hidden children. */
	private static JPanel column(int gap)
	{
		JPanel column = new JPanel()
		{
			@Override
			public Component add(Component c)
			{
				if (gap > 0 && getComponentCount() > 0)
				{
					super.add(new Spacer(gap, (JComponent) c));
				}
				((JComponent) c).setAlignmentX(LEFT_ALIGNMENT);
				return super.add(c);
			}
		};
		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setOpaque(false);
		return column;
	}

	/** A gap in a column that disappears along with the component after it. */
	private static final class Spacer extends JComponent
	{
		private final int size;
		private final JComponent next;

		Spacer(int size, JComponent next)
		{
			this.size = size;
			this.next = next;
			setAlignmentX(LEFT_ALIGNMENT);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(0, next.isVisible() ? size : 0);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}
	}

	/** Lets a button fill the column's width. */
	private static JComponent stretch(JButton button)
	{
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		return button;
	}

	private static JLabel heading(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		return label;
	}

	private static JLabel name(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setMinimumSize(new Dimension(0, 0));
		return label;
	}

	private static JLabel value()
	{
		JLabel label = new JLabel();
		label.setForeground(Color.WHITE);
		label.setHorizontalAlignment(JLabel.RIGHT);
		return label;
	}

	private static JLabel accentValue()
	{
		JLabel label = new PanelGraphs.AccentLabel();
		label.setHorizontalAlignment(JLabel.RIGHT);
		return label;
	}

	private static JLabel muted(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}
}
