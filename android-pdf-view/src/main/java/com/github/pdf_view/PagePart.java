package com.github.pdf_view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.LruCache;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Sep 19, 2016
 */
public class PagePart {

    private final int pageWidth;
    private final int pageHeight;

    private Rect bounds;
    private float scale;
    private Bitmap renderedPagePart;
    int index;


    public PagePart (Rect bounds, int index, int pageWidth, int pageHeight, float currentScale) {
        this.index = index;
        this.bounds = bounds;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.scale = currentScale;
    }

    public void drawPart(Canvas canvas, float scale, int pageOffsetLeft, int pageOffsetTop) {
        Rect targetRect = getScaledBounds(scale);
        targetRect.offset(pageOffsetLeft, pageOffsetTop);
        if(renderedPagePart != null) {
            canvas.drawBitmap(renderedPagePart, null, targetRect, null);
        }
    }

    public void renderPart(PdfDocument pdfDocument, PdfiumCore core) {
        Bitmap bmp = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        core.renderPageBitmap(pdfDocument, bmp, index, -bounds.left, -bounds.top, pageWidth, pageHeight);
        renderedPagePart = bmp;
    }

    public void recycle() {
        if(renderedPagePart != null) {
            renderedPagePart.recycle();
        }
        renderedPagePart = null;
    }

    public Rect getScaledBounds(float scale) {
        double deltaScale = (double) scale / this.scale;
        return new Rect((int) (bounds.left * deltaScale),
                (int) (bounds.top * deltaScale),
                (int) (bounds.right * deltaScale),
                (int) (bounds.bottom * deltaScale));
    }

    public boolean isRendered() {
        return renderedPagePart != null;
    }

    public int getRight() {
        return bounds.right;
    }
}


