package com.github.pdf_view;

import android.content.Context;
import android.net.Uri;

import com.github.pdf_view.render.PdfViewRenderer;

import java.io.IOException;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Sep 14, 2016
 */
public class PdfViewConfiguration {

    private int startPage;

    private String password;

    private PdfViewRenderer renderer;

    private Uri uri;

    public static final float DEFAULT_DOUBLE_TAP_SCALE = 3F;
    public static final float DEFAULT_MAX_ZOOM = 10F;
    private static final float MIN_SUPPORTED_ZOOM = 0.2F;

    private float doubleTapScale = DEFAULT_DOUBLE_TAP_SCALE;

    private int doubleTapScaleAnimationDuration =  300;
    private int pageSpacing;
    private onPageChangedListener pageChangeListener;
    private OnLoadListener onLoadListener;
    private OnErrorListener onErrorListener;
    private float minScale = MIN_SUPPORTED_ZOOM;
    private float maxScale = DEFAULT_MAX_ZOOM;
    private OnScaleListener onScaleListener;

    public PdfViewConfiguration(Context context, PdfViewRenderer.PdfRendererListener pdfRendererListener) {
        renderer = new PdfViewRenderer(context, pdfRendererListener);
    }

    public PdfViewConfiguration setMaxScale(float maxZoom) {
        this.maxScale = maxZoom;
        return this;
    }

    public PdfViewConfiguration setMinScale(float minZoom) {
        this.minScale = minZoom;
        return this;
    }

    public int getStartPage() {
        return startPage;
    }

    public PdfViewConfiguration setStartPage(int firstPage) {
        this.startPage = firstPage;
        return this;
    }

    public void load() {
        renderer.loadDocument(this);
    }

    public String getPassword() {
        return password;
    }

    public PdfViewConfiguration setPassword(String password) {
        this.password = password;
        return this;
    }

    PdfViewConfiguration setUri(Uri uri) {
        this.uri = uri;
        return this;
    }

    public Uri getUri() {
        return uri;
    }

    public float getDoubleTapScale() {
        return doubleTapScale;
    }

    public PdfViewConfiguration setDoubleTapScale(float doubleTapScale) {
        this.doubleTapScale = doubleTapScale;
        return this;
    }

    public int getDoubleTapScaleAnimationDuration() {
        return doubleTapScaleAnimationDuration;
    }

    public PdfViewConfiguration setDoubleTapScaleAnimationDuration(int doubleTapScaleAnimationDuration) {
        this.doubleTapScaleAnimationDuration = doubleTapScaleAnimationDuration;
        return this;
    }

    public PdfViewConfiguration setPageSpacing(int pageSpacing) {
        this.pageSpacing = pageSpacing;
        return this;
    }

    public int getPageSpacing() {
        return pageSpacing;
    }

    public PdfViewConfiguration setOnPageChangeListener(onPageChangedListener pageChangeListener) {
        this.pageChangeListener = pageChangeListener;
        return this;
    }

    public PdfViewConfiguration setOnLoadListener(OnLoadListener onLoadListener) {
        this.onLoadListener = onLoadListener;
        return this;
    }

    public PdfViewConfiguration setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
        return this;
    }

    public PdfViewConfiguration setOnScaleListener(OnScaleListener onScaleListener) {
        this.onScaleListener = onScaleListener;
        return this;
    }


    public void notifyError(IOException e) {
        if(onErrorListener != null) {
            onErrorListener.onError(e);
        }
    }

    public void notifyPageLoaded(int pageCount) {
        if(onLoadListener != null) {
            onLoadListener.onLoad(pageCount);
        }
    }

    public void notifyPageChanged(int startPage, int endPage) {
        if(pageChangeListener != null) {
            pageChangeListener.onPageChanged(startPage, endPage);
        }
    }

    public void notifyScaleChanged(float oldScale, float newScale,
                                   int oldScrollX, int newScrollX,
                                   int oldScrollY, int newScrollY) {
        if(onScaleListener != null) {
            onScaleListener.onScale(oldScale, newScale,
            oldScrollX, newScrollX,
            oldScrollY, newScrollY);
        }
    }

    public float getMinScale() {
        return minScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public interface onPageChangedListener {
        void onPageChanged(int startPage, int endPage);
    }

    public interface OnLoadListener {
        void onLoad(int pageCount);
    }

    public interface OnErrorListener {
        void onError(IOException e);
    }

    public interface OnScaleListener {
        void onScale(float oldScale, float newScale,
                     int oldScrollX, int newScrollX,
                     int oldScrollY, int newScrollY);
    }
}

