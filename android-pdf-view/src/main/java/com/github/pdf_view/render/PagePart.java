package com.github.pdf_view.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

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
        drawPart(canvas, scale, pageOffsetLeft, pageOffsetTop, false);
    }

    public void drawPart(Canvas canvas, float scale, int pageOffsetLeft, int pageOffsetTop, boolean fillColor) {
        Rect targetRect = getScaledBounds(scale);
        targetRect.offset(pageOffsetLeft, pageOffsetTop);
        if(renderedPagePart != null) {
            canvas.drawBitmap(renderedPagePart, null, targetRect, null);
        } else if(fillColor) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            canvas.drawRect(targetRect, paint);
        }
    }

    public void renderPart(PdfDocument pdfDocument, PdfiumCore core, BitmapCache bitmapCache) {
        Bitmap bmp = bitmapCache.peek(new Point(bounds.width(), bounds.height()));
        if(bmp == null) {
            bmp = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        }
        core.renderPageBitmap(pdfDocument, bmp, index, -bounds.left, -bounds.top, pageWidth, pageHeight);
        renderedPagePart = bmp;
    }

    public void recycle(BitmapCache bitmapCache) {
        Bitmap bmp = renderedPagePart;
        renderedPagePart = null;
        if(bmp != null) {
            bitmapCache.put(new Point(bounds.width(), bounds.height()), bmp);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PagePart pagePart = (PagePart) o;
        return  pageWidth == pagePart.pageWidth &&
                pageHeight == pagePart.pageHeight &&
                index == pagePart.index &&
                bounds.equals(pagePart.bounds);
    }

    @Override
    public int hashCode() {
        int result = pageWidth;
        result = 31 * result + pageHeight;
        result = 31 * result + bounds.hashCode();
        result = 31 * result + (renderedPagePart != null ? renderedPagePart.hashCode() : 0);
        result = 31 * result + index;
        return result;
    }
}


