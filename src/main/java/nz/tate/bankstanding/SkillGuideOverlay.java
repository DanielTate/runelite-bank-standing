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
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.Unlocks;

/**
 * The skill guide: click the Bank Standing tile to see what each level unlocks, like clicking
 * a skill in the skills tab. A click anywhere closes it again.
 */
@Singleton
class SkillGuideOverlay extends Overlay
{
	private static final int WIDTH = 300;
	private static final int ROW = 18;
	private static final int TOP = 34;
	private static final int PAD = 10;
	private static final Color FACE = new Color(62, 53, 41);
	private static final Color ROW_FACE = new Color(72, 62, 48);
	private static final Color EDGE_DARK = new Color(28, 24, 18);
	private static final Color EDGE_LIGHT = new Color(94, 82, 64);
	private static final Color TITLE = new Color(255, 152, 31);
	private static final Color REACHED = new Color(255, 255, 0);
	private static final Color REACHED_TEXT = new Color(255, 152, 31);
	private static final Color LOCKED = new Color(140, 128, 110);

	private final BankStandingPlugin plugin;
	private final Client client;
	private final SkillTileOverlay tile;
	private final BufferedImage icon;
	private volatile boolean open;
	private volatile Rectangle drawn = new Rectangle();
	/** Set when a press was ours, so the release and click don't reach the game either. */
	private boolean swallowing;

	final MouseAdapter mouse = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			swallowing = false;
			if (event.isAltDown() || plugin.getProgress() == null)
			{
				return event;
			}
			boolean onTile = SwingUtilities.isLeftMouseButton(event) && tile.getBounds().contains(event.getPoint());
			boolean onGuide = open && drawn.contains(event.getPoint());
			if (onTile)
			{
				open = !open;
			}
			else if (open)
			{
				open = false;
			}
			if (onTile || onGuide)
			{
				swallowing = true;
				event.consume();
			}
			return event;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent event)
		{
			return swallow(event);
		}

		@Override
		public MouseEvent mouseClicked(MouseEvent event)
		{
			return swallow(event);
		}

		private MouseEvent swallow(MouseEvent event)
		{
			if (swallowing)
			{
				event.consume();
			}
			return event;
		}
	};

	@Inject
	SkillGuideOverlay(BankStandingPlugin plugin, Client client, SkillTileOverlay tile, Icons icons)
	{
		this.plugin = plugin;
		this.client = client;
		this.tile = tile;
		this.icon = icons.full();
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	void close()
	{
		open = false;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Progress progress = plugin.getProgress();
		if (!open || progress == null)
		{
			return null;
		}
		int level = progress.level();

		int height = TOP + Unlocks.ALL.size() * ROW + PAD;
		int x = client.getViewportXOffset() + (client.getViewportWidth() - WIDTH) / 2;
		int y = client.getViewportYOffset() + Math.max(0, (client.getViewportHeight() - height) / 2);
		drawn = new Rectangle(x, y, WIDTH, height);

		graphics.setColor(FACE);
		graphics.fillRect(x, y, WIDTH, height);
		graphics.setColor(EDGE_LIGHT);
		graphics.drawLine(x + 1, y + 1, x + WIDTH - 2, y + 1);
		graphics.drawLine(x + 1, y + 1, x + 1, y + height - 2);
		graphics.setColor(EDGE_DARK);
		graphics.drawRect(x, y, WIDTH - 1, height - 1);

		graphics.drawImage(icon, x + PAD, y + 5, null);
		graphics.setFont(FontManager.getRunescapeBoldFont());
		shadowed(graphics, "Bank Standing", x + PAD + icon.getWidth() + 6, y + 22, TITLE);

		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();
		String hint = "Click to close";
		shadowed(graphics, hint, x + WIDTH - PAD - metrics.stringWidth(hint), y + 21, LOCKED);

		graphics.setFont(FontManager.getRunescapeFont());
		metrics = graphics.getFontMetrics();
		int rowY = y + TOP;
		for (int i = 0; i < Unlocks.ALL.size(); i++)
		{
			Unlocks.Unlock unlock = Unlocks.ALL.get(i);
			if (i % 2 == 0)
			{
				graphics.setColor(ROW_FACE);
				graphics.fillRect(x + PAD - 4, rowY, WIDTH - 2 * (PAD - 4), ROW);
			}
			boolean reached = level >= unlock.level;
			int baseline = rowY + (ROW + metrics.getAscent() - metrics.getDescent()) / 2;
			String number = String.valueOf(unlock.level);
			shadowed(graphics, number, x + PAD + 22 - metrics.stringWidth(number), baseline, reached ? REACHED : LOCKED);
			shadowed(graphics, unlock.text, x + PAD + 34, baseline, reached ? REACHED_TEXT : LOCKED);
			rowY += ROW;
		}
		return new Dimension(WIDTH, height);
	}

	private static void shadowed(Graphics2D graphics, String text, int x, int y, Color color)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}
}
