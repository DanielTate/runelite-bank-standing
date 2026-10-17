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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;

/**
 * A stopwatch pinned to the top of the bank while a peek holds a stand open, counting
 * down to the millisecond until the stand ends.
 */
@Singleton
class PeekTimer extends Overlay
{
	private static final long MILLIS_PER_TICK = 600;
	private static final long WARN_MILLIS = 3000;
	private static final float FONT_SIZE = 28f;
	private static final int PADDING = 6;
	private static final int GAP = 4;

	private final Client client;
	private final ItemManager itemManager;
	private final Font font = FontManager.getRunescapeBoldFont().deriveFont(FONT_SIZE);

	/** When the stand ends, in epoch millis; 0 while there's no peek under way. */
	private volatile long endsAt;

	@Inject
	PeekTimer(Client client, ItemManager itemManager)
	{
		this.client = client;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/** Called each tick with the ticks left (0 hides it), so it stays in step with the game. */
	void ticksLeft(int ticks)
	{
		endsAt = ticks > 0 ? System.currentTimeMillis() + ticks * MILLIS_PER_TICK : 0;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		long ends = endsAt;
		Widget bank = openBank();
		if (ends == 0 || bank == null)
		{
			return null;
		}
		long left = Math.max(0, ends - System.currentTimeMillis());
		String text = String.format("%02d:%02d.%03d", left / 60_000, left / 1000 % 60, left % 1000);

		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		BufferedImage icon = itemManager.getImage(ItemID.GIANT_STOPWATCH);
		// Fixed width from the widest digits, so the box doesn't twitch as the time changes.
		int textWidth = metrics.stringWidth("00:00.000");
		int width = PADDING + icon.getWidth() + GAP + textWidth + PADDING;
		int height = Math.max(icon.getHeight(), metrics.getAscent()) + PADDING * 2;

		Rectangle bounds = bank.getBounds();
		int x = bounds.x + (bounds.width - width) / 2;
		int y = Math.max(0, bounds.y - height - GAP);

		graphics.setColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		graphics.fillRect(x, y, width, height);
		graphics.setColor(Color.BLACK);
		graphics.drawRect(x, y, width - 1, height - 1);

		graphics.drawImage(icon, x + PADDING, y + (height - icon.getHeight()) / 2, null);
		int textX = x + PADDING + icon.getWidth() + GAP;
		int textY = y + (height + metrics.getAscent()) / 2 - 2;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, textX + 2, textY + 2);
		graphics.setColor(left <= WARN_MILLIS ? Color.RED : Color.WHITE);
		graphics.drawString(text, textX, textY);
		return new Dimension(width, height);
	}

	private Widget openBank()
	{
		for (int id : new int[]{InterfaceID.Bankmain.UNIVERSE, InterfaceID.BankDepositbox.UNIVERSE})
		{
			Widget widget = client.getWidget(id);
			if (widget != null && !widget.isHidden())
			{
				return widget;
			}
		}
		return null;
	}
}
