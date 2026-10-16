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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import nz.tate.bankstanding.core.Experience;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.Unlocks;

/**
 * A skills-tab style tile: the booth icon and the level shown as "42 / 42". Hovering it shows
 * the XP the way the skills tab does.
 */
@Singleton
class SkillTileOverlay extends Overlay
{
	private static final int WIDTH = 62;
	private static final int HEIGHT = 32;
	private static final Color FACE = new Color(62, 53, 41);
	private static final Color EDGE_DARK = new Color(28, 24, 18);
	private static final Color EDGE_LIGHT = new Color(94, 82, 64);
	private static final Color LEVEL = new Color(255, 255, 0);

	private final BankStandingPlugin plugin;
	private final Client client;
	private final TooltipManager tooltipManager;
	private final BufferedImage icon;
	private volatile long renderedAt;

	@Inject
	SkillTileOverlay(BankStandingPlugin plugin, Client client, TooltipManager tooltipManager, Icons icons)
	{
		this.plugin = plugin;
		this.client = client;
		this.tooltipManager = tooltipManager;
		this.icon = icons.full();
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
	}

	/** Where the tile is on screen, or empty if it hasn't been drawn lately. Mouse thread. */
	Rectangle shownBounds()
	{
		return System.nanoTime() - renderedAt < GameWindow.STALE_NANOS ? getBounds() : new Rectangle();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Progress progress = plugin.getProgress();
		if (progress == null)
		{
			return null;
		}
		renderedAt = System.nanoTime();

		graphics.setColor(FACE);
		graphics.fillRect(0, 0, WIDTH, HEIGHT);
		graphics.setColor(EDGE_LIGHT);
		graphics.drawLine(1, 1, WIDTH - 2, 1);
		graphics.drawLine(1, 1, 1, HEIGHT - 2);
		graphics.setColor(EDGE_DARK);
		graphics.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

		graphics.drawImage(icon, 3, (HEIGHT - icon.getHeight()) / 2, null);

		String level = String.valueOf(progress.level());
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();
		shadowed(graphics, level, 44 - metrics.stringWidth(level) / 2, 13);
		shadowed(graphics, "/", 47, 20);
		shadowed(graphics, level, 54 - metrics.stringWidth(level) / 2, 28);

		Point mouse = client.getMouseCanvasPosition();
		if (getBounds().contains(mouse.getX(), mouse.getY()))
		{
			tooltipManager.add(new Tooltip(tooltip(progress)));
		}
		return new Dimension(WIDTH, HEIGHT);
	}

	/** Like the skills tab: XP, then the next level and what's left, until 99. */
	static String tooltip(Progress progress)
	{
		NumberFormat format = NumberFormat.getIntegerInstance();
		String text = "Bank Standing XP: " + format.format(progress.xp);
		int level = progress.level();
		if (level < Experience.MAX_LEVEL)
		{
			int next = Experience.xpForLevel(level + 1);
			text += "</br>Next level at: " + format.format(next)
				+ "</br>Remaining XP: " + format.format(next - progress.xp);
		}
		int unlock = Unlocks.nextUnlockLevel(level);
		if (unlock > 0)
		{
			text += "</br>Next unlock at level " + unlock;
		}
		return text + "</br>Click to see unlocks";
	}

	private static void shadowed(Graphics2D graphics, String text, int x, int y)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(LEVEL);
		graphics.drawString(text, x, y);
	}
}
