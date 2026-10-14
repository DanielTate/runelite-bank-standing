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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import net.runelite.api.Client;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A window drawn like the game's skill guide: a parchment scroll with a title and a scrolling
 * list, a second scroll at the side with tabs, and the close button. Subclasses supply the
 * tabs and draw the rows. Clicks inside are kept from the game; a click outside closes it.
 */
abstract class ParchmentWindow extends Overlay
{
	static final int WIDTH = 500;
	static final int HEIGHT = 320;
	static final int MAIN_WIDTH = 350;
	static final int LIST_TOP = 62;
	static final int LIST_LEFT = 20;
	static final int LIST_BOTTOM = HEIGHT - 14;
	static final int SCROLLBAR_X = MAIN_WIDTH - 26;
	/** How wide the rows can be, up to the scrollbar. */
	static final int LIST_WIDTH = SCROLLBAR_X - LIST_LEFT - 4;
	private static final Rectangle SIDEBAR = new Rectangle(358, 40, 140, 270);
	private static final Rectangle CLOSE_BUTTON = new Rectangle(WIDTH - 28, 6, 26, 23);
	private static final int TAB_TOP = 36;
	private static final int TAB_ROW = 24;

	protected final Client client;
	protected final GuideArt art;

	private volatile boolean open;
	protected volatile int tab;
	/** The first row shown. */
	protected volatile int scroll;
	private volatile int visibleRows = 1;
	/** Where the window was last drawn, in canvas coordinates. */
	private volatile Rectangle drawn = new Rectangle();
	/** Set when a press was ours, so the release and click don't reach the game either. */
	private boolean swallowing;
	/** Run whenever this window opens, so the plugin can close the others. */
	private volatile Runnable onOpen;

	final MouseAdapter mouse = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			swallowing = false;
			if (event.isAltDown())
			{
				return event;
			}
			Point point = event.getPoint();
			Rectangle window = drawn;
			if (claimPress(event))
			{
				swallowing = true;
			}
			else if (open && window.contains(point))
			{
				click(point.x - window.x, point.y - window.y);
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
		if (open && drawn.contains(event.getPoint()))
		{
			scrollBy(event.getWheelRotation());
			event.consume();
		}
		return event;
	};

	ParchmentWindow(Client client, GuideArt art)
	{
		this.client = client;
		this.art = art;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	abstract String title();

	abstract String subtitle();

	abstract String[] tabs();

	abstract int rowHeight();

	abstract int rowCount();

	/** Draws one row with its top left corner at (x, y), at most {@link #LIST_WIDTH} wide. */
	abstract void drawRow(Graphics2D graphics, int row, int x, int y);

	/** Whether there's anything to show, such as being logged in. */
	boolean available()
	{
		return true;
	}

	/** A press before the window's own handling, such as on the button that opens it; true if used. */
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

	void showTab(int index)
	{
		tab = index;
		scroll = 0;
	}

	void scrollTo(int row)
	{
		scroll = row;
		scrollBy(0);
	}

	void scrollBy(int rows)
	{
		int max = Math.max(0, rowCount() - visibleRows);
		scroll = Math.max(0, Math.min(max, scroll + rows));
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
			int index = (y - SIDEBAR.y - TAB_TOP + TAB_ROW - 4) / TAB_ROW;
			if (index >= 0 && index < tabs().length)
			{
				showTab(index);
			}
			return;
		}
		if (x >= SCROLLBAR_X && x < SCROLLBAR_X + GuideArt.ARROW)
		{
			int bottom = LIST_TOP + visibleRows * rowHeight();
			if (y >= LIST_TOP && y < LIST_TOP + GuideArt.ARROW)
			{
				scrollBy(-1);
			}
			else if (y >= bottom - GuideArt.ARROW && y < bottom)
			{
				scrollBy(1);
			}
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!open || !available() || !art.ready())
		{
			return null;
		}
		int x = client.getViewportXOffset() + Math.max(0, (client.getViewportWidth() - WIDTH) / 2);
		int y = client.getViewportYOffset() + Math.max(0, (client.getViewportHeight() - HEIGHT) / 2);
		drawn = new Rectangle(x, y, WIDTH, HEIGHT);

		art.parchment(graphics, x, y, MAIN_WIDTH, HEIGHT);
		art.sideParchment(graphics, x + SIDEBAR.x, y + SIDEBAR.y, SIDEBAR.width, SIDEBAR.height);
		art.draw(graphics, GuideArt.CLOSE, x + CLOSE_BUTTON.x, y + CLOSE_BUTTON.y);

		art.title.drawCentred(graphics, title(), x + MAIN_WIDTH / 2, y + 8);
		art.subtitle.drawCentred(graphics, subtitle(), x + MAIN_WIDTH / 2, y + 8 + art.title.height());

		String[] tabs = tabs();
		int tabY = y + SIDEBAR.y + TAB_TOP;
		for (int i = 0; i < tabs.length; i++)
		{
			(i == tab ? art.selected : art.category).draw(graphics, tabs[i], x + SIDEBAR.x + 22,
				tabY - art.category.height() + 4);
			tabY += TAB_ROW;
		}

		visibleRows = Math.max(1, (LIST_BOTTOM - LIST_TOP) / rowHeight());
		scrollBy(0);
		int first = scroll;
		int rows = rowCount();
		for (int row = first; row < Math.min(rows, first + visibleRows); row++)
		{
			drawRow(graphics, row, x + LIST_LEFT, y + LIST_TOP + (row - first) * rowHeight());
		}
		if (rows > visibleRows)
		{
			art.scrollbar(graphics, x + SCROLLBAR_X, y + LIST_TOP, visibleRows * rowHeight(), rows, visibleRows, first);
		}
		return new Dimension(WIDTH, HEIGHT);
	}
}
