package com.github.pdf_view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.view.Display;
import android.view.WindowManager;

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
public class PdfViewRenderer {
    private final Context context;
    private final Handler backgroundHandler;
    private PdfViewConfiguration configuration;
    private final PdfRendererListener listener;
    private static final float MIN_SCALE = 0.2F;
    PdfDocument pdfDocument;
    PdfiumCore pdfiumCore;

    private Matrix matrix = new Matrix();

    private float scale = 1f;
    private int currentPage;

    private int surfaceWidth;
    private int surfaceHeight;

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
    private int pagePartHeight;

    private static final int RENDERED_THUMBNAIL_MARGIN = 5;

    private PdfViewRenderManager pdfRenderManager;
    private float currVelocity;

    private BitmapPool thumbnailsPool = new BitmapPool(30);


    public PdfViewRenderer(Context context, PdfRendererListener listener) {
        pdfiumCore = new PdfiumCore(context);
        this.context = context;
        this.listener = listener;
        paint = new Paint();
        handlerThread = new HandlerThread(getClass().getSimpleName());
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
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
                            listener.onError(e);
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
                currentPage = configuration.getFirstPage();
                pdfRenderManager = new PdfViewRenderManager(document, pdfiumCore, PdfViewRenderer.this);
                listener.onDocumentReady(pdfiumCore.getPageCount(pdfDocument), PdfViewRenderer.this);
            }
        }.execute();
    }


    public void onViewSizeChanged(int width, int height) {
        if(width == 0 || height == 0) {
            return;
        }
        surfaceWidth = width;
        surfaceHeight = height;
        int maxWidth = 0;
        for (Page page : pages) {
            if(page.width > maxWidth) {
                maxWidth = page.width;
            }
        }

        optimalScale = (float)  surfaceWidth / maxWidth;

        for (Page page : pages) {
            page.createThumbnail();
            if(page.width < maxWidth) {
                page.pageOffsetLeft = (int) ((maxWidth - page.width) * optimalScale / 2);
            }
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
            newPage.height = pdfiumCore.getPageHeight(pdfDocument, i);
            newPage.width = pdfiumCore.getPageWidth(pdfDocument, i);
            newPage.index = i;
            if(page != null) {
                newPage.pageOffsetTop = page.pageOffsetTop + page.height;
            }
            page = newPage;
            pages.add(page);
        }
    }

    public int getHorizontalScrollExtent() {
        return surfaceWidth;
    }

    public int getVerticalScrollExtent() {
        return surfaceHeight;
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

    private List<Page> getRenderingPages() {
        ArrayList<Page> result = new ArrayList<>();
        for(int i = getFirstRenderedPageIndex(); i < pages.size(); i++ ) {
            Page page = pages.get(i);
            if(page.getTop() > surfaceHeight + scrollY) {
                break;
            }
            result.add(page);
        }
        return result;
    }

    public int getFirstRenderedPageIndex() {
        for (Page page : pages) {
            if(page.getBottom() > scrollY) {
                return page.index;
            }
        }
        return -1;
    }

    public void scaleBy(float focusX, float focusY, float deltaScale) {
        float newScale =  deltaScale * this.scale;
        if (newScale < MIN_SCALE) {
            newScale = MIN_SCALE;
            deltaScale = newScale/scale;
        }
        matrix.postScale(deltaScale, deltaScale, focusX, focusY);
        this.scale = newScale;
        fixTranslate();
    }

    public int getMaxScrollY() {
        return Math.max(getVerticalScrollRange() - surfaceHeight, 0);
    }

    public int getMaxScrollX() {
        return Math.max(getHorizontalScrollRange() - surfaceWidth, 0);
    }


    public int getVerticalScrollRange() {
        Page lastPage = pages.get(pages.size() - 1);
        return Math.max(0,
                lastPage.getBottom());
    }


    private int getLastPageBottom() {
        Page lastPage = pages.get(pages.size() - 1);
        return lastPage.getBottom();
    }

    public int getHorizontalScrollRange() {
        return (int) Math.max(surfaceWidth * scale, 0);
}

    public void scrollTo(int scrollX, int scrollY) {
        scrollBy(scrollX - this.scrollX, scrollY  - this.scrollY, 0);
    }

    public void scaleTo(int focusX, int focusY, float scale) {
        scaleBy(focusX, focusY, scale/this.scale);
    }

    public void recycle() {
        pdfRenderManager.recycle();
        thumbnailsPool.recycle();
    }

    public void updateQuality() {
        pdfRenderManager.updateQuality(getRenderingPages());
    }

    public void notifyUpdate() {
        listener.onPageUpdated();
    }

    public interface PdfRendererListener {
         void onDocumentReady(int pageCount, PdfViewRenderer renderer);
         void onError(IOException e);
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

    void draw(Canvas canvas) {
        if(surfaceHeight == 0 || surfaceWidth == 0) {
            return;
        }
        List<Page> renderingPages = getRenderingPages();
        pdfRenderManager.draw(canvas, renderingPages);
    }

    private void scrollBy(int dx, int dy, float currVelocity) {
        matrix.postTranslate(-dx, -dy);
        fixTranslate();
        if(currVelocity < 1000) {
            updateThumbnails();
        }
    }

    private void updateThumbnails() {
        int firstRenderedIndex = getFirstRenderedPageIndex();
        int lastRenderedPage = getLastRenderedPage(firstRenderedIndex);
        int start = Math.max(firstRenderedIndex - RENDERED_THUMBNAIL_MARGIN, 0);
        int end = Math.min(lastRenderedPage + RENDERED_THUMBNAIL_MARGIN + 1, pages.size());
        pdfRenderManager.renderThumbnail(pages.subList(start, end));
    }

    private int getLastRenderedPage(int firstRenderedPage) {
        for(int i = firstRenderedPage; i < pages.size(); i++) {
            if(pages.get(i).getTop() > scrollY + surfaceWidth) {
                return i - 1;
            }
        }
        return pages.size() - 1;
    }

    public void fixTranslate() {
        float[] values = new float[9];
        matrix.getValues(values);
        if (surfaceWidth * scale < surfaceWidth) {
            scrollX = 0;
            offsetLeft = (int) ((surfaceWidth - surfaceWidth * scale) / 2);
            values[Matrix.MTRANS_X] = offsetLeft;
        } else {
            offsetLeft = 0;
            scrollX = getAllowedScroll(0, getMaxScrollX(), (int) -values[Matrix.MTRANS_X]);
            values[Matrix.MTRANS_X] = -scrollX;
        }

        int lastPageBottom = getLastPageBottom();
        if (lastPageBottom < surfaceHeight) {
            scrollY = 0;
            offsetTop = (int) ((float)(surfaceHeight - lastPageBottom) / 2);
            values[Matrix.MTRANS_Y] = offsetLeft;
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

    public class Page {

        private PagePart thumbnail;

        private float lastUpdatedScale;
        private int pageOffsetTop;
        private int width;
        private int pageOffsetLeft;
        private int height;
        private int index;
        List<PagePart> parts = new ArrayList<>();

        private int getBottom() {
            return getTop() + getHeight();
        }

        private int getHeight() {
            return (int) (height * scale * optimalScale);
        }

        private int getWidth() {
            return (int) (width * scale * optimalScale);
        }

        private int getTop() {
            return (int) ((pageOffsetTop * optimalScale + configuration.getPageSpacing() * index) * scale);
        }

        public float getScale() {
            return scale;
        }

        public int getRenderTop() {
            return offsetTop + getTop() - scrollY;
        }

        public int getRenderLeft() {
            return (int) (pageOffsetLeft * scale + offsetLeft - scrollX);
        }

        public  List<PagePart> getParts() {
            return parts;
        }

        public void prepareActualParts() {
            int visiblePageTop = Math.max(scrollY - getTop(), 0);
            int visiblePageLeft = Math.max(scrollX - getPageOffsetLeft(), 0);
            int visiblePageRight = Math.min(surfaceWidth + visiblePageLeft, getWidth());
            int visiblePageBottom = visiblePageTop + Math.min(surfaceHeight - Math.max(0, getTop() - scrollY), getHeight() - visiblePageTop);
            removeUnusedParts(visiblePageLeft, visiblePageTop, visiblePageRight, visiblePageBottom);
            if (getWidth() < thumbnail.getBounds().width()) {
                return;
            }
            int left = (visiblePageLeft / pagePartWidth) * pagePartWidth;
            int top = (visiblePageTop / pagePartHeight) * pagePartHeight;

            while (top < visiblePageBottom) {
                   int tempLeft = left;
                   while (tempLeft < visiblePageRight) {
                       Rect partBounds = new Rect(tempLeft, top,
                               Math.min(getWidth(), tempLeft + pagePartWidth),
                               Math.min(top + pagePartHeight, getHeight()));
                       PagePart pagePart = new PagePart(partBounds, index, getWidth(),
                               getHeight(), scale, null);
                       if(!parts.contains(pagePart)) {
                           parts.add(pagePart);
                       }
                       tempLeft += pagePartWidth;
                   }
                top += pagePartHeight;
            }
        }

        private boolean isOverlaps(Rect rect1, Rect rect2) {
            boolean result =  rect1.left < rect2.left + rect2.width()
                    && rect1.left + rect1.width() > rect2.left
                    && rect1.top < rect2.top + rect2.height()
                    && rect1.top + rect1.height() > rect2.top;
            return result;
        }

        private void removeUnusedParts(int left, int top, int right, int bottom) {
            Rect visibleBounds = new Rect(left, top, right, bottom);
            if(lastUpdatedScale != scale) {
                parts.clear();
                lastUpdatedScale = scale;
                return;
            }
            List<PagePart> actualLeft = new ArrayList<>();
            for (PagePart pPart: parts) {
                if(isOverlaps(pPart.getBounds(), visibleBounds)){
                    actualLeft.add(pPart);
                }
            }
            parts = actualLeft;
        }

        public PagePart getThumbnail() {
            return thumbnail;
        }

        private void createThumbnail() {
            thumbnail = new PagePart(
                    new Rect(0, 0, getWidth(),  getHeight()), index,
                    getWidth(), getHeight(), getScale(), thumbnailsPool
            );
        }

        public int getPageOffsetLeft() {
            return (int) (pageOffsetLeft * scale);
        }
    }
}

