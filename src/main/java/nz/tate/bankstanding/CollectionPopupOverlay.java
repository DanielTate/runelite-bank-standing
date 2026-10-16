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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The banner that slides down when something new goes in the Bank Standing collection log.
 * It's styled like the game's interfaces but titled with the skill's name and icon, so it's
 * never mistaken for the game's own popup. Several in a row are shown one after another.
 */
@Singleton
class CollectionPopupOverlay extends Overlay
{
	private static final int WIDTH = 320;
	private static final int HEIGHT = 64;
	private static final Color ORANGE = new Color(0xff, 0x98, 0x1f);
	private static final int ICON = 32;
	private static final long SLIDE_MS = 350;
	private static final long SHOW_MS = 5000;
	private static final long FADE_MS = 600;

	/** One popup: the line under the title, and the icon to show. */
	private static final class Entry
	{
		final String text;
		final int item;
		final boolean gold;
		long startedAt;

		Entry(String text, int item, boolean gold)
		{
			this.text = text;
			this.item = item;
			this.gold = gold;
		}
	}

	private final Client client;
	private final GuideArt art;
	private final BufferedImage skillIcon;
	private final Deque<Entry> queue = new ArrayDeque<>();

	@Inject
	CollectionPopupOverlay(Client client, GuideArt art, Icons icons)
	{
		this.client = client;
		this.art = art;
		this.skillIcon = icons.small();
		setPosition(OverlayPosition.DYNAMIC);
		// Always on top, so the resizable side panel and other plugins' bars can't cover it.
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	/** Queues a popup; {@code item} is the icon, or -1 for none. Client thread. */
	void show(String text, int item, boolean gold)
	{
		queue.addLast(new Entry(text, item, gold));
	}

	void clear()
	{
		queue.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Entry entry = queue.peekFirst();
		if (entry == null || !art.ready())
		{
			return null;
		}
		long now = System.currentTimeMillis();
		if (entry.startedAt == 0)
		{
			entry.startedAt = now;
		}
		long age = now - entry.startedAt;
		if (age > SHOW_MS)
		{
			queue.pollFirst();
			return null;
		}

		int x = client.getViewportXOffset() + (client.getViewportWidth() - WIDTH) / 2;
		int restY = client.getViewportYOffset() + 10;
		int y = age < SLIDE_MS ? restY - (int) ((HEIGHT + 10) * (1 - (double) age / SLIDE_MS)) : restY;
		float alpha = age > SHOW_MS - FADE_MS ? (SHOW_MS - age) / (float) FADE_MS : 1f;

		Composite before = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
		art.darkBacking(graphics, x, y, WIDTH, HEIGHT);
		art.steelBorder(graphics, x, y, WIDTH, HEIGHT);
		int textX = x + 24;
		if (entry.item > 0)
		{
			Image icon = art.itemIcon(entry.item, entry.gold);
			GuideArt.drawIcon(graphics, icon, x + 20, y + (HEIGHT - ICON) / 2, ICON);
			textX = x + 20 + ICON + 10;
		}
		int centre = (textX + x + WIDTH - 20) / 2;
		// "Bank Standing" and the skill's icon, so it can't be mistaken for the game's own popup.
		String title = "Bank Standing collection log";
		int titleWidth = skillIcon.getWidth() + 4 + art.bold.width(title);
		int titleX = centre - titleWidth / 2;
		graphics.drawImage(skillIcon, titleX, y + 12, null);
		art.bold.drawShadowed(graphics, title, titleX + skillIcon.getWidth() + 4, y + 14, ORANGE);
		art.plain.drawShadowedCentred(graphics, entry.text, centre, y + 18 + art.bold.height(), Color.WHITE);
		graphics.setComposite(before);
		return new Dimension(WIDTH, HEIGHT);
	}
}
