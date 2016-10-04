package com.github.pdf_view;


import android.graphics.Canvas;
import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.util.Log;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Sep 20, 2016
 */
public class PdfViewRenderManager {
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    final int cacheSize = maxMemory / 8;

    private BitmapCache cache = new BitmapCache(cacheSize);

    private ThreadPoolExecutor thumbnailExecutor = new ThreadPoolExecutor(1, 2, 1, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    private ThreadPoolExecutor contentExecutor = new ThreadPoolExecutor(1, 2, 1, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());


    private static final String TAG = PdfViewRenderManager.class.getSimpleName();
    private final PdfiumCore pdfium;
    private final PdfDocument document;
    private final PdfViewRenderer renderer;

    private List<PagePart> renderedThumbnails = new ArrayList<>();
    private RenderThumbnailsTask renderThumbnailsTask;

    private List<PagePart> renderedContentParts = new ArrayList<>();
    private RenderContentTask renderContentTask;

    PdfViewRenderManager(PdfDocument document, PdfiumCore pdfium, PdfViewRenderer renderer) {
        this.pdfium = pdfium;
        this.renderer = renderer;
        this.document = document;
    }


    public void renderThumbnail(List<PdfViewRenderer.Page> thumbPages) {
        boolean allRendered = true;
        for (PdfViewRenderer.Page tPage : thumbPages) {
            if(!renderedThumbnails.contains(tPage.getThumbnail())) {
                allRendered = false;
                break;
            }
        }
        if(allRendered) {
            return;
        }
        List<PagePart> toRenderParts = new ArrayList<>();
        for (PdfViewRenderer.Page page : thumbPages) {
            toRenderParts.add(page.getThumbnail());
        }

        RenderThumbnailsTask newRenderTask = new RenderThumbnailsTask();

        logRenderedContent("Should be rendered thumbnails : ", toRenderParts);

        if(renderThumbnailsTask != null) {
            cancelRendering(renderedThumbnails, toRenderParts, renderThumbnailsTask, newRenderTask,
                    thumbnailExecutor, "Rendered thumbnails : ");
        } else {
            recycleUnusedParts(renderedThumbnails, toRenderParts);
            logRenderedContent("Rendered thumbnails : ", renderedThumbnails);
            newRenderTask.executeOnExecutor(thumbnailExecutor, toRenderParts);
        }
    }

    private void recycleUnusedParts(List<PagePart> rendered, List<PagePart> toRenderParts) {
        List<PagePart> toRecycle = new ArrayList<>();
        for (PagePart pPart : rendered) {
            if(toRenderParts.contains(pPart)) {
                continue;
            }
            toRecycle.add(pPart);
        }

        if(rendered == renderedThumbnails && toRecycle.size() != 0) {
            logRenderedContent("Recycle thumbnails: ", toRecycle);
        }

        for (PagePart pPart : toRecycle) {
            pPart.recycle(cache);
            rendered.remove(pPart);
        }
    }

    private void cancelRendering(final List<PagePart> renderedParts, final List<PagePart> toRenderParts, RenderTask cancelRenderTask, final RenderTask newRenderTask, final ThreadPoolExecutor executor, final String message) {
        cancelRenderTask.setCancelListener(new Runnable() {
            @Override
            public void run() {
                recycleUnusedParts(renderedParts, toRenderParts);
                logRenderedContent(message, renderedParts);
                newRenderTask.executeOnExecutor(executor, toRenderParts);
            }
        });
        cancelRenderTask.cancel(false);
    }

    public void updateQuality(List<PdfViewRenderer.Page> renderingPages) {
        renderContent(renderingPages);
    }

    public void renderContent(List<PdfViewRenderer.Page> renderingPages) {
        List<PagePart> toRenderParts = new ArrayList<>();
        for (PdfViewRenderer.Page page: renderingPages) {
            page.prepareActualParts();
            toRenderParts.addAll(page.getParts());
        }
        boolean allRendered = true;
        for (PagePart pPart : toRenderParts) {
            if(!renderedContentParts.contains(pPart)) {
                allRendered = false;
                break;
            }
        }
        if(allRendered) {
            return;
        }
        RenderContentTask newRenderTask = new RenderContentTask();
        if(renderContentTask != null) {
            cancelRendering(renderedContentParts, toRenderParts, renderContentTask, newRenderTask,
                    contentExecutor, "Rendered content : ");
        } else {
            recycleUnusedParts(renderedContentParts, toRenderParts);
            logRenderedContent("Rendered content : ", renderedContentParts);
            newRenderTask.executeOnExecutor(contentExecutor, toRenderParts);
        }
    }

    public void draw(Canvas canvas, List<PdfViewRenderer.Page> pages) {
        for (PdfViewRenderer.Page p : pages) {
            p.getThumbnail().drawPart(canvas, p.getScale(), p.getRenderLeft(), p.getRenderTop(), true);
            for (PagePart pPart : p.getParts()) {
                pPart.drawPart(canvas, p.getScale(), p.getRenderLeft(), p.getRenderTop());
            }
        }
    }

    private void logRenderedContent(String message, List<PagePart> pageParts) {
        StringBuilder builder = new StringBuilder();
        builder.append(message);
        for (PagePart pPart : pageParts) {
            builder.append(pPart.index).append(":").append(pPart.getScaledBounds(1)).append(" ");
        }
        Log.d(TAG, builder.toString());
    }

    public void recycle() {
        if(renderContentTask != null) {
            renderContentTask.cancel(true);
        }
        if(renderThumbnailsTask != null) {
            renderThumbnailsTask.cancel(true);
        }

        for (PagePart thumbnail : renderedThumbnails) {
            thumbnail.recycle(cache);
        }

        for (PagePart content : renderedContentParts) {
            content.recycle(cache);
        }
        cache.evictAll();
    }

    public class RenderTask extends AsyncTask<List<PagePart>, PagePart, PagePart> {

        private Runnable onCancel;

        public void setCancelListener(Runnable onCancel) {
            this.onCancel = onCancel;
        }

        @Override
        protected PagePart doInBackground(List<PagePart>... params) {
            List<PagePart> parts = params[0];
            for (PagePart part : parts) {
                if (part.isRendered()) {
                    continue;
                }
                part.renderPart(document, pdfium, cache);
                if(isCancelled()) {
                    return part;
                } else {
                    publishProgress(part);
                }
            }
            return null;
        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
            if(onCancel != null) {
                onCancel.run();
            }
        }

        @Override
        @CallSuper
        protected void onPostExecute(PagePart pagePart) {
           renderer.notifyUpdate();
        }
    }


    public class RenderThumbnailsTask extends RenderTask {

        @Override
        protected void onPreExecute() {
            renderThumbnailsTask = this;
        }

        @Override
        protected void onCancelled(PagePart pagePart) {
            if(pagePart != null) {
                renderedThumbnails.add(pagePart);
                renderer.notifyUpdate();
            }
            logRenderedContent("Rendered thumbnails : ", renderedThumbnails);
            onCancelled();
        }

        @Override
        protected PagePart doInBackground(List<PagePart>... params) {
            return super.doInBackground(params);
        }

        @Override
        protected void onProgressUpdate(PagePart... values) {
            for (PagePart value : values) {
                renderedThumbnails.add(value);
            }
            renderer.notifyUpdate();
            logRenderedContent("Rendered thumbnails Progress : ", renderedThumbnails);
        }

        @Override
        protected void onPostExecute(PagePart aVoid) {
            super.onPostExecute(aVoid);
            renderer.notifyUpdate();
            renderThumbnailsTask = null;
        }

        @Override
        protected void onCancelled() {
            renderThumbnailsTask = null;
            super.onCancelled();
        }
    }

    public class RenderContentTask extends RenderTask {

        @Override
        protected void onPreExecute() {
            renderContentTask = this;
        }

        @Override
        protected void onCancelled(PagePart pagePart) {
            if(pagePart != null) {
                renderedContentParts.add(pagePart);
                renderer.notifyUpdate();
            }
            logRenderedContent("Rendered content : ", renderedContentParts);
            onCancelled();
        }

        @Override
        protected void onCancelled() {
            renderContentTask = null;
            super.onCancelled();
        }

        @Override
        protected void onProgressUpdate(PagePart... values) {
            for (PagePart value : values) {
                renderedContentParts.add(value);
            }
            renderer.notifyUpdate();
            logRenderedContent("Rendered content : ", renderedContentParts);
        }

        @Override
        protected void onPostExecute(PagePart aVoid) {
            super.onPostExecute(aVoid);
            renderContentTask = null;
        }
    }
}


