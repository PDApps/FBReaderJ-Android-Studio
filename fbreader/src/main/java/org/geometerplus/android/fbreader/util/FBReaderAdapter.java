package org.geometerplus.android.fbreader.util;

import com.github.johnpersano.supertoasts.library.SuperActivityToast;

import org.geometerplus.android.fbreader.httpd.DataService;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.view.ZLTextRegion;

/**
 * Created by Andrew Churilo on 17.08.2017.
 */

public interface FBReaderAdapter {
    void onPluginNotFound(final Book book);
    boolean isPaused();
    void setOnResumeAction(Runnable onResumeAction);
    void showToast(final SuperActivityToast toast);
    ZLViewWidget getViewWidget();
    void outlineRegion(ZLTextRegion.Soul soul);
    DataService.Connection getDataConnection();
    boolean isToastShown();
    void hideToast();
}
