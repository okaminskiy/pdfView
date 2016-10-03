package com.github.pdf_view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.util.Objects;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Sep 19, 2016
 */
public class PagePart {

    private final int pageWidth;
    private final int pageHeight;
    private final BitmapPool bitmapPool;

    private Rect bounds;
    private float scale;
    private Bitmap renderedPagePart;
    int index;


    public PagePart (Rect bounds, int index, int pageWidth, int pageHeight, float currentScale, BitmapPool bitmapPool) {
        this.index = index;
        this.bounds = bounds;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.scale = currentScale;
        this.bitmapPool = bitmapPool;
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

    public void renderPart(PdfDocument pdfDocument, PdfiumCore core) {
        Bitmap bmp = createBitmap();
        core.renderPageBitmap(pdfDocument, bmp, index, -bounds.left, -bounds.top, pageWidth, pageHeight);
        renderedPagePart = bmp;
    }

    private Bitmap createBitmap() {
        Bitmap bmp = bitmapPool == null ? null : bitmapPool.peek();
        if(bmp == null) {
            bmp = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        }
        if(bmp.getWidth() != bounds.width() || bmp.getHeight() != bounds.height()) {
            bmp.recycle();
            bmp = createBitmap();
        }
        return bmp;
    }

    public void recycle() {
        Bitmap bmp = renderedPagePart;
        renderedPagePart = null;
        if(bmp != null) {
            recycleBitmap(bmp);
        }
        renderedPagePart = null;
    }

    private void recycleBitmap(Bitmap bmp) {
        if(bitmapPool != null) {
            bitmapPool.put(bmp);
        } else {
            bmp.recycle();
        }
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

    public Rect getBounds() {
        return bounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PagePart pagePart = (PagePart) o;
        return  pageWidth == pagePart.pageWidth &&
                pageHeight == pagePart.pageWidth &&
                index == pagePart.index &&
                bounds.equals(bounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageWidth, pageHeight, bounds, pageWidth, pageHeight, index);
    }
}


