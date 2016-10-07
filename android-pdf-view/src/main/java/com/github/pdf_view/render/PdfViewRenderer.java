package com.github.pdf_view.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.view.Display;
import android.view.WindowManager;

import com.github.pdf_view.PdfViewConfiguration;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Sep 14, 2016
 */
public class PdfViewRenderer implements RenderInfo {
    private int firstVisiblePage;
    private int lastVisiblePage;


    private final Context context;
    private PdfViewConfiguration configuration;
    private final PdfRendererListener listener;
    PdfDocument pdfDocument;
    PdfiumCore pdfiumCore;

    private Matrix matrix = new Matrix();

    private float scale = 1f;

    private int renderWidth;
    private int renderHeight;

    private List<Page> pages;

    private float optimalScale;
    private int scrollY;
    private int scrollX;

    private int offsetTop;
    private int offsetLeft;

    private Paint paint;
    private HandlerThread handlerThread;

    private final static float PAGE_PART_TO_SCREEN_RATIO = 3F;

    private int pagePartWidth;
    private int initialPage;
    private int pagePartHeight;

    private static final int RENDERED_THUMBNAIL_MARGIN = 5;

    private PdfViewRenderManager pdfRenderManager;
    private float maxAvailableScale;

    public PdfViewRenderer(Context context, PdfRendererListener listener) {
        pdfiumCore = new PdfiumCore(context);
        this.context = context;
        this.listener = listener;
        paint = new Paint();
        handlerThread = new HandlerThread(getClass().getSimpleName());
        handlerThread.start();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        if(Build.VERSION.SDK_INT >= 13) {
            display.getSize(size);
        } else {
            size.x = display.getWidth();
            size.y = display.getHeight();
        }
        pagePartWidth = (int) (size.x / PAGE_PART_TO_SCREEN_RATIO);
        pagePartHeight = (int) ((size.y) / PAGE_PART_TO_SCREEN_RATIO);
    }


    //TODO add assets
    public void loadDocument(final PdfViewConfiguration configuration) {
        this.configuration = configuration;
        new AsyncTask<Void, Void, PdfDocument>() {

            @Override
            protected PdfDocument doInBackground(Void... voids) {
                try {
                    pdfDocument = pdfiumCore.newDocument(getSeekableFileDescriptor(
                            configuration.getUri().toString()), configuration.getPassword());
                    preparePages();
                    return pdfDocument;
                } catch (final IOException e) {
                    new Handler(context.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            configuration.notifyError(e);
                        }
                    });
                }
                return null;
            }

            @Override
            protected void onPostExecute(PdfDocument document) {
                if(document == null) {
                    return;
                }
                pdfRenderManager = new PdfViewRenderManager(document, pdfiumCore, PdfViewRenderer.this);
                configuration.notifyPageLoaded(pdfiumCore.getPageCount(pdfDocument));
                listener.onDocumentReady(PdfViewRenderer.this, configuration);
            }
        }.execute();
    }


    public void onViewSizeChanged(int width, int height) {
        if(width == 0 || height == 0) {
            return;
        }
        renderWidth = width;
        renderHeight = height;
        int maxWidth = 0;
        for (Page page : pages) {
            if(page.getOriginalWidth() > maxWidth) {
                maxWidth = page.getOriginalWidth();
            }
        }

        optimalScale = (float) renderWidth / maxWidth;

        for (Page page : pages) {
            page.createThumbnail();
            if(page.width < maxWidth) {
                page.pageOffsetLeft = (int) ((maxWidth - page.width) * optimalScale / 2);
            }
        }

        if(initialPage != -1) {
            scrollToPage(initialPage);
            initialPage = -1;
        }

        updateThumbnails();
        updateQuality();
    }

    private void preparePages() {
        int pageCount = pdfiumCore.getPageCount(pdfDocument);
        pages = new ArrayList(pageCount);
        Page page = null;
        for(int i = 0; i < pageCount; i++) {
            Page newPage = new Page();
            pdfiumCore.openPage(pdfDocument, i);
            newPage.renderInfo = this;
            newPage.height = pdfiumCore.getPageHeight(pdfDocument, i);
            newPage.width = pdfiumCore.getPageWidth(pdfDocument, i);
            newPage.index = i;
            if(page != null) {
                newPage.pageOffsetTop = page.pageOffsetTop + page.height;
            }
            page = newPage;
            pages.add(page);
        }
        initialPage = configuration.getStartPage();
        maxAvailableScale = (float) Integer.MAX_VALUE / getLastPageBottom();
    }

    public int getHorizontalScrollExtent() {
        return renderWidth;
    }

    public int getVerticalScrollExtent() {
        return renderHeight;
    }

    public int getScrollY() {
        return scrollY;
    }

    public int getScrollX() {
        return scrollX;
    }

    public boolean canScrollVertically(int direction) {
        return direction > 0 ?  scrollY != getVerticalScrollRange()
                : scrollY != 0;
    }

    public boolean canScrollHorizontally(int direction) {
        return  direction > 0 ? false : scrollX != 0;
    }

    public float getScale() {
        return scale;
    }

    @Override
    public float getNormalizeScale() {
        return optimalScale;
    }

    @Override
    public int getPageSpacing() {
        return configuration.getPageSpacing();
    }

    @Override
    public int getRenderOffsetLeft() {
        return offsetLeft;
    }

    @Override
    public int getRenderOffsetTop() {
        return offsetTop;
    }

    @Override
    public int getPartWidth() {
        return pagePartWidth;
    }

    @Override
    public int getPartHeight() {
        return pagePartHeight;
    }

    @Override
    public int getRenderWidth() {
        return renderWidth;
    }

    @Override
    public int getRenderHeight() {
        return renderHeight;
    }

    private List<Page> getRenderingPages() {
        int firstRenderedPage = getFirstRenderedPageIndex(firstVisiblePage);
        int lastRenderedPage = getLastRenderedPage(firstRenderedPage);
        return pages.subList(firstRenderedPage, lastRenderedPage + 1);
    }

    private int getFirstRenderedPageIndex(int firstVisiblePage) {
        if(pages.get(firstVisiblePage).getTop() > scrollY) {
            return searchFirstPageNegative(firstVisiblePage);
        }
        if(pages.get(firstVisiblePage).getBottom() <= scrollY) {
            return searchFirstPagePositive(++firstVisiblePage);
        }
        return firstVisiblePage;
    }

    private int searchFirstPagePositive(int firstVisiblePage) {
            if(pages.get(firstVisiblePage).getBottom() >= scrollY) {
                return firstVisiblePage ;
            }
            return firstVisiblePage == getLastPageIndex() ? firstVisiblePage
                    : searchFirstPagePositive(++firstVisiblePage);
    }

    private int searchFirstPageNegative(int firstVisiblePage) {
        if(pages.get(firstVisiblePage).getBottom() <= scrollY) {
            return firstVisiblePage + 1;
        }
        return firstVisiblePage == 0 ?  0 : searchFirstPageNegative(--firstVisiblePage);
    }

    public void scaleBy(float focusX, float focusY, float deltaScale) {
        deltaScale = fixScale(deltaScale);
        matrix.postScale(deltaScale, deltaScale, focusX, focusY);
        this.scale = scale * deltaScale;
        fixTranslate();
    }

    private float fixScale(float deltaScale) {
        float newScale =  deltaScale * this.scale;
        float maxZoom = Math.min(maxAvailableScale, configuration.getMaxScale());
        if(newScale > maxZoom) {
            return maxZoom / scale;
        }
        if (newScale < configuration.getMinScale()) {
            return configuration.getMinScale() / scale;
        }
        return deltaScale;
    }

    public int getMaxScrollY() {
        return Math.max(getVerticalScrollRange() - renderHeight, 0);
    }

    public int getMaxScrollX() {
        return Math.max(getHorizontalScrollRange() - renderWidth, 0);
    }


    public int getVerticalScrollRange() {
        Page lastPage = pages.get(getLastPageIndex());
        return Math.max(0,
                lastPage.getBottom());
    }


    private int getLastPageBottom() {
        Page lastPage = pages.get(getLastPageIndex());
        return lastPage.getBottom();
    }

    public int getHorizontalScrollRange() {
        return (int) Math.max(renderWidth * scale, 0);
}

    public void scrollTo(int scrollX, int scrollY) {
        scrollBy(scrollX - this.scrollX, scrollY  - this.scrollY);
    }

    public void scrollToPage(int pageIndex) {
        scrollTo(0, pages.get(pageIndex).getTop());
    }

    public void scaleTo(int focusX, int focusY, float scale) {
        scaleBy(focusX, focusY, scale/this.scale);
    }

    public void recycle() {
        pdfRenderManager.recycle();
    }

    public void updateQuality() {
        pdfRenderManager.updateQuality(getRenderingPages());
    }

    public void notifyUpdate() {
        listener.onPageUpdated();
    }

    public void restoreState(PdfRendererState pdfState) {
        if(!pdfState.getUri().equals(configuration.getUri())
                || pages.size() <= pdfState.getFirstRenderedPage()) {
            return;
        }
        scaleTo(0, 0, pdfState.getScale());
        scrollToPage(pdfState.getFirstRenderedPage());
        scrollBy((int) (pdfState.getOriginalPageScrollX() * optimalScale * scale), (int) (pdfState.getOriginalPageScrollY() * optimalScale * scale));
        updateThumbnails();
        updateQuality();
    }

    public Parcelable getCurrentState() {
        int firstRenderedPage = getFirstRenderedPageIndex(firstVisiblePage);
        int originalPageScrollY =
                (int) (Math.max(scrollY - pages.get(firstRenderedPage).getTop(), 0f) / optimalScale / scale);
        int originalPageScrollX = (int) (scrollX / (optimalScale * scale));
        return new PdfRendererState(configuration.getUri(), scale, firstRenderedPage, originalPageScrollX, originalPageScrollY);
    }

    public interface PdfRendererListener {
         void onDocumentReady(PdfViewRenderer renderer, PdfViewConfiguration configuration);
         void onPageUpdated();
    }

    protected ParcelFileDescriptor getSeekableFileDescriptor(String path) throws IOException {
        ParcelFileDescriptor pfd;

        File pdfCopy = new File(path);
        if (pdfCopy.exists()) {
            pfd = ParcelFileDescriptor.open(pdfCopy, ParcelFileDescriptor.MODE_READ_ONLY);
            return pfd;
        }

        if (!path.contains("://")) {
            path = String.format("file://%s", path);
        }

        Uri uri = Uri.parse(path);
        pfd = context.getContentResolver().openFileDescriptor(uri, "r");

        if (pfd == null) {
            throw new IOException("Cannot get FileDescriptor for " + path);
        }
        return pfd;
    }

    public void draw(Canvas canvas) {
        if(renderHeight == 0 || renderWidth == 0) {
            return;
        }
        List<Page> renderingPages = getRenderingPages();
        pdfRenderManager.draw(canvas, renderingPages);
    }

    private void scrollBy(int dx, int dy) {
        matrix.postTranslate(-dx, -dy);
        fixTranslate();
        updateThumbnails();
    }

    private void updateThumbnails() {
        int firstRenderedIndex = getFirstRenderedPageIndex(firstVisiblePage);
        int lastRenderedIndex = getLastRenderedPage(firstRenderedIndex);
        int start = Math.max(firstRenderedIndex - RENDERED_THUMBNAIL_MARGIN, 0);
        int end = Math.min(lastRenderedIndex + RENDERED_THUMBNAIL_MARGIN + 1, pages.size());
        if(firstRenderedIndex != firstVisiblePage || lastRenderedIndex != lastVisiblePage) {
            configuration.notifyPageChanged(firstRenderedIndex, lastRenderedIndex);
            firstVisiblePage = firstRenderedIndex;
            lastVisiblePage = lastRenderedIndex;
        }
        pdfRenderManager.renderThumbnail(pages.subList(start, end));
    }

    private int getLastRenderedPage(int firstRenderedPage) {
        for(int i = firstRenderedPage; i < pages.size(); i++) {
            if(pages.get(i).getTop() > scrollY + renderHeight) {
                return i - 1;
            }
        }
        return getLastPageIndex();
    }

    public void fixTranslate() {
        float[] values = new float[9];
        matrix.getValues(values);
        if (renderWidth * scale < renderWidth) {
            scrollX = 0;
            offsetLeft = (int) ((renderWidth - renderWidth * scale) / 2);
            values[Matrix.MTRANS_X] = offsetLeft;
        } else {
            offsetLeft = 0;
            scrollX = getAllowedScroll(0, getMaxScrollX(), (int) -values[Matrix.MTRANS_X]);
            values[Matrix.MTRANS_X] = -scrollX;
        }

        int lastPageBottom = getLastPageBottom();
        if (lastPageBottom < renderHeight) {
            scrollY = 0;
            offsetTop = (int) ((float)(renderHeight - lastPageBottom) / 2);
            values[Matrix.MTRANS_Y] = offsetTop;
        } else {
            offsetTop = 0;
            scrollY = getAllowedScroll(0, getMaxScrollY()
                    , (int) -values[Matrix.MTRANS_Y]);
            values[Matrix.MTRANS_Y] = -scrollY;
        }
        matrix.setValues(values);
    }

    public int getAllowedScroll(int min, int max, int requested) {
        return Math.max(Math.min(max, requested), min);
    }

    public int getLastPageIndex() {
        return pages.size() == 0 ? 0 : pages.size() - 1;
    }

}

