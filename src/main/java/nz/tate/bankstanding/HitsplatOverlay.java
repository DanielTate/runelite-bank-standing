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
import java.awt.image.BufferedImage;
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
 */
class HitsplatOverlay extends Overlay
{
	private static final int DAMAGE_SPRITE = 1359;
	/** The yellow disease splat, for max hits and boosted hits. Unconfirmed: falls back to red. */
	private static final int RARE_SPRITE = Integer.getInteger("bankstanding.rareSplat", 1361);

	private final Client client;
	private final SpriteManager spriteManager;
	private final DropFeed feed;

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

		Point at = player.getCanvasImageLocation(splat, player.getLogicalHeight() / 2);
		if (at == null)
		{
			return null;
		}
		graphics.drawImage(splat, at.getX(), at.getY(), null);

		graphics.setFont(FontManager.getRunescapeSmallFont());
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
		BufferedImage splat = drop.isMaxHit() || drop.isBoosted() ? spriteManager.getSprite(RARE_SPRITE, 0) : null;
		return splat != null ? splat : spriteManager.getSprite(DAMAGE_SPRITE, 0);
	}
}
