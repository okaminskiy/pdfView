package com.github.pdf_view.render;

import android.graphics.Rect;

/**
 * Created by Oleg on 10/10/2016.
 */

public class ThumbnailsPagePart extends PagePart {
    public ThumbnailsPagePart(Rect bounds, int index, int pageWidth, int pageHeight, float currentScale) {
        super(bounds, index, pageWidth, pageHeight, currentScale);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
