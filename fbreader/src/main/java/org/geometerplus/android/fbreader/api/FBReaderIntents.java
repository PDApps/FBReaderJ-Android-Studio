/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.android.fbreader.api;

import android.content.Intent;

import org.geometerplus.fbreader.book.*;

public abstract class FBReaderIntents {
	public static final String DEFAULT_PACKAGE = "org.pdapps.dicti2";

	public interface Action {
		String API                              = "android.fbreader.action.API";
		String API_CALLBACK                     = "android.fbreader.action.API_CALLBACK";
		String CONFIG_SERVICE                   = "android.fbreader.action.CONFIG_SERVICE";
		String LIBRARY_SERVICE                  = "android.fbreader.action.LIBRARY_SERVICE";
		String CLOSE                            = "android.fbreader.action.CLOSE";
		String PLUGIN_CRASH                     = "android.fbreader.action.PLUGIN_CRASH";

		String PLUGIN_VIEW                      = "android.fbreader.action.plugin.VIEW";
		String PLUGIN_CONNECT_COVER_SERVICE     = "android.fbreader.action.plugin.CONNECT_COVER_SERVICE";
	}

	public interface Event {
		String CONFIG_OPTION_CHANGE             = "fbreader.config_service.option_change_event";

		String LIBRARY_BOOK                     = "fbreader.library_service.book_event";
		String LIBRARY_BUILD                    = "fbreader.library_service.build_event";
		String LIBRARY_COVER_READY              = "fbreader.library_service.cover_ready";
	}

	public interface Key {
		String BOOK                             = "fbreader.book";
		String BOOKMARK                         = "fbreader.bookmark";
		String PLUGIN                           = "fbreader.plugin";
		String TYPE                             = "fbreader.type";
	}

	public static Intent defaultInternalIntent(String action) {
		return internalIntent(action).addCategory(Intent.CATEGORY_DEFAULT);
	}

	public static Intent internalIntent(String action) {
		return new Intent(action).setPackage(DEFAULT_PACKAGE);
	}

	public static void putBookExtra(Intent intent, String key, Book book) {
		intent.putExtra(key, SerializerUtil.serialize(book));
	}

	public static void putBookExtra(Intent intent, Book book) {
		putBookExtra(intent, Key.BOOK, book);
	}

	public static <B extends AbstractBook> B getBookExtra(Intent intent, String key, AbstractSerializer.BookCreator<B> creator) {
		return SerializerUtil.deserializeBook(intent.getStringExtra(key), creator);
	}

	public static <B extends AbstractBook> B getBookExtra(Intent intent, AbstractSerializer.BookCreator<B> creator) {
		return getBookExtra(intent, Key.BOOK, creator);
	}

	public static void putBookmarkExtra(Intent intent, String key, Bookmark bookmark) {
		intent.putExtra(key, SerializerUtil.serialize(bookmark));
	}

	public static void putBookmarkExtra(Intent intent, Bookmark bookmark) {
		putBookmarkExtra(intent, Key.BOOKMARK, bookmark);
	}

	public static Bookmark getBookmarkExtra(Intent intent, String key) {
		return SerializerUtil.deserializeBookmark(intent.getStringExtra(key));
	}

	public static Bookmark getBookmarkExtra(Intent intent) {
		return getBookmarkExtra(intent, Key.BOOKMARK);
	}
}
