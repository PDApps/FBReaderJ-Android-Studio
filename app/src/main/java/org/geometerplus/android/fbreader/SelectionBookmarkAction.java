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
import android.content.Intent;
import android.os.Parcelable;
import android.view.View;

import com.github.johnpersano.supertoasts.library.Style;
import com.github.johnpersano.supertoasts.library.SuperActivityToast;

import org.geometerplus.android.fbreader.util.FBReaderAdapter;
import org.geometerplus.zlibrary.core.resources.ZLResource;

import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.fbreader.bookmark.EditBookmarkActivity;
import org.geometerplus.android.util.OrientationUtil;

public class SelectionBookmarkAction extends FBAndroidAction {
	private FBReaderAdapter fbReaderAdapter;

	SelectionBookmarkAction(Activity baseApplication, FBReaderApp fbreader, FBReaderAdapter readerAdapter) {
		super(baseApplication, fbreader);
		this.fbReaderAdapter = readerAdapter;
	}

	@Override
	protected void run(Object... params) {
		final Bookmark bookmark;
		if (params.length != 0) {
			bookmark = (Bookmark) params[0];
		} else {
			bookmark = Reader.addSelectionBookmark();
		}
		if (bookmark == null) {
			return;
		}

		final SuperActivityToast toast =
				new SuperActivityToast(BaseActivity, Style.TYPE_BUTTON);
		toast.setText(bookmark.getText());
		toast.setDuration(Style.DURATION_VERY_LONG);
		toast.setButtonIconResource(android.R.drawable.ic_menu_edit);
		toast.setOnButtonClickListener("bkmk", null, new SuperActivityToast.OnButtonClickListener() {
			@Override
			public void onClick(View view, Parcelable token) {
				final Intent intent =
						new Intent(BaseActivity.getApplicationContext(), EditBookmarkActivity.class);
				FBReaderIntents.putBookmarkExtra(intent, bookmark);
				OrientationUtil.startActivity(BaseActivity, intent);
			}
		});
		fbReaderAdapter.showToast(toast);
	}
}
