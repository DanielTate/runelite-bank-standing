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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A window drawn over the game view, like the game's own interfaces: centred, opened and
 * closed by the plugin. Clicks and scrolling inside it are kept from the game; a click outside
 * closes it. Subclasses draw it and handle clicks in window coordinates.
 */
abstract class GameWindow extends Overlay
{
	protected final Client client;
	protected final GuideArt art;

	private volatile boolean open;
	/** Where the window was last drawn, in canvas coordinates, and when (System.nanoTime). */
	private volatile Rectangle drawn = new Rectangle();
	private volatile long drawnAt;
	/** Set when a press was ours, so the release and click don't reach the game either. */
	private boolean swallowing;
	/** Run whenever this window opens, so the plugin can close the others. */
	private volatile Runnable onOpen;
	/** A window not drawn for this long is treated as gone, for clicks. */
	static final long STALE_NANOS = 1_000_000_000L;

	final MouseAdapter mouse = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			swallowing = false;
			// Alt is for moving overlays; consumed means another window already took it.
			if (event.isAltDown() || event.isConsumed())
			{
				return event;
			}
			Point point = event.getPoint();
			Rectangle window = visible();
			if (open && window.contains(point))
			{
				if (SwingUtilities.isLeftMouseButton(event))
				{
					click(point.x - window.x, point.y - window.y);
				}
				swallowing = true;
			}
			else if (claimPress(event))
			{
				swallowing = true;
			}
			else
			{
				open = false;
			}
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
		Rectangle window = visible();
		if (open && !event.isConsumed() && window.contains(event.getPoint()))
		{
			wheel(event.getX() - window.x, event.getY() - window.y, event.getWheelRotation());
			event.consume();
		}
		return event;
	};

	GameWindow(Client client, GuideArt art)
	{
		this.client = client;
		this.art = art;
		setPosition(OverlayPosition.DYNAMIC);
		// Always on top, so the resizable side panel and other plugins' bars can't cover it.
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
	}

	/** A left press inside the window, in window coordinates. Mouse thread. */
	abstract void click(int x, int y);

	/** The mouse wheel inside the window, in window coordinates. Mouse thread. */
	abstract void wheel(int x, int y, int rotation);

	/** Whether there's anything to show, such as being logged in. */
	boolean available()
	{
		return true;
	}

	/** A press outside the window, such as on the button that opens it; true if used. */
	boolean claimPress(MouseEvent event)
	{
		return false;
	}

	void setOnOpen(Runnable onOpen)
	{
		this.onOpen = onOpen;
	}

	void open()
	{
		open = true;
		Runnable callback = onOpen;
		if (callback != null)
		{
			callback.run();
		}
	}

	void close()
	{
		open = false;
	}

	boolean isOpen()
	{
		return open;
	}

	/** Whether to draw this frame: open, available and with the art loaded. */
	boolean showing()
	{
		boolean showing = open && available() && art.ready();
		if (!showing)
		{
			drawn = new Rectangle();
		}
		return showing;
	}

	/**
	 * Where the window is on screen now, or empty. Overlays can stop being drawn without being
	 * told (hidden overlays, a minimised client), so an old position doesn't count.
	 */
	private Rectangle visible()
	{
		return System.nanoTime() - drawnAt < STALE_NANOS ? drawn : new Rectangle();
	}

	/** Centres a window of this size in the game view and remembers where, for clicks. */
	Rectangle place(int width, int height)
	{
		int x = client.getViewportXOffset() + Math.max(0, (client.getViewportWidth() - width) / 2);
		int y = client.getViewportYOffset() + Math.max(0, (client.getViewportHeight() - height) / 2);
		Rectangle window = new Rectangle(x, y, width, height);
		drawn = window;
		drawnAt = System.nanoTime();
		return window;
	}
}
