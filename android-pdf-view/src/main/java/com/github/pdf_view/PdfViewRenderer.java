package com.github.pdf_view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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

    private final static float PAGE_PART_TO_SCREEN_RATIO = 4F;

    private int pagePartWidth;
    private int pagePartHeight;

    private static final int RENDERED_THUMBNAIL_MARGIN = 2;

    private PdfViewRenderManager pdfRenderManager;

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
                    return pdfDocument = pdfiumCore.newDocument(getSeekableFileDescriptor(
                            configuration.getUri().toString()), configuration.getPassword());
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
                preparePages();
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
        matrix.postScale(deltaScale, deltaScale, focusX, focusY);
        this.scale = deltaScale * this.scale;
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
        scrollBy(scrollX - this.scrollX, scrollY  - this.scrollY);
    }

    public void scaleTo(int focusX, int focusY, float scale) {
        scaleBy(focusX, focusY, scale/this.scale);
    }

    public void recycle() {

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
        canvas.drawColor(Color.LTGRAY);
        List<Page> renderingPages = getRenderingPages();
        pdfRenderManager.draw(canvas, renderingPages);
    }

    public void scrollBy(int dx, int dy) {
        matrix.postTranslate(-dx, -dy);
        fixTranslate();
        updateThumbnails();
    }

    private void updateThumbnails() {
        int firstRenderedIndex = getFirstRenderedPageIndex();
        int start = Math.max(firstRenderedIndex - RENDERED_THUMBNAIL_MARGIN, 0);
        int end = Math.min(firstRenderedIndex + RENDERED_THUMBNAIL_MARGIN + 1, pages.size());
        pdfRenderManager.renderThumbnail(pages.subList(start, end));
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

        private int pageOffsetTop;
        private int width;
        private int pageOffsetLeft;
        private int height;
        private int index;
//        List<PagePart> parts = new ArrayList<>();
//        private int rowSize;
//        private int partsWidth;

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

//        public void updateParts() {
//            List<PagePart> result = new ArrayList<>();
//            int partBottom = pagePartHeight;
//            int partTop = 0;
//            int rowSize = 0;
//            while (partTop < getHeight()) {
//                int partLeft = 0;
//                int partRight = pagePartWidth;
//                rowSize = 0;
//                while (partLeft < getWidth()){
//                    Rect partBounds = new Rect(partLeft, partTop,
//                            partRight, partBottom);
//                    PagePart pagePart = new PagePart(partBounds, index, getWidth(),
//                            getHeight(), scale);
//                    partLeft = partBounds.right ;
//                    partRight = Math.min(getWidth(), partLeft + pagePartWidth);
//                    result.add(pagePart);
//                    rowSize ++;
//                }
//                partTop = partBottom;
//                partBottom = Math.min(getHeight(), partTop + pagePartHeight);
//            }
//            this.rowSize = rowSize;
//            this.partsWidth = result.get(rowSize - 1).getRight();
//            parts = result;
//        }

//        public boolean shouldQualityBeUpdated() {
//                return partsWidth != getWidth();
//        }

        public List<PagePart> getActualPageParts() {
            int visiblePageTop = Math.max(scrollY - getTop(), 0);
            int visiblePageLeft = scrollX - getPageOffsetLeft();
            int visiblePageRight = surfaceWidth + visiblePageLeft;
            int visiblePageBottom = visiblePageTop +
                    (visiblePageTop == 0 ? Math.min(getHeight(), surfaceHeight + scrollY - getTop())
                    : Math.min(getHeight() - visiblePageTop, surfaceHeight + scrollX - getTop()));
            List<PagePart> pageParts = new ArrayList<>();
            if (getWidth() < thumbnail.getBounds().width()) {
                return pageParts;
            }
            int left = (visiblePageLeft / pagePartWidth) * pagePartWidth;
            int top = (visiblePageTop / pagePartHeight) * pagePartHeight;

            while (top < visiblePageBottom) {
                   int tempLeft = left;
                   while (tempLeft < visiblePageRight) {
                       Rect partBounds = new Rect(tempLeft, top,
                               tempLeft + pagePartWidth, top + pagePartHeight);
                       PagePart pagePart = new PagePart(partBounds, index, getWidth(),
                               getHeight(), scale);
                       pageParts.add(pagePart);
                       tempLeft += pagePartWidth;
                   }
                top += pagePartHeight;
            }
            return pageParts;
        }

        public PagePart getThumbnail() {
            return thumbnail;
        }

        private void createThumbnail() {
            thumbnail = new PagePart(
                    new Rect(0, 0, getWidth(),  getHeight()), index,
                    getWidth(), getHeight(), getScale()
            );
        }

        public int getPageOffsetLeft() {
            return (int) (pageOffsetLeft * scale);
        }
    }
}

