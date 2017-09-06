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

package org.geometerplus.android.fbreader.libraryService;

import java.util.*;

import android.content.*;
import android.os.RemoteException;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.zlibrary.core.options.Config;

import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;

import org.geometerplus.fbreader.book.*;

public class BookCollectionShadow extends AbstractBookCollection<Book> {
	private volatile LibraryService.LibraryImplementation myInterface;
	private static BookCollectionShadow mInstance;

	public static void onMessage(Intent intent) {
		if (mInstance == null) {
			System.out.println("BookCollectionShadow is null");
			return;
		}
		if (mInstance.myReceiver == null) {
			return;
		}
		mInstance.myReceiver.onReceive(null, intent);
	}

	private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (!hasListeners()) {
				return;
			}

			try {
				final String type = intent.getStringExtra("type");
				if (FBReaderIntents.Event.LIBRARY_BOOK.equals(intent.getAction())) {
					final Book book = SerializerUtil.deserializeBook(intent.getStringExtra("book"), BookCollectionShadow.this);
					fireBookEvent(BookEvent.valueOf(type), book);
				} else {
					fireBuildEvent(Status.valueOf(type));
				}
			} catch (Exception e) {
				// ignore
			}
		}
	};

	public BookCollectionShadow() {
		mInstance = this;
		myInterface = LibraryService.getImplementation();
	}

	public synchronized void bindToService(Context context, Runnable onBindAction) {
		if (onBindAction != null) {
			Config.Instance().runOnConnect(onBindAction);
		}
	}

	public synchronized void reset(boolean force) {
		if (myInterface != null) {
			myInterface.reset(force);
		}
	}

	public synchronized int size() {
		if (myInterface == null) {
			return 0;
		}
		return myInterface.size();
	}

	public synchronized Status status() {
		if (myInterface == null) {
			return Status.NotStarted;
		}
			return Status.valueOf(myInterface.status());
	}

	public List<Book> books(final BookQuery query) {
		return listCall(new ListCallable<Book>() {
			public List<Book> call() throws RemoteException {
				return SerializerUtil.deserializeBookList(
					myInterface.books(SerializerUtil.serialize(query)), BookCollectionShadow.this
				);
			}
		});
	}

	public synchronized boolean hasBooks(Filter filter) {
		if (myInterface == null) {
			return false;
		}
			return myInterface.hasBooks(SerializerUtil.serialize(new BookQuery(filter, 1)));
	}

	public List<Book> recentlyAddedBooks(final int count) {
		return listCall(new ListCallable<Book>() {
			public List<Book> call() throws RemoteException {
				return SerializerUtil.deserializeBookList(
					myInterface.recentlyAddedBooks(count), BookCollectionShadow.this
				);
			}
		});
	}

	public List<Book> recentlyOpenedBooks(final int count) {
		return listCall(new ListCallable<Book>() {
			public List<Book> call() throws RemoteException {
				return SerializerUtil.deserializeBookList(
					myInterface.recentlyOpenedBooks(count), BookCollectionShadow.this
				);
			}
		});
	}

	public synchronized Book getRecentBook(int index) {
		if (myInterface == null) {
			return null;
		}
			return SerializerUtil.deserializeBook(myInterface.getRecentBook(index), this);
	}

	public synchronized Book getBookByFile(String path) {
		if (myInterface == null) {
			return null;
		}
			return SerializerUtil.deserializeBook(myInterface.getBookByFile(path), this);
	}

	public synchronized Book getBookById(long id) {
		if (myInterface == null) {
			return null;
		}
			return SerializerUtil.deserializeBook(myInterface.getBookById(id), this);
	}

	public synchronized Book getBookByUid(UID uid) {
		if (myInterface == null) {
			return null;
		}
			return SerializerUtil.deserializeBook(myInterface.getBookByUid(uid.Type, uid.Id), this);
	}

	public synchronized Book getBookByHash(String hash) {
		if (myInterface == null) {
			return null;
		}
			return SerializerUtil.deserializeBook(myInterface.getBookByHash(hash), this);
	}

	public List<Author> authors() {
		return listCall(new ListCallable<Author>() {
			public List<Author> call() throws RemoteException {
				final List<String> strings = myInterface.authors();
				final List<Author> authors = new ArrayList<Author>(strings.size());
				for (String s : strings) {
					authors.add(Util.stringToAuthor(s));
				}
				return authors;
			}
		});
	}

	public List<Tag> tags() {
		return listCall(new ListCallable<Tag>() {
			public List<Tag> call() throws RemoteException {
				final List<String> strings = myInterface.tags();
				final List<Tag> tags = new ArrayList<Tag>(strings.size());
				for (String s : strings) {
					tags.add(Util.stringToTag(s));
				}
				return tags;
			}
		});
	}

	public synchronized boolean hasSeries() {
		if (myInterface != null) {
				return myInterface.hasSeries();
		}
		return false;
	}

	public List<String> series() {
		return listCall(new ListCallable<String>() {
			public List<String> call() throws RemoteException {
				return myInterface.series();
			}
		});
	}

	public List<String> titles(final BookQuery query) {
		return listCall(new ListCallable<String>() {
			public List<String> call() throws RemoteException {
				return myInterface.titles(SerializerUtil.serialize(query));
			}
		});
	}

	public List<String> firstTitleLetters() {
		return listCall(new ListCallable<String>() {
			public List<String> call() throws RemoteException {
				return myInterface.firstTitleLetters();
			}
		});
	}

	public synchronized boolean saveBook(Book book) {
		if (myInterface == null) {
			return false;
		}
			return myInterface.saveBook(SerializerUtil.serialize(book));
	}

	public synchronized boolean canRemoveBook(Book book, boolean deleteFromDisk) {
		if (myInterface == null) {
			return false;
		}
			return myInterface.canRemoveBook(SerializerUtil.serialize(book), deleteFromDisk);
	}

	public synchronized void removeBook(Book book, boolean deleteFromDisk) {
		if (myInterface != null) {
				myInterface.removeBook(SerializerUtil.serialize(book), deleteFromDisk);
		}
	}

	public synchronized void addToRecentlyOpened(Book book) {
		if (myInterface != null) {
				myInterface.addToRecentlyOpened(SerializerUtil.serialize(book));
		}
	}

	public synchronized void removeFromRecentlyOpened(Book book) {
		if (myInterface != null) {
				myInterface.removeFromRecentlyOpened(SerializerUtil.serialize(book));
		}
	}

	public List<String> labels() {
		return listCall(new ListCallable<String>() {
			public List<String> call() throws RemoteException {
				return myInterface.labels();
			}
		});
	}

	public String getHash(Book book, boolean force) {
		if (myInterface == null) {
			return null;
		}
			return myInterface.getHash(SerializerUtil.serialize(book), force);
	}

	public void setHash(Book book, String hash) {
		if (myInterface == null) {
			return;
		}
			myInterface.setHash(SerializerUtil.serialize(book), hash);
	}

	public synchronized ZLTextFixedPosition.WithTimestamp getStoredPosition(long bookId) {
		if (myInterface == null) {
			return null;
		}

			final PositionWithTimestamp pos = myInterface.getStoredPosition(bookId);
			if (pos == null) {
				return null;
			}

			return new ZLTextFixedPosition.WithTimestamp(
				pos.ParagraphIndex, pos.ElementIndex, pos.CharIndex, pos.Timestamp
			);
	}

	public synchronized void storePosition(long bookId, ZLTextPosition position) {
		if (position != null && myInterface != null) {
				myInterface.storePosition(bookId, new PositionWithTimestamp(position));
		}
	}

	public synchronized boolean isHyperlinkVisited(Book book, String linkId) {
		if (myInterface == null) {
			return false;
		}

			return myInterface.isHyperlinkVisited(SerializerUtil.serialize(book), linkId);
	}

	public synchronized void markHyperlinkAsVisited(Book book, String linkId) {
		if (myInterface != null) {
				myInterface.markHyperlinkAsVisited(SerializerUtil.serialize(book), linkId);
		}
	}

	@Override
	public String getCoverUrl(Book book) {
		if (myInterface == null) {
			return null;
		}
			return myInterface.getCoverUrl(book.getPath());
	}

	@Override
	public String getDescription(Book book) {
		if (myInterface == null) {
			return null;
		}
			return myInterface.getDescription(SerializerUtil.serialize(book));
	}

	@Override
	public List<Bookmark> bookmarks(final BookmarkQuery query) {
		return listCall(new ListCallable<Bookmark>() {
			public List<Bookmark> call() throws RemoteException {
				return SerializerUtil.deserializeBookmarkList(
					myInterface.bookmarks(SerializerUtil.serialize(query))
				);
			}
		});
	}

	public synchronized void saveBookmark(Bookmark bookmark) {
		if (myInterface != null) {
				bookmark.update(SerializerUtil.deserializeBookmark(
					myInterface.saveBookmark(SerializerUtil.serialize(bookmark))
				));
		}
	}

	public synchronized void deleteBookmark(Bookmark bookmark) {
		if (myInterface != null) {
				myInterface.deleteBookmark(SerializerUtil.serialize(bookmark));
		}
	}

	public synchronized List<String> deletedBookmarkUids() {
		return listCall(new ListCallable<String>() {
			public List<String> call() throws RemoteException {
				return myInterface.deletedBookmarkUids();
			}
		});
	}

	public void purgeBookmarks(List<String> uids) {
		if (myInterface != null) {
				myInterface.purgeBookmarks(uids);
		}
	}

	public synchronized HighlightingStyle getHighlightingStyle(int styleId) {
		if (myInterface == null) {
			return null;
		}
			return SerializerUtil.deserializeStyle(myInterface.getHighlightingStyle(styleId));
	}

	public List<HighlightingStyle> highlightingStyles() {
		return listCall(new ListCallable<HighlightingStyle>() {
			public List<HighlightingStyle> call() throws RemoteException {
				return SerializerUtil.deserializeStyleList(myInterface.highlightingStyles());
			}
		});
	}

	public synchronized void saveHighlightingStyle(HighlightingStyle style) {
		if (myInterface != null) {
				myInterface.saveHighlightingStyle(SerializerUtil.serialize(style));
		}
	}

	public int getDefaultHighlightingStyleId() {
		if (myInterface == null) {
			return 1;
		}
			return myInterface.getDefaultHighlightingStyleId();
	}

	public void setDefaultHighlightingStyleId(int styleId) {
		if (myInterface != null) {
				myInterface.setDefaultHighlightingStyleId(styleId);
		}
	}

	public synchronized void rescan(String path) {
		if (myInterface != null) {
				myInterface.rescan(path);
		}
	}

	public List<FormatDescriptor> formats() {
		return listCall(new ListCallable<FormatDescriptor>() {
			public List<FormatDescriptor> call() throws RemoteException {
				final List<String> serialized = myInterface.formats();
				final List<FormatDescriptor> formats =
					new ArrayList<FormatDescriptor>(serialized.size());
				for (String s : serialized) {
					formats.add(Util.stringToFormatDescriptor(s));
				}
				return formats;
			}
		});
	}

	public synchronized boolean setActiveFormats(List<String> formats) {
		if (myInterface != null) {
				return myInterface.setActiveFormats(formats);
		}
		return false;
	}

	private interface ListCallable<T> {
		List<T> call() throws RemoteException;
	}

	private synchronized <T> List<T> listCall(ListCallable<T> callable) {
		if (myInterface == null) {
			return Collections.emptyList();
		}
		try {
			return callable.call();
		} catch (Exception e) {
			return Collections.emptyList();
		} catch (Throwable e) {
			// TODO: report problem
			return Collections.emptyList();
		}
	}

	public Book createBook(long id, String url, String title, String encoding, String language) {
		return new Book(id, url.substring("file://".length()), title, encoding, language);
	}
}
