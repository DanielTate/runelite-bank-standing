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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws each drop as a hitsplat on the player, using the game's own splat sprites. The API
 * can't add real hitsplats, so this is drawn on top of the scene; only this client sees it.
 *
 * <p>Normal drops are red. Hitting the normal max for your level gives a brighter red splat
 * with a gold border. Anything above the normal max (only possible with a gear bonus) gives
 * the yellow disease splat.
 */
class HitsplatOverlay extends Overlay
{
	private static final int DAMAGE_SPRITE = 1359;
	/** The yellow disease splat, for boosted hits. Unconfirmed: falls back to red. */
	private static final int BOOSTED_SPRITE = 1361;
	/** Cache key for the bordered max-hit splat, which is drawn from the red one. */
	private static final int MAX_HIT_KEY = -1;
	private static final Color MAX_HIT_BORDER = new Color(240, 196, 80);
	private static final float MAX_HIT_BRIGHTEN = 1.2f;
	private static final float SCALE = 1.5f;
	/** How far above the top of the player's model the splat sits, in local units. */
	private static final int ABOVE_HEAD = 40;

	private final Client client;
	private final SpriteManager spriteManager;
	private final DropFeed feed;
	private final Map<Integer, BufferedImage> scaledSprites = new HashMap<>();

	@Inject
	HitsplatOverlay(Client client, SpriteManager spriteManager, DropFeed feed)
	{
		this.client = client;
		this.spriteManager = spriteManager;
		this.feed = feed;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		DropFeed.Drop[] drops = feed.recent(DropFeed.HITSPLAT_MILLIS);
		Player player = client.getLocalPlayer();
		if (drops.length == 0 || player == null)
		{
			return null;
		}

		DropFeed.Drop drop = drops[drops.length - 1];
		BufferedImage splat = splat(drop);
		if (splat == null)
		{
			return null;
		}

		Point at = player.getCanvasImageLocation(splat, player.getLogicalHeight() + ABOVE_HEAD);
		if (at == null)
		{
			return null;
		}
		graphics.drawImage(splat, at.getX(), at.getY(), null);

		graphics.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics metrics = graphics.getFontMetrics();
		String text = String.valueOf(drop.getAmount());
		int x = at.getX() + (splat.getWidth() - metrics.stringWidth(text)) / 2;
		int y = at.getY() + (splat.getHeight() + metrics.getAscent()) / 2 - 1;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(Color.WHITE);
		graphics.drawString(text, x, y);
		return null;
	}

	private BufferedImage splat(DropFeed.Drop drop)
	{
		BufferedImage splat = null;
		if (drop.isBoosted())
		{
			splat = scaled(BOOSTED_SPRITE);
		}
		else if (drop.isMaxHit())
		{
			splat = maxHitSplat();
		}
		return splat != null ? splat : scaled(DAMAGE_SPRITE);
	}

	private BufferedImage maxHitSplat()
	{
		BufferedImage cached = scaledSprites.get(MAX_HIT_KEY);
		if (cached == null)
		{
			BufferedImage red = scaled(DAMAGE_SPRITE);
			if (red == null)
			{
				return null;
			}
			cached = bordered(red);
			scaledSprites.put(MAX_HIT_KEY, cached);
		}
		return cached;
	}

	/** The splat a little brighter, with a one-pixel gold border around its shape. */
	static BufferedImage bordered(BufferedImage splat)
	{
		int w = splat.getWidth() + 2;
		int h = splat.getHeight() + 2;
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int argb = pixel(splat, x - 1, y - 1);
				if ((argb >>> 24) > 0)
				{
					out.setRGB(x, y, brighten(argb));
				}
				else if (opaque(splat, x - 2, y - 1) || opaque(splat, x, y - 1)
					|| opaque(splat, x - 1, y - 2) || opaque(splat, x - 1, y))
				{
					out.setRGB(x, y, MAX_HIT_BORDER.getRGB());
				}
			}
		}
		return out;
	}

	private static int pixel(BufferedImage image, int x, int y)
	{
		return x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight() ? 0 : image.getRGB(x, y);
	}

	private static boolean opaque(BufferedImage image, int x, int y)
	{
		return (pixel(image, x, y) >>> 24) > 0;
	}

	private static int brighten(int argb)
	{
		int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * MAX_HIT_BRIGHTEN));
		int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * MAX_HIT_BRIGHTEN));
		int b = Math.min(255, Math.round((argb & 0xFF) * MAX_HIT_BRIGHTEN));
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	/** The sprite enlarged by {@link #SCALE}, pixel for pixel so it stays crisp. */
	private BufferedImage scaled(int id)
	{
		BufferedImage cached = scaledSprites.get(id);
		if (cached != null)
		{
			return cached;
		}
		BufferedImage sprite = spriteManager.getSprite(id, 0);
		if (sprite == null)
		{
			return null;
		}
		BufferedImage big = new BufferedImage(Math.round(sprite.getWidth() * SCALE),
			Math.round(sprite.getHeight() * SCALE), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = big.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(sprite, 0, 0, big.getWidth(), big.getHeight(), null);
		g.dispose();
		scaledSprites.put(id, big);
		return big;
	}
}
