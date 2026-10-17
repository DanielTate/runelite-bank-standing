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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import nz.tate.bankstanding.core.XpRoller;

/**
 * A row of small boxes, like the game's own buff bar, one for each modifier helping your
 * stand right now: an emote with its seconds left, or a full set while it's worn.
 */
@Singleton
class ModifierWidget extends Overlay
{
	private static final int BOX = 32;
	private static final int GAP = 2;
	private static final Color BORDER = new Color(0, 0, 0, 160);
	private static final Color TOTAL_BORDER = new Color(214, 176, 48);
	private static final long WARN_MILLIS = 5000;

	/** One box. At most one of item and sprite is set; the other is -1. */
	static final class Shown
	{
		final String name;
		final String effect;
		final int item;
		final int sprite;
		/** When it runs out, in epoch millis, or 0 if it lasts while worn. */
		final long endsAt;
		/** What it adds to the total: max hit, and whether it shifts the rolls. */
		final int maxHit;
		final boolean rolls;
		/** A cooldown rather than a buff: drawn faded and left out of the total. */
		final boolean cooldown;

		Shown(String name, String effect, int item, int sprite, long endsAt, int maxHit, boolean rolls, boolean cooldown)
		{
			this.name = name;
			this.effect = effect;
			this.item = item;
			this.sprite = sprite;
			this.endsAt = endsAt;
			this.maxHit = maxHit;
			this.rolls = rolls;
			this.cooldown = cooldown;
		}

		static Shown buff(String name, String effect, int item, int sprite, long endsAt, int maxHit, boolean rolls)
		{
			return new Shown(name, effect, item, sprite, endsAt, maxHit, rolls, false);
		}

		static Shown cooldown(String name, String effect, int item, int sprite, long endsAt)
		{
			return new Shown(name, effect, item, sprite, endsAt, 0, false, true);
		}
	}

	private final Client client;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final TooltipManager tooltipManager;
	private final Icons icons;
	private final Font font = FontManager.getRunescapeSmallFont();

	private volatile List<Shown> shown = Collections.emptyList();

	@Inject
	ModifierWidget(Client client, ItemManager itemManager, SpriteManager spriteManager,
		TooltipManager tooltipManager, Icons icons)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.tooltipManager = tooltipManager;
		this.icons = icons;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.UNDER_WIDGETS);
		setMovable(true);
	}

	/** What to show; an empty list hides it. Called each tick from the client thread. */
	void show(List<Shown> modifiers)
	{
		shown = modifiers;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<Shown> boxes = shown;
		if (boxes.isEmpty())
		{
			return null;
		}
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		Rectangle bounds = getBounds();
		Point mouse = client.getMouseCanvasPosition();
		long now = System.currentTimeMillis();
		int x = 0;
		int maxHit = 0;
		boolean rolls = false;
		int buffs = 0;
		for (Shown box : boxes)
		{
			if (!box.cooldown)
			{
				buffs++;
				maxHit += box.maxHit;
				rolls |= box.rolls;
			}
			graphics.setColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
			graphics.fillRect(x, 0, BOX, BOX);
			graphics.setColor(BORDER);
			graphics.drawRect(x, 0, BOX - 1, BOX - 1);

			Composite before = graphics.getComposite();
			if (box.cooldown)
			{
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
			}
			BufferedImage image = image(box);
			if (image != null)
			{
				int w = Math.min(image.getWidth(), BOX - 4);
				int h = Math.min(image.getHeight(), BOX - 4);
				graphics.drawImage(image, x + (BOX - w) / 2, (BOX - h) / 2, w, h, null);
			}
			graphics.setComposite(before);

			if (box.endsAt > 0)
			{
				long left = Math.max(0, box.endsAt - now);
				Color colour = box.cooldown ? Color.LIGHT_GRAY : left <= WARN_MILLIS ? Color.RED : Color.WHITE;
				label(graphics, metrics, x, timeLeft(left), colour);
			}

			hover(bounds, mouse, x, box.name + "</br>" + box.effect
				+ (box.endsAt > 0 ? "</br>" + (box.cooldown ? "Ready in " : "Ends in ") + timeLeft(Math.max(0, box.endsAt - now)) : ""));
			x += BOX + GAP;
		}

		// The total only adds up the boxes shown, so it never gives away a hidden modifier.
		if (buffs >= 2)
		{
			maxHit = Math.min(maxHit, XpRoller.MAX_BONUS_HIT);
			graphics.setColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
			graphics.fillRect(x, 0, BOX, BOX);
			graphics.setColor(TOTAL_BORDER);
			graphics.drawRect(x, 0, BOX - 1, BOX - 1);
			BufferedImage icon = icons.small();
			graphics.drawImage(icon, x + (BOX - icon.getWidth()) / 2, 3, null);
			label(graphics, metrics, x, maxHit > 0 ? "+" + maxHit : "+", Color.GREEN);
			String effect = maxHit > 0 ? "+" + maxHit + " max hit" : "";
			if (rolls)
			{
				effect += (effect.isEmpty() ? "B" : ", b") + "etter rolls";
			}
			hover(bounds, mouse, x, "All together</br>" + effect);
			x += BOX + GAP;
		}
		return new Dimension(x - GAP, BOX);
	}

	/** Seconds under a minute, minutes after that. */
	private static String timeLeft(long millis)
	{
		long seconds = (millis + 999) / 1000;
		return seconds < 60 ? seconds + "s" : (seconds + 59) / 60 + "m";
	}

	private static void label(Graphics2D graphics, FontMetrics metrics, int x, String text, Color colour)
	{
		int tx = x + BOX - 2 - metrics.stringWidth(text);
		int ty = BOX - 3;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, tx + 1, ty + 1);
		graphics.setColor(colour);
		graphics.drawString(text, tx, ty);
	}

	private void hover(Rectangle bounds, Point mouse, int x, String text)
	{
		if (bounds != null && mouse != null && new Rectangle(bounds.x + x, bounds.y, BOX, BOX)
			.contains(mouse.getX(), mouse.getY()))
		{
			tooltipManager.add(new Tooltip(text));
		}
	}

	private BufferedImage image(Shown box)
	{
		if (box.item > 0)
		{
			return itemManager.getImage(box.item);
		}
		if (box.sprite > 0)
		{
			return spriteManager.getSprite(box.sprite, 0);
		}
		return icons.small();
	}
}
