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
import nz.tate.bankstanding.core.Unlocks;

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
	/** Scrollbar sprites: up, down, thumb top, middle, bottom, track. */
	static final int[] PARCHMENT_SCROLLBAR = {ARROW_UP, ARROW_DOWN, THUMB_TOP, THUMB_MIDDLE, THUMB_BOTTOM, TRACK};
	static final int[] DARK_SCROLLBAR = {773, 788, 789, 790, 791, 792};
	/** The steel border: corners (top left, top right, bottom left, bottom right), then edges. */
	static final int[] STEEL_CORNERS = {310, 311, 312, 313};
	static final int STEEL_EDGE_HORIZONTAL = 314;
	static final int STEEL_EDGE_VERTICAL = 315;
	static final int DARK_BACKING = 897;
	/** Split tabs, selected and not: left edge, middle, right edge. */
	static final int[] TAB = {998, 999, 1000};
	static final int[] TAB_DIM = {1001, 1002, 1003};
	static final int STEEL_CLOSE = 535;

	static final Color INK = new Color(0x46, 0x32, 0x0a);
	static final Color FADED_INK = new Color(0x8c, 0x74, 0x4c);
	static final Color SELECTED = new Color(0x80, 0x00, 0x00);

	private static final int QUILL_FONT = 645;
	private static final int SMALL_FONT = 494;
	private static final int PLAIN_FONT = 495;
	private static final int BOLD_FONT = 496;
	private static final int LARGE_BOLD_FONT = 497;
	private static final int COINS = 995;

	private final Client client;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final Map<Integer, BufferedImage> sprites = new ConcurrentHashMap<>();
	/** Mirrored copies of sprites, by the original's ID. */
	private final Map<Integer, BufferedImage> flipped = new ConcurrentHashMap<>();
	/** Tinted icons, keyed by item ID and tint. */
	private final Map<Long, Image> tintedIcons = new ConcurrentHashMap<>();
	private BufferedImage sideParchment;
	/** The game's fonts: the skill guide's title, small, plain, bold and larger bold. */
	GameFont quill;
	GameFont small;
	GameFont plain;
	GameFont bold;
	GameFont largeBold;

	@Inject
	GuideArt(Client client, SpriteManager spriteManager, ItemManager itemManager)
	{
		this.client = client;
		this.spriteManager = spriteManager;
		this.itemManager = itemManager;
		for (int id : new int[]{TITLE_SCROLL, CLOSE, STEEL_CLOSE, DARK_BACKING, STEEL_EDGE_HORIZONTAL, STEEL_EDGE_VERTICAL})
		{
			load(id);
		}
		for (int[] set : new int[][]{PARCHMENT_SCROLLBAR, DARK_SCROLLBAR, STEEL_CORNERS, TAB, TAB_DIM})
		{
			for (int id : set)
			{
				load(id);
			}
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

	/** A mirror image: left to right if {@code horizontally}, otherwise top to bottom. */
	private static BufferedImage flip(BufferedImage image, boolean horizontally)
	{
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int px = 0; px < w; px++)
		{
			for (int py = 0; py < h; py++)
			{
				out.setRGB(horizontally ? w - 1 - px : px, horizontally ? py : h - 1 - py, image.getRGB(px, py));
			}
		}
		return out;
	}

	/** How far a tab's slanted right end reaches in, so labels can stay clear of it. */
	int tabSlant()
	{
		BufferedImage right = sprites.get(TAB[2]);
		return right == null ? 0 : right.getWidth();
	}

	/** Loads the fonts the first time; false until everything needed to draw is ready. Client thread. */
	boolean ready()
	{
		if (quill == null)
		{
			quill = GameFont.load(client, QUILL_FONT);
			small = GameFont.load(client, SMALL_FONT);
			plain = GameFont.load(client, PLAIN_FONT);
			bold = GameFont.load(client, BOLD_FONT);
			largeBold = GameFont.load(client, LARGE_BOLD_FONT);
		}
		return quill != null && small != null && plain != null && bold != null && largeBold != null
			&& sprites.containsKey(TITLE_SCROLL) && sprites.containsKey(DARK_BACKING);
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

	/** A scrollbar from one of the sprite sets: arrows, a tiled track and a stretched thumb. */
	void scrollbar(Graphics2D graphics, int[] set, int x, int y, int height, int total, int visible, int first)
	{
		draw(graphics, set[0], x, y);
		draw(graphics, set[1], x, y + height - ARROW);
		BufferedImage track = sprites.get(set[5]);
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
		BufferedImage top = sprites.get(set[2]);
		BufferedImage middle = sprites.get(set[3]);
		BufferedImage bottom = sprites.get(set[4]);
		if (top != null && middle != null && bottom != null)
		{
			graphics.drawImage(middle, x, thumbY + top.getHeight(), middle.getWidth(),
				thumbHeight - top.getHeight() - bottom.getHeight(), null);
			graphics.drawImage(top, x, thumbY, null);
			graphics.drawImage(bottom, x, thumbY + thumbHeight - bottom.getHeight(), null);
		}
	}

	/** The game's dark stone backing, tiled over an area. */
	void darkBacking(Graphics2D graphics, int x, int y, int width, int height)
	{
		BufferedImage tile = sprites.get(DARK_BACKING);
		if (tile == null)
		{
			return;
		}
		java.awt.Shape clip = graphics.getClip();
		graphics.clipRect(x, y, width, height);
		for (int ty = y; ty < y + height; ty += tile.getHeight())
		{
			for (int tx = x; tx < x + width; tx += tile.getWidth())
			{
				graphics.drawImage(tile, tx, ty, null);
			}
		}
		graphics.setClip(clip);
	}

	/** The steel border used around the game's windows. */
	void steelBorder(Graphics2D graphics, int x, int y, int width, int height)
	{
		// The game only has the top and right edges; the bottom and left are those flipped.
		BufferedImage horizontal = sprites.get(STEEL_EDGE_HORIZONTAL);
		BufferedImage vertical = sprites.get(STEEL_EDGE_VERTICAL);
		BufferedImage topLeft = sprites.get(STEEL_CORNERS[0]);
		if (horizontal == null || vertical == null || topLeft == null)
		{
			return;
		}
		BufferedImage bottom = flipped.computeIfAbsent(STEEL_EDGE_HORIZONTAL, id -> flip(horizontal, false));
		BufferedImage left = flipped.computeIfAbsent(STEEL_EDGE_VERTICAL, id -> flip(vertical, true));
		int cw = topLeft.getWidth();
		int ch = topLeft.getHeight();
		java.awt.Shape clip = graphics.getClip();
		graphics.clipRect(x + cw, y, width - 2 * cw, height);
		for (int tx = x + cw; tx < x + width - cw; tx += horizontal.getWidth())
		{
			graphics.drawImage(horizontal, tx, y, null);
			graphics.drawImage(bottom, tx, y + height - bottom.getHeight(), null);
		}
		graphics.setClip(clip);
		graphics.clipRect(x, y + ch, width, height - 2 * ch);
		for (int ty = y + ch; ty < y + height - ch; ty += vertical.getHeight())
		{
			graphics.drawImage(left, x, ty, null);
			graphics.drawImage(vertical, x + width - vertical.getWidth(), ty, null);
		}
		graphics.setClip(clip);
		draw(graphics, STEEL_CORNERS[0], x, y);
		draw(graphics, STEEL_CORNERS[1], x + width - cw, y);
		draw(graphics, STEEL_CORNERS[2], x, y + height - ch);
		draw(graphics, STEEL_CORNERS[3], x + width - cw, y + height - ch);
	}

	/** One split tab, {@code width} wide: lighter when selected. */
	void tab(Graphics2D graphics, int x, int y, int width, boolean selected)
	{
		int[] set = selected ? TAB : TAB_DIM;
		BufferedImage left = sprites.get(set[0]);
		BufferedImage middle = sprites.get(set[1]);
		BufferedImage right = sprites.get(set[2]);
		if (left == null || middle == null || right == null)
		{
			return;
		}
		graphics.drawImage(middle, x + left.getWidth(), y, width - left.getWidth() - right.getWidth(), middle.getHeight(), null);
		graphics.drawImage(left, x, y, null);
		graphics.drawImage(right, x + width - right.getWidth(), y, null);
	}

	/** An item's icon (coins as a big stack, without the number), optionally turned gold. */
	Image itemIcon(int item, boolean gold)
	{
		return itemIcon(item, gold ? Unlocks.GOLD_TINT : 0);
	}

	/** An item's icon tinted to an RGB colour, keeping its light and shade; 0 leaves it as it is. */
	Image itemIcon(int item, int tint)
	{
		if (tint == 0)
		{
			return itemManager.getImage(item, item == COINS ? 10_000 : 1, false);
		}
		long key = (long) item << 32 | tint;
		Image icon = tintedIcons.get(key);
		if (icon == null)
		{
			AsyncBufferedImage image = itemManager.getImage(item);
			tintedIcons.put(key, image);
			image.onLoaded(() -> tintedIcons.put(key, tinted(image, tint)));
			return image;
		}
		return icon;
	}

	private static final Color STAR = new Color(0xff, 0xd7, 0x00);
	private static final Color STAR_EDGE = new Color(0x3e, 0x2a, 0x00);

	/** A small gold star centred on (x, y), marking something new. */
	static void star(Graphics2D graphics, int x, int y, int radius)
	{
		java.awt.Polygon star = new java.awt.Polygon();
		for (int i = 0; i < 10; i++)
		{
			double r = i % 2 == 0 ? radius : radius * 0.45;
			double angle = Math.PI * i / 5 - Math.PI / 2;
			star.addPoint(x + (int) Math.round(r * Math.cos(angle)), y + (int) Math.round(r * Math.sin(angle)));
		}
		Object antialias = graphics.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(STAR);
		graphics.fillPolygon(star);
		graphics.setColor(STAR_EDGE);
		graphics.drawPolygon(star);
		graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
			antialias == null ? java.awt.RenderingHints.VALUE_ANTIALIAS_DEFAULT : antialias);
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

	/** The same picture in one colour, keeping its light and shade. */
	static BufferedImage tinted(BufferedImage image, int tint)
	{
		int tr = (tint >> 16) & 0xff;
		int tg = (tint >> 8) & 0xff;
		int tb = tint & 0xff;
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
				out.setRGB(px, py, alpha << 24 | Math.round(tr * light) << 16 | Math.round(tg * light) << 8
					| Math.round(tb * light));
			}
		}
		return out;
	}
}
