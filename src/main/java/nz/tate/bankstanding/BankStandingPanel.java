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
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;
import nz.tate.bankstanding.core.StatsView;

/**
 * Level, XP and the per-bank table, plus the credits. It only shows results, never the
 * reasons behind them. Updated from the plugin through {@link #show}, on the Swing thread.
 */
class BankStandingPanel extends PluginPanel
{
	private static final String CREDITS = "Thanks to the MERC clan, Shalli, FkXP, someusert8, Cnorthor and Zerek.";

	private final JLabel level = new JLabel();
	private final JLabel totalXp = new JLabel();
	private final JLabel toNext = new JLabel();
	private final ProgressBar progressBar = new ProgressBar();
	private final JPanel bankRows = new JPanel();

	BankStandingPanel(Icons icons)
	{
		setLayout(new BorderLayout(0, 10));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setOpaque(false);

		JLabel title = new JLabel("Bank Standing", new ImageIcon(icons.full()), JLabel.LEFT);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		top.add(title);
		top.add(spaced(level));
		top.add(spaced(totalXp));
		top.add(spaced(toNext));
		progressBar.setForeground(new Color(214, 176, 48));
		progressBar.setBorder(new EmptyBorder(6, 0, 0, 0));
		top.add(progressBar);
		add(top, BorderLayout.NORTH);

		bankRows.setLayout(new GridLayout(0, 1, 0, 4));
		bankRows.setOpaque(false);
		JPanel banks = new JPanel(new BorderLayout(0, 6));
		banks.setOpaque(false);
		JLabel heading = new JLabel("Banks");
		heading.setFont(FontManager.getRunescapeBoldFont());
		heading.setForeground(Color.WHITE);
		banks.add(heading, BorderLayout.NORTH);
		banks.add(bankRows, BorderLayout.CENTER);
		add(banks, BorderLayout.CENTER);

		JLabel credits = new JLabel("<html>" + CREDITS + "</html>");
		credits.setFont(FontManager.getRunescapeSmallFont());
		credits.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		add(credits, BorderLayout.SOUTH);

		showLoggedOut();
	}

	void showLoggedOut()
	{
		level.setText("Log in to see your progress.");
		totalXp.setText(" ");
		toNext.setText(" ");
		progressBar.setValue(0);
		progressBar.setMaximumValue(100);
		progressBar.setCenterLabel("");
		bankRows.removeAll();
		revalidate();
		repaint();
	}

	void show(StatsView view)
	{
		level.setText("Level " + view.level);
		totalXp.setText("XP: " + view.totalXp);
		toNext.setText("To next level: " + view.toNextLevel);
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

		JLabel detail = muted(row.time + "   " + row.xp + " xp");
		panel.add(detail, BorderLayout.SOUTH);
		return panel;
	}

	private static JLabel spaced(JLabel label)
	{
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(4, 0, 0, 0));
		return label;
	}

	private static JLabel muted(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return label;
	}
}
