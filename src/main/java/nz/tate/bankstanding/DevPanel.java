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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import nz.tate.bankstanding.core.CollectionLog;
import nz.tate.bankstanding.core.Experience;
import nz.tate.bankstanding.core.Keywords;

/**
 * Testing tools, shown as a second sidebar button only when RuneLite runs in developer mode.
 * Buttons that show something (splats, effects) award no XP; the progress buttons change the
 * real save for the logged-in account.
 */
class DevPanel extends PluginPanel
{
	DevPanel(BankStandingPlugin plugin, DevOptions dev)
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		add(body, BorderLayout.NORTH);

		body.add(heading("Hitsplats"));
		body.add(buttons(
			button("Normal", () -> plugin.devSplat(false, false, null)),
			button("Max hit", () -> plugin.devSplat(true, false, null)),
			button("Boosted", () -> plugin.devSplat(false, true, null)),
			button("Phrase: anywhere", () -> plugin.devSplat(false, false, Keywords.Kind.ANYWHERE)),
			button("Phrase: bank", () -> plugin.devSplat(false, false, Keywords.Kind.LOCATION)),
			button("Phrase: date", () -> plugin.devSplat(false, false, Keywords.Kind.DATE)),
			button("Phrase: date + bank", () -> plugin.devSplat(false, false, Keywords.Kind.DATE_AND_LOCATION))));

		JSpinner sprite = new JSpinner(new SpinnerNumberModel(1359, 0, 99999, 1));
		body.add(row(new JLabel("Sprite ID"), sprite));
		body.add(buttons(button("Show sprite", () -> plugin.devSprite((Integer) sprite.getValue()))));

		body.add(heading("Effects"));
		body.add(buttons(
			button("Level-up fireworks", plugin::devFireworks),
			button("99 celebration", plugin::devCelebrate),
			button("Cape emote", plugin::capeEmote)));

		body.add(heading("Cape"));
		JCheckBox cape = new JCheckBox("Show the 99 cape at any level", dev.previewCape);
		cape.setOpaque(false);
		cape.addActionListener(e -> plugin.devPreviewCape(cape.isSelected()));
		body.add(cape);
		JComboBox<String> capeBase = new JComboBox<>();
		for (int base : CapeDisplay.BASES)
		{
			capeBase.addItem(CapeDisplay.name(base));
		}
		capeBase.setSelectedItem(CapeDisplay.name(CapeDisplay.DEFAULT_BASE));
		capeBase.addActionListener(e -> dev.capeBase = CapeDisplay.BASES[capeBase.getSelectedIndex()]);
		body.add(row(new JLabel("Cape base"), capeBase));

		body.add(heading("Standing unlocks"));
		JCheckBox pretend = new JCheckBox("Act as if standing (rug, seat, fidgets, bankers)", dev.pretendStanding);
		pretend.setOpaque(false);
		pretend.addActionListener(e -> dev.pretendStanding = pretend.isSelected());
		body.add(pretend);

		body.add(heading("Collection log"));
		JComboBox<String> drop = new JComboBox<>();
		for (CollectionLog.Drop each : CollectionLog.DROPS)
		{
			drop.addItem(each.name);
		}
		body.add(drop);
		body.add(buttons(
			button("Give drop", () -> plugin.devGiveDrop(CollectionLog.DROPS.get(drop.getSelectedIndex()).key)),
			button("Give next missing", plugin::devGiveNextDrop),
			button("Test popup", plugin::devTestPopup),
			button("Open log", plugin::openCollectionLog),
			button("Reset log", plugin::devResetCollection)));
		JCheckBox pet = new JCheckBox("Show the pet without owning it", dev.previewPet);
		pet.setOpaque(false);
		pet.addActionListener(e -> plugin.devPreviewPet(pet.isSelected()));
		body.add(pet);

		body.add(heading("Progress (changes your save)"));
		JSpinner level = new JSpinner(new SpinnerNumberModel(99, 1, Experience.MAX_LEVEL, 1));
		body.add(row(new JLabel("Level"), level));
		body.add(buttons(
			button("Set level", () -> plugin.devSetLevel((Integer) level.getValue())),
			button("One drop to 99", () -> plugin.devSetXp(Experience.xpForLevel(Experience.MAX_LEVEL) - 1)),
			button("Reset progress", plugin::devReset),
			button("Check records", plugin::devCheckRecords)));

		JSpinner multiplier = new JSpinner(new SpinnerNumberModel(dev.xpMultiplier, 1, 1000, 1));
		multiplier.addChangeListener(e -> dev.xpMultiplier = (Integer) multiplier.getValue());
		body.add(row(new JLabel("XP multiplier"), multiplier));
	}

	private static JLabel heading(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setBorder(new EmptyBorder(10, 0, 4, 0));
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JPanel buttons(JButton... buttons)
	{
		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		for (JButton button : buttons)
		{
			panel.add(button);
		}
		panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		return panel;
	}

	private static JPanel row(JLabel label, JComponent spinner)
	{
		JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		panel.add(label, BorderLayout.WEST);
		panel.add(spinner, BorderLayout.CENTER);
		return panel;
	}

	private static JButton button(String text, Runnable action)
	{
		JButton button = new JButton(text);
		button.addActionListener(e -> action.run());
		return button;
	}
}
