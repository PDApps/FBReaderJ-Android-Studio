/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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
import android.widget.Toast;

import com.github.johnpersano.supertoasts.library.Style;
import com.github.johnpersano.supertoasts.library.SuperActivityToast;
import com.github.johnpersano.supertoasts.library.SuperToast;

import org.geometerplus.android.fbreader.image.ImageViewActivity;
import org.geometerplus.android.fbreader.util.FBReaderAdapter;
import org.geometerplus.android.util.OrientationUtil;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.bookmodel.FBHyperlinkType;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.util.AutoTextSnippet;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlink;
import org.geometerplus.zlibrary.text.view.ZLTextHyperlinkRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextImageRegionSoul;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;

public class ProcessHyperlinkAction extends FBAndroidAction {
	private FBReaderAdapter fbReaderAdapter;
	public ProcessHyperlinkAction(Activity baseActivity, FBReaderApp fbreader, FBReaderAdapter readerAdapter) {
		super(baseActivity, fbreader);
		this.fbReaderAdapter = readerAdapter;
	}

	@Override
	public boolean isEnabled() {
		return Reader.getTextView().getOutlinedRegion() != null;
	}

	@Override
	protected void run(Object ... params) {
		final ZLTextRegion region = Reader.getTextView().getOutlinedRegion();
		if (region == null) {
			return;
		}

		final ZLTextRegion.Soul soul = region.getSoul();
		if (soul instanceof ZLTextHyperlinkRegionSoul) {
			Reader.getTextView().hideOutline();
			Reader.getViewWidget().repaint();
			final ZLTextHyperlink hyperlink = ((ZLTextHyperlinkRegionSoul)soul).Hyperlink;
			switch (hyperlink.Type) {
				case FBHyperlinkType.EXTERNAL:
					openInBrowser(hyperlink.Id);
					break;
				case FBHyperlinkType.INTERNAL:
				case FBHyperlinkType.FOOTNOTE:
				{
					final AutoTextSnippet snippet = Reader.getFootnoteData(hyperlink.Id);
					if (snippet == null) {
						break;
					}

					Reader.Collection.markHyperlinkAsVisited(Reader.getCurrentBook(), hyperlink.Id);
					final boolean showToast;
					switch (Reader.MiscOptions.ShowFootnoteToast.getValue()) {
						default:
						case never:
							showToast = false;
							break;
						case footnotesOnly:
							showToast = hyperlink.Type == FBHyperlinkType.FOOTNOTE;
							break;
						case footnotesAndSuperscripts:
							showToast =
								hyperlink.Type == FBHyperlinkType.FOOTNOTE ||
								region.isVerticallyAligned();
							break;
						case allInternalLinks:
							showToast = true;
							break;
					}
					if (showToast) {
						final SuperActivityToast toast;
						if (snippet.IsEndOfText) {
							toast = new SuperActivityToast(BaseActivity, Style.TYPE_STANDARD);
						} else {
							toast = new SuperActivityToast(BaseActivity, Style.TYPE_BUTTON);
							toast.setButtonIconResource(android.R.drawable.ic_menu_more);
							toast.setButtonText(ZLResource.resource("toast").getResource("more").getValue());
							toast.setOnButtonClickListener("ftnt", null, new SuperActivityToast.OnButtonClickListener() {
								@Override
								public void onClick(View view, Parcelable token) {
									Reader.getTextView().hideOutline();
									Reader.tryOpenFootnote(hyperlink.Id);
								}
							});
						}
						toast.setText(snippet.getText());
						toast.setDuration(Reader.MiscOptions.FootnoteToastDuration.getValue().Value);
						toast.setOnDismissListener("ftnt", new SuperToast.OnDismissListener() {
							@Override
							public void onDismiss(View view, Parcelable token) {
								Reader.getTextView().hideOutline();
								Reader.getViewWidget().repaint();
							}
						});
						Reader.getTextView().outlineRegion(region);
						fbReaderAdapter.showToast(toast);
					} else {
						Reader.tryOpenFootnote(hyperlink.Id);
					}
					break;
				}
			}
		} else if (soul instanceof ZLTextImageRegionSoul) {
			Reader.getTextView().hideOutline();
			Reader.getViewWidget().repaint();
			final String url = ((ZLTextImageRegionSoul)soul).ImageElement.URL;
			if (url != null) {
				try {
					final Intent intent = new Intent();
					intent.setClass(BaseActivity, ImageViewActivity.class);
					intent.putExtra(ImageViewActivity.URL_KEY, url);
					intent.putExtra(
						ImageViewActivity.BACKGROUND_COLOR_KEY,
						Reader.ImageOptions.ImageViewBackground.getValue().intValue()
					);
					OrientationUtil.startActivity(BaseActivity, intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void openInBrowser(final String url) {
		// TODO: make method abstract. Add realisation to open Dicti WebView
		Toast.makeText(BaseActivity, "For developers: Add realisation to open Dicti WebView", Toast.LENGTH_SHORT).show();

	}
}
