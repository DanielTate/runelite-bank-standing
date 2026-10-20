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
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
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

		body.add(heading("Tea delivery"));
		JComboBox<String> teaBanker = new JComboBox<>();
		teaBanker.addItem("Any (as in game)");
		teaBanker.addItem("This bank's banker");
		teaBanker.addItem("Classic banker");
		for (String bank : TeaDelivery.visitorBanks())
		{
			teaBanker.addItem(bank);
		}
		body.add(wide(teaBanker));
		body.add(buttons(button("Send tea", () ->
		{
			int chosen = teaBanker.getSelectedIndex();
			plugin.devTea(chosen == 0 ? null : chosen == 1 ? TeaDelivery.LOCAL : chosen == 2 ? TeaDelivery.CLASSIC
				: (String) teaBanker.getSelectedItem());
		})));

		body.add(heading("Tea cup tuner"));
		JComboBox<String> tuneBanker = new JComboBox<>();
		for (String choice : TeaDelivery.tunerChoices())
		{
			tuneBanker.addItem(choice);
		}
		body.add(wide(tuneBanker));
		JSpinner height = new JSpinner(new SpinnerNumberModel(0, -400, 100, 1));
		JSpinner forward = new JSpinner(new SpinnerNumberModel(0, -200, 200, 1));
		JSpinner side = new JSpinner(new SpinnerNumberModel(0, -200, 200, 1));
		body.add(row(new JLabel("Height (up is -)"), height));
		body.add(row(new JLabel("Forward (-)/back (+)"), forward));
		body.add(row(new JLabel("Left/right"), side));
		JTextArea tuned = new JTextArea(4, 20);
		tuned.setEditable(false);
		tuned.setLineWrap(true);
		// Each banker's values once changed, to copy back.
		Map<String, int[]> changed = new TreeMap<>();
		boolean[] loading = {false};
		Runnable load = () ->
		{
			String choice = (String) tuneBanker.getSelectedItem();
			int[] cup = changed.containsKey(choice) ? changed.get(choice) : TeaDelivery.cupOf(choice);
			loading[0] = true;
			height.setValue(cup[0]);
			forward.setValue(cup[1]);
			side.setValue(cup[2]);
			loading[0] = false;
			plugin.devTune(choice, cup);
		};
		ChangeListener update = e ->
		{
			if (loading[0])
			{
				return;
			}
			String choice = (String) tuneBanker.getSelectedItem();
			int[] cup = {(Integer) height.getValue(), (Integer) forward.getValue(), (Integer) side.getValue()};
			changed.put(choice, cup);
			StringBuilder text = new StringBuilder();
			for (Map.Entry<String, int[]> each : changed.entrySet())
			{
				int[] v = each.getValue();
				text.append(each.getKey()).append(": height ").append(v[0]).append(", forward ").append(v[1])
					.append(", side ").append(v[2]).append('\n');
			}
			tuned.setText(text.toString());
			plugin.devTune(choice, cup);
		};
		height.addChangeListener(update);
		forward.addChangeListener(update);
		side.addChangeListener(update);
		tuneBanker.addActionListener(e -> load.run());
		body.add(buttons(
			button("Show banker", load),
			button("Turn a quarter", plugin::devTurnTuner),
			button("Hide banker", plugin::devStopTuning),
			button("Copy values", () -> Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(tuned.getText()), null))));
		body.add(wide(tuned));

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
		JCheckBox modifiers = new JCheckBox("Preview the modifier widget", dev.previewModifiers);
		modifiers.setOpaque(false);
		modifiers.addActionListener(e -> dev.previewModifiers = modifiers.isSelected());
		body.add(modifiers);

		body.add(heading("Collection log"));
		JComboBox<String> drop = new JComboBox<>();
		for (CollectionLog.Drop each : CollectionLog.DROPS)
		{
			drop.addItem(each.name);
		}
		body.add(wide(drop));
		body.add(buttons(
			button("Give drop", () -> plugin.devGiveDrop(CollectionLog.DROPS.get(drop.getSelectedIndex()).key)),
			button("Give next missing", plugin::devGiveNextDrop),
			button("Test popup", plugin::devTestPopup),
			button("Open log", plugin::openCollectionLog),
			button("Reset log", plugin::devResetCollection)));
		JCheckBox pet = new JCheckBox("Show the pets without owning them", dev.previewPet);
		pet.setOpaque(false);
		pet.addActionListener(e -> plugin.devPreviewPet(pet.isSelected()));
		body.add(pet);
		JComboBox<String> rat = new JComboBox<>();
		for (Object[] each : Pets.DEV_RATS)
		{
			rat.addItem((String) each[0]);
		}
		rat.addActionListener(e ->
		{
			Object[] chosen = Pets.DEV_RATS[rat.getSelectedIndex()];
			plugin.devRat(new int[]{(int) chosen[1], (int) chosen[2], (int) chosen[3]});
		});
		body.add(row(new JLabel("Bank rat"), rat));

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

	/** A full-width control lined up with the rest; a bare combo box would shift everything right. */
	private static JPanel wide(JComponent component)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		panel.add(component, BorderLayout.CENTER);
		return panel;
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
