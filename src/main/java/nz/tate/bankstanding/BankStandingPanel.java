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
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;
import nz.tate.bankstanding.core.Experience;
import nz.tate.bankstanding.core.StatsView;

/**
 * Level, XP and the per-bank table, plus the credits. It only shows results, never the
 * reasons behind them. Updated from the plugin through {@link #show}, on the Swing thread.
 */
class BankStandingPanel extends PluginPanel
{
	private static final String CREDITS = "Thanks to the MERC clan, Shalli, FkXP, someusert8, Cnorthor, Zerek and Dennis.";
	/** Must match runelite-plugin.properties; a test checks it does. */
	static final String VERSION = "1.2.1";

	private static final Color GOLD = new Color(214, 176, 48);

	private final JLabel status = new JLabel();
	private final JLabel title = new JLabel();
	private final JButton capeEmote = new JButton("Cape emote");
	private final JButton collectionLog = new JButton("Collection log");
	private final JPanel stats = new JPanel(new GridLayout(0, 2, 0, 4));
	private final JLabel level = value();
	private final JLabel totalXp = value();
	private final JLabel toNext = value();
	private final JPanel sessionSection = new JPanel(new BorderLayout(0, 6));
	private final JLabel sessionXp = value();
	private final JLabel sessionTime = value();
	private final JLabel xpPerHour = value();
	private final JLabel todayTime = value();
	private final ProgressBar progressBar = new ProgressBar();
	private final JPanel bankRows = new JPanel();
	private final JPanel recordRows = new JPanel(new GridLayout(0, 2, 0, 4));
	private final JPanel recordSection = new JPanel(new BorderLayout(0, 6));

	BankStandingPanel(Icons icons, Runnable onCapeEmote, Runnable onCollectionLog)
	{
		setLayout(new BorderLayout(0, 10));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Every row of the top block shares one left edge: title, a two-column stats grid
		// (names left, values right), then the bar.
		JPanel top = new JPanel(new BorderLayout(0, 8));
		top.setOpaque(false);

		JLabel name = new JLabel("Bank Standing", new ImageIcon(icons.full()), JLabel.LEFT);
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);
		name.setIconTextGap(6);
		title.setForeground(GOLD);
		capeEmote.setFocusPainted(false);
		capeEmote.addActionListener(e -> onCapeEmote.run());
		collectionLog.setFocusPainted(false);
		collectionLog.addActionListener(e -> onCollectionLog.run());
		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
		buttons.setOpaque(false);
		buttons.add(collectionLog);
		buttons.add(capeEmote);
		JPanel heading = new JPanel(new BorderLayout(0, 2));
		heading.setOpaque(false);
		heading.add(name, BorderLayout.NORTH);
		heading.add(title, BorderLayout.CENTER);
		heading.add(buttons, BorderLayout.SOUTH);
		top.add(heading, BorderLayout.NORTH);

		stats.setOpaque(false);
		stats.add(name("Level"));
		stats.add(level);
		stats.add(name("XP"));
		stats.add(totalXp);
		stats.add(name("To next level"));
		stats.add(toNext);
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JPanel middle = new JPanel(new BorderLayout());
		middle.setOpaque(false);
		middle.add(stats, BorderLayout.NORTH);
		middle.add(status, BorderLayout.SOUTH);
		top.add(middle, BorderLayout.CENTER);

		progressBar.setForeground(GOLD);
		top.add(progressBar, BorderLayout.SOUTH);

		JPanel sessionStats = new JPanel(new GridLayout(0, 2, 0, 4));
		sessionStats.setOpaque(false);
		sessionStats.add(name("XP"));
		sessionStats.add(sessionXp);
		sessionStats.add(name("Time"));
		sessionStats.add(sessionTime);
		sessionStats.add(name("XP per hour"));
		sessionStats.add(xpPerHour);
		sessionStats.add(name("Standing today"));
		sessionStats.add(todayTime);
		sessionSection.setOpaque(false);
		sessionSection.add(heading("This session"), BorderLayout.NORTH);
		sessionSection.add(sessionStats, BorderLayout.CENTER);

		recordRows.setOpaque(false);
		recordSection.setOpaque(false);
		recordSection.add(heading("Records"), BorderLayout.NORTH);
		recordSection.add(recordRows, BorderLayout.CENTER);

		JPanel north = new JPanel(new BorderLayout(0, 12));
		north.setOpaque(false);
		north.add(top, BorderLayout.NORTH);
		JPanel middleSections = new JPanel(new BorderLayout(0, 12));
		middleSections.setOpaque(false);
		middleSections.add(sessionSection, BorderLayout.NORTH);
		middleSections.add(recordSection, BorderLayout.SOUTH);
		north.add(middleSections, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		bankRows.setLayout(new GridLayout(0, 1, 0, 4));
		bankRows.setOpaque(false);
		JPanel banks = new JPanel(new BorderLayout(0, 6));
		banks.setOpaque(false);
		banks.add(heading("Banks"), BorderLayout.NORTH);
		banks.add(bankRows, BorderLayout.CENTER);
		add(banks, BorderLayout.CENTER);

		JLabel credits = new JLabel("<html>" + CREDITS + "</html>");
		credits.setFont(FontManager.getRunescapeSmallFont());
		credits.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		JLabel version = new JLabel("v" + VERSION, SwingConstants.RIGHT);
		version.setFont(FontManager.getRunescapeSmallFont());
		version.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		JPanel footer = new JPanel(new BorderLayout(0, 2));
		footer.setOpaque(false);
		footer.add(credits, BorderLayout.CENTER);
		footer.add(version, BorderLayout.SOUTH);
		add(footer, BorderLayout.SOUTH);

		showLoggedOut();
	}

	void showLoggedOut()
	{
		status.setText("Log in to see your progress.");
		status.setVisible(true);
		title.setVisible(false);
		capeEmote.setVisible(false);
		collectionLog.setVisible(false);
		stats.setVisible(false);
		sessionSection.setVisible(false);
		recordSection.setVisible(false);
		progressBar.setValue(0);
		progressBar.setMaximumValue(100);
		progressBar.setCenterLabel("");
		bankRows.removeAll();
		revalidate();
		repaint();
	}

	void show(StatsView view)
	{
		status.setVisible(false);
		title.setText(view.title);
		title.setVisible(true);
		capeEmote.setVisible(view.level >= Experience.MAX_LEVEL);
		collectionLog.setVisible(true);
		stats.setVisible(true);
		sessionSection.setVisible(true);
		recordSection.setVisible(true);
		recordRows.removeAll();
		for (String[] record : view.records)
		{
			recordRows.add(name(record[0]));
			JLabel value = value();
			value.setText(record[1]);
			recordRows.add(value);
		}
		sessionXp.setText(view.sessionXp);
		sessionTime.setText(view.sessionTime);
		xpPerHour.setText(view.xpPerHour);
		todayTime.setText(view.todayTime);
		level.setText(String.valueOf(view.level));
		totalXp.setText(view.totalXp);
		toNext.setText(view.toNextLevel);
		progressBar.setMaximumValue(100);
		progressBar.setValue(view.percent);
		progressBar.setCenterLabel(view.percent + "%");

		bankRows.removeAll();
		if (view.banks.isEmpty())
		{
			bankRows.add(muted("Stand near any bank for 30 seconds."));
		}
		for (StatsView.BankRow row : view.banks)
		{
			bankRows.add(bankRow(row));
		}
		revalidate();
		repaint();
	}

	private static JPanel bankRow(StatsView.BankRow row)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		JLabel name = new JLabel(row.name);
		name.setForeground(Color.WHITE);
		panel.add(name, BorderLayout.NORTH);

		JPanel detail = new JPanel(new BorderLayout());
		detail.setOpaque(false);
		detail.add(muted(row.time), BorderLayout.WEST);
		JLabel xp = muted(row.xp + " xp");
		xp.setHorizontalAlignment(JLabel.RIGHT);
		detail.add(xp, BorderLayout.EAST);
		panel.add(detail, BorderLayout.SOUTH);
		return panel;
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
		return label;
	}

	private static JLabel value()
	{
		JLabel label = new JLabel();
		label.setForeground(Color.WHITE);
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
