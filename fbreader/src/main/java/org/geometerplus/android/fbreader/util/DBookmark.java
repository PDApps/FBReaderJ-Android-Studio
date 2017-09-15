package org.geometerplus.android.fbreader.util;

import org.geometerplus.fbreader.util.FixedTextSnippet;
import org.geometerplus.fbreader.util.TextSnippet;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;

import java.util.UUID;

/**
 * Created by Andrew Churilo on 15.09.2017.
 */

public class DBookmark {
    private UUID uuid;
    private TextSnippet textSnippet;

    public DBookmark(TextSnippet textSnippet){
        this.uuid = UUID.randomUUID();
        this.textSnippet = textSnippet;
    }

    public TextSnippet getTextSnippet() {
        return textSnippet;
    }

    public UUID getUuid() {
        return uuid;
    }
}
