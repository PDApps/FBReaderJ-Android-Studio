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

package org.geometerplus.android.fbreader;

import android.app.Activity;

import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class SelectionHidePanelAction extends FBAndroidAction {
    public SelectionHidePanelAction(Activity baseActivity, FBReaderApp fbreader) {
        super(baseActivity, fbreader);
    }

    @Override
    protected void run(Object... params) {
        hideSelectionPanel();
    }

    public void hideSelectionPanel() {
        final FBReaderApp.PopupPanel popup = Reader.getActivePopup();
        if (popup != null && popup.getId() == SelectionPopup.ID) {
            Reader.hideActivePopup();
        }
    }
}
