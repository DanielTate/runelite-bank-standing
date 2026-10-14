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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * The game art the parchment windows share: the scroll, close button and scrollbar sprites,
 * the skill guide's fonts, and item icons (some turned gold). All taken from the game cache.
 */
@Singleton
class GuideArt
{
	static final int TITLE_SCROLL = 436;
	static final int CLOSE = 541;
	static final int ARROW_UP = 793;
	static final int ARROW_DOWN = 794;
	static final int THUMB_TOP = 795;
	static final int THUMB_MIDDLE = 796;
	static final int THUMB_BOTTOM = 797;
	static final int TRACK = 798;
	static final int ARROW = 16;

	static final Color INK = new Color(0x46, 0x32, 0x0a);
	static final Color FADED_INK = new Color(0x8c, 0x74, 0x4c);
	static final Color SELECTED = new Color(0x80, 0x00, 0x00);

	private static final int TITLE_FONT = 645;
	private static final int SUBTITLE_FONT = 494;
	private static final int CATEGORY_FONT = 497;
	private static final int COINS = 995;

	private final Client client;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final Map<Integer, BufferedImage> sprites = new ConcurrentHashMap<>();
	private final Map<Integer, Image> goldIcons = new ConcurrentHashMap<>();
	private BufferedImage sideParchment;
	GameFont title;
	GameFont subtitle;
	GameFont category;
	GameFont selected;

	@Inject
	GuideArt(Client client, SpriteManager spriteManager, ItemManager itemManager)
	{
		this.client = client;
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
		for (int id : new int[]{TITLE_SCROLL, CLOSE, ARROW_UP, ARROW_DOWN, THUMB_TOP, THUMB_MIDDLE, THUMB_BOTTOM, TRACK})
		{
			load(id);
		}
	}

	/** Starts loading a sprite, so it's ready when first drawn. */
	void load(int sprite)
	{
		if (!sprites.containsKey(sprite))
		{
			spriteManager.getSpriteAsync(sprite, 0, image -> sprites.put(sprite, image));
		}
	}

	BufferedImage sprite(int id)
	{
		return sprites.get(id);
	}

	/** Loads the fonts the first time; false until everything needed to draw is ready. Client thread. */
	boolean ready()
	{
		if (title == null)
		{
			title = GameFont.load(client, TITLE_FONT, INK);
			subtitle = GameFont.load(client, SUBTITLE_FONT, INK);
			category = GameFont.load(client, CATEGORY_FONT, INK);
			selected = GameFont.load(client, CATEGORY_FONT, SELECTED);
		}
		return title != null && subtitle != null && category != null && selected != null
			&& sprites.containsKey(TITLE_SCROLL);
	}

	/** A parchment scroll with its rolled ends left and right. */
	void parchment(Graphics2D graphics, int x, int y, int width, int height)
	{
		ninePatch(graphics, sprites.get(TITLE_SCROLL), x, y, width, height, 16, 6);
	}

	/** A parchment scroll on its side, rolled at the top and bottom. */
	void sideParchment(Graphics2D graphics, int x, int y, int width, int height)
	{
		BufferedImage source = sprites.get(TITLE_SCROLL);
		if (sideParchment == null && source != null)
		{
			BufferedImage out = new BufferedImage(source.getHeight(), source.getWidth(), BufferedImage.TYPE_INT_ARGB);
			for (int sy = 0; sy < source.getHeight(); sy++)
			{
				for (int sx = 0; sx < source.getWidth(); sx++)
				{
					out.setRGB(source.getHeight() - 1 - sy, sx, source.getRGB(sx, sy));
				}
			}
			sideParchment = out;
		}
		ninePatch(graphics, sideParchment, x, y, width, height, 6, 16);
	}

	void draw(Graphics2D graphics, int sprite, int x, int y)
	{
		BufferedImage image = sprites.get(sprite);
		if (image != null)
		{
			graphics.drawImage(image, x, y, null);
		}
	}

	/** The parchment scrollbar: arrows, a tiled track and a stretched thumb. */
	void scrollbar(Graphics2D graphics, int x, int y, int height, int total, int visible, int first)
	{
		draw(graphics, ARROW_UP, x, y);
		draw(graphics, ARROW_DOWN, x, y + height - ARROW);
		BufferedImage track = sprites.get(TRACK);
		if (track != null)
		{
			for (int ty = y + ARROW; ty < y + height - ARROW; ty += track.getHeight())
			{
				int h = Math.min(track.getHeight(), y + height - ARROW - ty);
				graphics.drawImage(track, x, ty, x + track.getWidth(), ty + h, 0, 0, track.getWidth(), h, null);
			}
		}
		int trackHeight = height - 2 * ARROW;
		int thumbHeight = Math.max(12, trackHeight * visible / total);
		int thumbY = y + ARROW + (trackHeight - thumbHeight) * first / Math.max(1, total - visible);
		BufferedImage top = sprites.get(THUMB_TOP);
		BufferedImage middle = sprites.get(THUMB_MIDDLE);
		BufferedImage bottom = sprites.get(THUMB_BOTTOM);
		if (top != null && middle != null && bottom != null)
		{
			graphics.drawImage(middle, x, thumbY + top.getHeight(), middle.getWidth(),
				thumbHeight - top.getHeight() - bottom.getHeight(), null);
			graphics.drawImage(top, x, thumbY, null);
			graphics.drawImage(bottom, x, thumbY + thumbHeight - bottom.getHeight(), null);
		}
	}

	/** An item's icon (coins as a big stack, without the number), optionally turned gold. */
	Image itemIcon(int item, boolean gold)
	{
		if (!gold)
		{
			return itemManager.getImage(item, item == COINS ? 10_000 : 1, false);
		}
		Image icon = goldIcons.get(item);
		if (icon == null)
		{
			AsyncBufferedImage image = itemManager.getImage(item);
			goldIcons.put(item, image);
			image.onLoaded(() -> goldIcons.put(item, gilded(image)));
			return image;
		}
		return icon;
	}

	/** Draws an icon shrunk, if it has to be, to fit a square box, centred in it. */
	static void drawIcon(Graphics2D graphics, Image icon, int x, int y, int box)
	{
		if (icon == null)
		{
			return;
		}
		int width = icon.getWidth(null);
		int height = icon.getHeight(null);
		if (width <= 0 || height <= 0)
		{
			return;
		}
		double scale = Math.min(1.0, Math.min((double) box / width, (double) box / height));
		int w = (int) Math.round(width * scale);
		int h = (int) Math.round(height * scale);
		graphics.drawImage(icon, x + (box - w) / 2, y + (box - h) / 2, w, h, null);
	}

	/** Stretches a sprite to any size, keeping its edges (cx wide, cy tall) unstretched. */
	static void ninePatch(Graphics2D graphics, BufferedImage source, int x, int y, int width, int height, int cx, int cy)
	{
		if (source == null)
		{
			return;
		}
		int sw = source.getWidth();
		int sh = source.getHeight();
		int[][] cols = {{0, cx, 0, cx}, {cx, sw - cx, cx, width - cx}, {sw - cx, sw, width - cx, width}};
		int[][] rows = {{0, cy, 0, cy}, {cy, sh - cy, cy, height - cy}, {sh - cy, sh, height - cy, height}};
		for (int[] col : cols)
		{
			for (int[] row : rows)
			{
				graphics.drawImage(source, x + col[2], y + row[2], x + col[3], y + row[3],
					col[0], row[0], col[1], row[1], null);
			}
		}
	}

	/** The same picture in gold, keeping its light and shade. */
	static BufferedImage gilded(BufferedImage image)
	{
		BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int py = 0; py < image.getHeight(); py++)
		{
			for (int px = 0; px < image.getWidth(); px++)
			{
				int argb = image.getRGB(px, py);
				int alpha = argb >>> 24;
				if (alpha == 0)
				{
					continue;
				}
				int r = (argb >> 16) & 0xff;
				int g = (argb >> 8) & 0xff;
				int b = argb & 0xff;
				float light = Math.min(1f, (0.3f * r + 0.59f * g + 0.11f * b) / 255f * 1.6f + 0.1f);
				out.setRGB(px, py, alpha << 24 | Math.round(255 * light) << 16 | Math.round(200 * light) << 8
					| Math.round(60 * light));
			}
		}
		return out;
	}
}
