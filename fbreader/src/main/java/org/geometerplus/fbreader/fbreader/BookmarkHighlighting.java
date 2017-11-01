/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.fbreader;

import android.graphics.Color;

import org.geometerplus.android.fbreader.util.DBookmark;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.text.view.ZLTextSimpleHighlighting;
import org.geometerplus.zlibrary.text.view.ZLTextView;

public final class BookmarkHighlighting extends ZLTextSimpleHighlighting {
    BookmarkHighlighting(ZLTextView view, DBookmark dBookmark) {
        super(view, dBookmark);
    }

    @Override
    public ZLColor getBackgroundColor() {
        return new ZLColor(getDBookmark().getColor());
    }

    @Override
    public ZLColor getForegroundColor() {
        return View.getSelectionForegroundColor();
    }

    @Override
    public ZLColor getOutlineColor() {
        if (getDBookmark().isSelected()) {
            int color = getDBookmark().getColor();
            color = manipulateColor(color, 0.9f);
            return new ZLColor(color);
        } else {
            return null;
        }
    }

    private static int manipulateColor(int color, float factor) {
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.rgb(Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }
}
