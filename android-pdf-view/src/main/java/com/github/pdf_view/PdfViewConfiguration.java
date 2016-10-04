package com.github.pdf_view;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Sep 14, 2016
 */
public class PdfViewConfiguration {

    private int firstPage;

    private String password;

    private PdfViewRenderer renderer;

    private Uri uri;

    public static final float DEFAULT_DOUBLE_TAP_SCALE = 3F;
    private float doubleTapScale = DEFAULT_DOUBLE_TAP_SCALE;

    private int doubleTapScaleAnimationDuration =  300;
    private int pageSpacing;
    private onPageChangedListener pageChangeListener;
    private OnLoadListener onLoadListener;
    private OnErrorListener onErrorListener;

    public PdfViewConfiguration(Context context, PdfViewRenderer.PdfRendererListener pdfRendererListener) {
        renderer = new PdfViewRenderer(context, pdfRendererListener);
    }

    public PdfViewConfiguration maxScale(float maxScale) {
        return this;
    }

    public PdfViewConfiguration minScale(float minScale) {
        return this;
    }

    public int getFirstPage() {
        return firstPage;
    }

    public PdfViewConfiguration setFirstPage(int firstPage) {
        this.firstPage = firstPage;
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

    public PdfViewConfiguration setOnLoad(OnLoadListener onLoadListener) {
        this.onLoadListener = onLoadListener;
        return this;
    }

    public PdfViewConfiguration setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
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

    public interface onPageChangedListener {
        void onPageChanged(int startPage, int endPage);
    }

    public interface OnLoadListener {
        void onLoad(int pageCount);
    }

    public interface OnErrorListener {
        void onError(IOException e);
    }
}

