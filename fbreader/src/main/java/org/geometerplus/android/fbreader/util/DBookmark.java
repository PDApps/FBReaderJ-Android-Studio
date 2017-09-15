package org.geometerplus.android.fbreader.util;

import org.geometerplus.fbreader.util.FixedTextSnippet;
import org.geometerplus.fbreader.util.TextSnippet;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Andrew Churilo on 15.09.2017.
 */

public class DBookmark implements Serializable {
    private UUID uuid;
    private FixedTextSnippet textSnippet;

    public DBookmark(TextSnippet textSnippet) {
        this.uuid = UUID.randomUUID();
        this.textSnippet = (FixedTextSnippet) textSnippet;
    }

    public TextSnippet getTextSnippet() {
        return textSnippet;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBookmark) {
            DBookmark dBookmark = (DBookmark) obj;
            return uuid.equals(dBookmark.getUuid());
        } else {
            return false;
        }
    }
}
