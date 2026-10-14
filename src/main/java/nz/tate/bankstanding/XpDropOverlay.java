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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Bank Standing XP drops: icon and amount drifting upward and fading, like the game's own.
 * Movable with alt-drag; it sits top right by default, near where real drops appear.
 */
class XpDropOverlay extends Overlay
{
	private static final int WIDTH = 80;
	private static final int HEIGHT = 90;
	private static final int ICON = 16;

	private final DropFeed feed;
	private final BufferedImage icon;

	@Inject
	XpDropOverlay(DropFeed feed, Icons icons)
	{
		this.feed = feed;
		this.icon = icons.small();
		setPosition(OverlayPosition.TOP_RIGHT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics metrics = graphics.getFontMetrics();
		long now = System.currentTimeMillis();
		Composite original = graphics.getComposite();

		for (DropFeed.Drop drop : feed.recent(DropFeed.XP_DROP_MILLIS))
		{
			float age = (now - drop.getCreatedMillis()) / (float) DropFeed.XP_DROP_MILLIS;
			int y = HEIGHT - (int) (age * HEIGHT);
			float alpha = age < 0.7f ? 1f : Math.max(0f, (1f - age) / 0.3f);
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

			String text = "+" + drop.getAmount();
			int textX = WIDTH - metrics.stringWidth(text);
			graphics.drawImage(icon, textX - ICON - 3, y - ICON + 2, null);
			graphics.setColor(Color.BLACK);
			graphics.drawString(text, textX + 1, y + 1);
			graphics.setColor(Color.WHITE);
			graphics.drawString(text, textX, y);
		}

		graphics.setComposite(original);
		return new Dimension(WIDTH, HEIGHT);
	}
}
