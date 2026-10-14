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
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.runelite.api.Client;
import net.runelite.api.SpritePixels;

/**
 * Draws text in one of the game's own fonts. The game keeps each font as a sprite with one
 * frame per character; this loads those frames once and draws them in any colour, optionally
 * with the game's one-pixel black shadow.
 */
final class GameFont
{
	private final BufferedImage[] masks = new BufferedImage[256];
	private final Map<Integer, BufferedImage[]> tinted = new ConcurrentHashMap<>();
	private final int[] offsetX = new int[256];
	private final int[] offsetY = new int[256];
	private final int[] advance = new int[256];
	private final int height;

	private GameFont(SpritePixels[] frames)
	{
		int maxHeight = 0;
		int spaceWidth = 4;
		for (int c = 0; c < Math.min(256, frames.length); c++)
		{
			SpritePixels frame = frames[c];
			if (frame == null)
			{
				continue;
			}
			maxHeight = Math.max(maxHeight, frame.getMaxHeight());
			spaceWidth = Math.max(spaceWidth, frame.getMaxWidth() / 3);
			if (frame.getWidth() == 0)
			{
				continue;
			}
			masks[c] = frame.toBufferedImage();
			offsetX[c] = frame.getOffsetX();
			offsetY[c] = frame.getOffsetY();
			advance[c] = frame.getOffsetX() + frame.getWidth() + 1;
		}
		advance[' '] = spaceWidth;
		height = maxHeight;
	}

	/** Loads font {@code id}, or null if it isn't ready. Client thread only. */
	static GameFont load(Client client, int id)
	{
		SpritePixels[] frames = client.getSprites(client.getIndexSprites(), id, 0);
		return frames == null || frames.length == 0 ? null : new GameFont(frames);
	}

	int height()
	{
		return height;
	}

	int width(String text)
	{
		int width = 0;
		for (int i = 0; i < text.length(); i++)
		{
			width += advance[text.charAt(i) & 0xff];
		}
		return width;
	}

	/** Draws the text with its top edge at {@code top}; returns the x after it. */
	int draw(Graphics2D graphics, String text, int x, int top, Color colour)
	{
		BufferedImage[] glyphs = tinted.computeIfAbsent(colour.getRGB(), rgb -> tint(rgb));
		for (int i = 0; i < text.length(); i++)
		{
			int c = text.charAt(i) & 0xff;
			if (glyphs[c] != null)
			{
				graphics.drawImage(glyphs[c], x + offsetX[c], top + offsetY[c], null);
			}
			x += advance[c];
		}
		return x;
	}

	/** Draws the text over a black shadow one pixel down and right, like the game's interfaces. */
	int drawShadowed(Graphics2D graphics, String text, int x, int top, Color colour)
	{
		draw(graphics, text, x + 1, top + 1, Color.BLACK);
		return draw(graphics, text, x, top, colour);
	}

	void drawCentred(Graphics2D graphics, String text, int centreX, int top, Color colour)
	{
		draw(graphics, text, centreX - width(text) / 2, top, colour);
	}

	void drawShadowedCentred(Graphics2D graphics, String text, int centreX, int top, Color colour)
	{
		drawShadowed(graphics, text, centreX - width(text) / 2, top, colour);
	}

	private BufferedImage[] tint(int argb)
	{
		BufferedImage[] glyphs = new BufferedImage[256];
		int rgb = argb & 0xffffff;
		for (int c = 0; c < 256; c++)
		{
			BufferedImage mask = masks[c];
			if (mask == null)
			{
				continue;
			}
			BufferedImage out = new BufferedImage(mask.getWidth(), mask.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < mask.getHeight(); y++)
			{
				for (int x = 0; x < mask.getWidth(); x++)
				{
					int alpha = mask.getRGB(x, y) >>> 24;
					if (alpha != 0)
					{
						out.setRGB(x, y, alpha << 24 | rgb);
					}
				}
			}
			glyphs[c] = out;
		}
		return glyphs;
	}
}
