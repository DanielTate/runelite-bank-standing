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
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.AsyncBufferedImage;
import nz.tate.bankstanding.core.Progress;
import nz.tate.bankstanding.core.Unlocks;

/**
 * The skill guide: click the Bank Standing tile to see what each level unlocks, like clicking
 * a skill in the skills tab. It's drawn from the game's own parchment, scrollbar and close
 * button sprites, with the categories on a second scroll at the side. The mouse wheel or the
 * arrows scroll the list; a click outside it, or on the close button, closes it.
 */
@Singleton
class SkillGuideOverlay extends Overlay
{
	private static final int TITLE_SCROLL = 436;
	private static final int CLOSE = 541;
	private static final int ARROW_UP = 793;
	private static final int ARROW_DOWN = 794;
	private static final int THUMB_TOP = 795;
	private static final int THUMB_MIDDLE = 796;
	private static final int THUMB_BOTTOM = 797;
	private static final int TRACK = 798;

	private static final int WIDTH = 500;
	private static final int HEIGHT = 320;
	private static final int MAIN_WIDTH = 350;
	private static final Rectangle SIDEBAR = new Rectangle(358, 40, 140, 270);
	private static final Rectangle CLOSE_BUTTON = new Rectangle(WIDTH - 28, 6, 26, 23);
	private static final int TITLE_FONT = 645;
	private static final int SUBTITLE_FONT = 494;
	private static final int CATEGORY_FONT = 497;
	private static final int LIST_TOP = 62;
	private static final int LIST_LEFT = 20;
	private static final int ROW = 40;
	private static final int ICON = 32;
	private static final int SCROLLBAR_X = MAIN_WIDTH - 26;
	private static final int ARROW = 16;
	private static final int CATEGORY_TOP = 36;
	private static final int CATEGORY_ROW = 24;

	private static final Color INK = new Color(0x46, 0x32, 0x0a);
	private static final Color FADED_INK = new Color(0x8c, 0x74, 0x4c);
	private static final Color SELECTED = new Color(0x80, 0x00, 0x00);

	private final BankStandingPlugin plugin;
	private final Client client;
	private final SkillTileOverlay tile;
	private final ItemManager itemManager;
	private final Map<Integer, BufferedImage> sprites = new ConcurrentHashMap<>();
	private final Map<Integer, Image> goldIcons = new ConcurrentHashMap<>();

	private GameFont titleFont;
	private GameFont subtitleFont;
	private GameFont categoryFont;
	private GameFont selectedFont;
	/** The parchment turned on its side, for the sidebar; made once. */
	private BufferedImage rotatedParchment;
	private volatile boolean open;
	private volatile Unlocks.Category category = Unlocks.Category.REACH;
	/** The first row shown. */
	private volatile int scroll;
	private volatile int visibleRows = 1;
	/** Where the guide was last drawn, in canvas coordinates. */
	private volatile Rectangle drawn = new Rectangle();
	/** Set when a press was ours, so the release and click don't reach the game either. */
	private boolean swallowing;

	final MouseAdapter mouse = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			swallowing = false;
			Progress progress = plugin.getProgress();
			if (event.isAltDown() || progress == null)
			{
				return event;
			}
			Point point = event.getPoint();
			Rectangle guide = drawn;
			boolean onTile = SwingUtilities.isLeftMouseButton(event) && tile.getBounds().contains(point);
			boolean onGuide = open && guide.contains(point);
			if (onTile)
			{
				open = !open;
				if (open)
				{
					showNext(progress.level());
				}
			}
			else if (onGuide)
			{
				click(point.x - guide.x, point.y - guide.y);
			}
			else
			{
				open = false;
			}
			swallowing = onTile || onGuide;
			if (swallowing)
			{
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

	final MouseWheelListener wheel = event ->
	{
		if (open && drawn.contains(event.getPoint()))
		{
			scrollBy(event.getWheelRotation());
			event.consume();
		}
		return event;
	};

	@Inject
	SkillGuideOverlay(BankStandingPlugin plugin, Client client, SkillTileOverlay tile, SpriteManager spriteManager,
		ItemManager itemManager)
	{
		this.plugin = plugin;
		this.client = client;
		this.tile = tile;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);

		for (int id : new int[]{TITLE_SCROLL, CLOSE, ARROW_UP, ARROW_DOWN, THUMB_TOP, THUMB_MIDDLE, THUMB_BOTTOM, TRACK})
		{
			spriteManager.getSpriteAsync(id, 0, image -> sprites.put(id, image));
		}
		for (Unlocks.Unlock unlock : Unlocks.ALL)
		{
			if (unlock.sprite > 0)
			{
				spriteManager.getSpriteAsync(unlock.sprite, 0, image -> sprites.put(unlock.sprite, image));
			}
		}
	}

	void close()
	{
		open = false;
	}

	/** Opens on the category of the next unlock, scrolled so it shows. */
	private void showNext(int level)
	{
		Unlocks.Unlock next = Unlocks.next(level);
		category = next == null ? Unlocks.Category.CAPE : next.category;
		List<Unlocks.Unlock> lines = Unlocks.in(category);
		scroll = Math.max(0, (next == null ? 0 : lines.indexOf(next)) - 1);
		scrollBy(0);
	}

	private void click(int x, int y)
	{
		if (CLOSE_BUTTON.contains(x, y))
		{
			open = false;
			return;
		}
		if (SIDEBAR.contains(x, y))
		{
			int index = (y - SIDEBAR.y - CATEGORY_TOP + CATEGORY_ROW - 4) / CATEGORY_ROW;
			Unlocks.Category[] all = Unlocks.Category.values();
			if (index >= 0 && index < all.length)
			{
				category = all[index];
				scroll = 0;
			}
			return;
		}
		if (x >= SCROLLBAR_X && x < SCROLLBAR_X + ARROW)
		{
			int bottom = LIST_TOP + visibleRows * ROW;
			if (y >= LIST_TOP && y < LIST_TOP + ARROW)
			{
				scrollBy(-1);
			}
			else if (y >= bottom - ARROW && y < bottom)
			{
				scrollBy(1);
			}
		}
	}

	private void scrollBy(int rows)
	{
		int max = Math.max(0, Unlocks.in(category).size() - visibleRows);
		scroll = Math.max(0, Math.min(max, scroll + rows));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Progress progress = plugin.getProgress();
		BufferedImage parchment = sprites.get(TITLE_SCROLL);
		if (!open || progress == null || parchment == null)
		{
			return null;
		}
		int level = progress.level();
		int x = client.getViewportXOffset() + Math.max(0, (client.getViewportWidth() - WIDTH) / 2);
		int y = client.getViewportYOffset() + Math.max(0, (client.getViewportHeight() - HEIGHT) / 2);
		drawn = new Rectangle(x, y, WIDTH, HEIGHT);

		ninePatch(graphics, parchment, x, y, MAIN_WIDTH, HEIGHT, 16, 6);
		drawSidebar(graphics, parchment, x + SIDEBAR.x, y + SIDEBAR.y);
		draw(graphics, CLOSE, x + CLOSE_BUTTON.x, y + CLOSE_BUTTON.y);

		if (titleFont == null)
		{
			titleFont = GameFont.load(client, TITLE_FONT, INK);
			subtitleFont = GameFont.load(client, SUBTITLE_FONT, INK);
			categoryFont = GameFont.load(client, CATEGORY_FONT, INK);
			selectedFont = GameFont.load(client, CATEGORY_FONT, SELECTED);
		}
		if (titleFont != null && subtitleFont != null)
		{
			titleFont.drawCentred(graphics, "Bank Standing", x + MAIN_WIDTH / 2, y + 8);
			subtitleFont.drawCentred(graphics, category.title, x + MAIN_WIDTH / 2, y + 8 + titleFont.height());
		}
		graphics.setFont(FontManager.getRunescapeFont());

		List<Unlocks.Unlock> lines = Unlocks.in(category);
		visibleRows = (HEIGHT - LIST_TOP - 14) / ROW;
		scrollBy(0);
		int first = scroll;
		FontMetrics metrics = graphics.getFontMetrics();
		int rowY = y + LIST_TOP;
		for (int i = first; i < Math.min(lines.size(), first + visibleRows); i++)
		{
			Unlocks.Unlock unlock = lines.get(i);
			drawIcon(graphics, icon(unlock), x + LIST_LEFT, rowY + (ROW - ICON) / 2);
			graphics.setColor(level >= unlock.level ? INK : FADED_INK);
			int baseline = rowY + (ROW + metrics.getAscent() - metrics.getDescent()) / 2;
			String number = String.valueOf(unlock.level);
			graphics.drawString(number, x + LIST_LEFT + ICON + 30 - metrics.stringWidth(number), baseline);
			graphics.drawString(unlock.text, x + LIST_LEFT + ICON + 40, baseline);
			rowY += ROW;
		}
		if (lines.size() > visibleRows)
		{
			drawScrollbar(graphics, x + SCROLLBAR_X, y + LIST_TOP, visibleRows * ROW, lines.size(), first);
		}
		return new Dimension(WIDTH, HEIGHT);
	}

	private void drawSidebar(Graphics2D graphics, BufferedImage parchment, int x, int y)
	{
		ninePatch(graphics, rotated(parchment), x, y, SIDEBAR.width, SIDEBAR.height, 6, 16);
		if (categoryFont == null || selectedFont == null)
		{
			return;
		}
		int rowY = y + CATEGORY_TOP;
		for (Unlocks.Category each : Unlocks.Category.values())
		{
			(each == category ? selectedFont : categoryFont).draw(graphics, each.title, x + 22,
				rowY - categoryFont.height() + 4);
			rowY += CATEGORY_ROW;
		}
	}

	private void drawScrollbar(Graphics2D graphics, int x, int y, int height, int total, int first)
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
		int thumbHeight = Math.max(12, trackHeight * visibleRows / total);
		int thumbY = y + ARROW + (trackHeight - thumbHeight) * first / Math.max(1, total - visibleRows);
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

	private Image icon(Unlocks.Unlock unlock)
	{
		if (unlock.sprite > 0)
		{
			return sprites.get(unlock.sprite);
		}
		if (!unlock.gold)
		{
			// A big stack of coins, without the number on it.
			return itemManager.getImage(unlock.item, unlock.item == 995 ? 10_000 : 1, false);
		}
		Image gold = goldIcons.get(unlock.item);
		if (gold == null)
		{
			AsyncBufferedImage image = itemManager.getImage(unlock.item);
			goldIcons.put(unlock.item, image);
			image.onLoaded(() -> goldIcons.put(unlock.item, gilded(image)));
			return image;
		}
		return gold;
	}

	/** The same picture in gold, keeping its light and shade. */
	private static BufferedImage gilded(BufferedImage image)
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
				int gr = Math.round(255 * light);
				int gg = Math.round(200 * light);
				int gb = Math.round(60 * light);
				out.setRGB(px, py, alpha << 24 | gr << 16 | gg << 8 | gb);
			}
		}
		return out;
	}

	private BufferedImage rotated(BufferedImage source)
	{
		if (rotatedParchment == null)
		{
			BufferedImage out = new BufferedImage(source.getHeight(), source.getWidth(), BufferedImage.TYPE_INT_ARGB);
			for (int sy = 0; sy < source.getHeight(); sy++)
			{
				for (int sx = 0; sx < source.getWidth(); sx++)
				{
					out.setRGB(source.getHeight() - 1 - sy, sx, source.getRGB(sx, sy));
				}
			}
			rotatedParchment = out;
		}
		return rotatedParchment;
	}

	/** Stretches a sprite to any size, keeping its edges (cx wide, cy tall) unstretched. */
	private static void ninePatch(Graphics2D graphics, BufferedImage source, int x, int y, int width, int height,
		int cx, int cy)
	{
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

	/** Draws an icon shrunk, if it has to be, to fit a 32 by 32 box, centred in it. */
	private static void drawIcon(Graphics2D graphics, Image icon, int x, int y)
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
		double scale = Math.min(1.0, Math.min((double) ICON / width, (double) ICON / height));
		int w = (int) Math.round(width * scale);
		int h = (int) Math.round(height * scale);
		graphics.drawImage(icon, x + (ICON - w) / 2, y + (ICON - h) / 2, w, h, null);
	}

	private void draw(Graphics2D graphics, int sprite, int x, int y)
	{
		BufferedImage image = sprites.get(sprite);
		if (image != null)
		{
			graphics.drawImage(image, x, y, null);
		}
	}
}
