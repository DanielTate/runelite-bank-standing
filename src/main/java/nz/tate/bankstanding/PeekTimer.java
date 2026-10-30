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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
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
 * A stopwatch pinned to a side of the bank while a peek holds a stand open, counting
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
	private final BankStandingConfig config;
	private final Font font = FontManager.getRunescapeBoldFont().deriveFont(FONT_SIZE);

	/** When the stand ends, in epoch millis; 0 while there's no peek under way. */
	private volatile long endsAt;

	@Inject
	PeekTimer(Client client, ItemManager itemManager, BankStandingConfig config)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.config = config;
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
		if (ends == 0 || bank == null || !config.showPeekTimer())
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

		// Placed in screen space, then drawn at full size into a scaled box when compact.
		double scale = config.compactOverlays() ? BankStandingConfig.COMPACT_SCALE : 1;
		int shownWidth = (int) Math.ceil(width * scale);
		int shownHeight = (int) Math.ceil(height * scale);
		Point at = place(bank.getBounds(), shownWidth, shownHeight);

		AffineTransform transform = graphics.getTransform();
		graphics.translate(at.x, at.y);
		graphics.scale(scale, scale);
		graphics.setColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		graphics.fillRect(0, 0, width, height);
		graphics.setColor(Color.BLACK);
		graphics.drawRect(0, 0, width - 1, height - 1);

		graphics.drawImage(icon, PADDING, (height - icon.getHeight()) / 2, null);
		int textX = PADDING + icon.getWidth() + GAP;
		int textY = (height + metrics.getAscent()) / 2 - 2;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, textX + 2, textY + 2);
		graphics.setColor(left <= WARN_MILLIS ? Color.RED : Color.WHITE);
		graphics.drawString(text, textX, textY);
		graphics.setTransform(transform);
		return new Dimension(shownWidth, shownHeight);
	}

	/** Where the timer's top left goes, kept on screen. */
	private Point place(Rectangle bank, int width, int height)
	{
		int x;
		int y;
		Rectangle chat;
		Rectangle map;
		switch (config.peekTimerPosition())
		{
			case ABOVE_CHAT:
				// With the chatbox closed, the bottom left of the screen.
				chat = shown(InterfaceID.Chatbox.UNIVERSE);
				x = chat == null ? GAP : chat.x + GAP;
				y = (chat == null ? client.getCanvasHeight() : chat.y) - height - GAP;
				break;
			case BY_MINIMAP:
				// Left of the minimap and its orbs, in whichever layout is on; else the top right.
				map = shown(InterfaceID.Toplevel.MAPCONTAINER, InterfaceID.ToplevelOsrsStretch.MAP_CONTAINER,
					InterfaceID.ToplevelPreEoc.MAP_CONTAINER);
				x = (map == null ? client.getCanvasWidth() : map.x) - width - GAP;
				y = map == null ? GAP : map.y + GAP;
				break;
			case BELOW:
				x = bank.x + (bank.width - width) / 2;
				y = bank.y + bank.height + GAP;
				break;
			case LEFT:
				x = bank.x - width - GAP;
				y = bank.y + (bank.height - height) / 2;
				break;
			case RIGHT:
				x = bank.x + bank.width + GAP;
				y = bank.y + (bank.height - height) / 2;
				break;
			default:
				x = bank.x + (bank.width - width) / 2;
				y = bank.y - height - GAP;
				break;
		}
		x = Math.max(0, Math.min(x, client.getCanvasWidth() - width));
		y = Math.max(0, Math.min(y, client.getCanvasHeight() - height));
		return new Point(x, y);
	}

	private Widget openBank()
	{
		return widget(InterfaceID.Bankmain.UNIVERSE, InterfaceID.BankDepositbox.UNIVERSE);
	}

	/** The first of these widgets that's showing, or null. */
	private Widget widget(int... ids)
	{
		for (int id : ids)
		{
			Widget widget = client.getWidget(id);
			if (widget != null && !widget.isHidden())
			{
				return widget;
			}
		}
		return null;
	}

	/** Where the first of these widgets that's showing is on screen, or null. */
	private Rectangle shown(int... ids)
	{
		Widget widget = widget(ids);
		return widget == null ? null : widget.getBounds();
	}
}
