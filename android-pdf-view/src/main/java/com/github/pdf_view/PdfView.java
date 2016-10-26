package com.github.pdf_view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

import com.github.pdf_view.render.PdfRendererState;
import com.github.pdf_view.render.PdfViewRenderer;
import com.github.pdf_view.utils.CancelableTask;
import com.github.pdf_view.utils.SimpleAnimationListener;


/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Sep 14, 2016
 */
public class PdfView extends View implements NestedScrollingChild, PdfViewRenderer.PdfRendererListener, ScrollingView {

    private static final String SUPER_STATE = "SUPER_STATE";
    private static final String RENDERER_STATE = "RENDERER_STATE";

    public PdfViewRenderer pdfViewRenderer;

    private boolean surfaceNotSet;
    private GestureDetector dragGestureDetector;
    private NestedScrollingChildHelper nestedScrollingChildHelper = new NestedScrollingChildHelper(this);
    private State state = State.IDLE;
    private int[] scrollOffset = new int[2];
    private int scrollOffsetX;
    private int scrollOffsetY;
    private OverScroller scroller;
    private FlingTask flingTask;
    private ScaleGestureDetector scaleDetector;
    private long doubleTapAnimationDuration;
    private boolean doubleTapScaleEnabled = true;
    private boolean flingEnabled = true;
    private ValueAnimator scaleAnimation;
    private PdfRendererState pdfState;


    public PdfView(Context context) {
        this(context, null);
    }

    public PdfView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PdfView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PdfView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void scrollBy(int dx, int dy, int[] consumed) {
        int scrollX = pdfViewRenderer.getScrollX();
        int scrollY = pdfViewRenderer.getScrollY();
        scrollBy(dx, dy);
        consumed[0] = pdfViewRenderer.getScrollX() - scrollX;
        consumed[1] = pdfViewRenderer.getScrollY() - scrollY;
    }

    public float getPdfScale() {
        return pdfViewRenderer == null ? 0 : pdfViewRenderer.getScale();
    }

    public State getState() {
        return state;
    }

    public void beginScale() {
        setState(State.ZOOM);
    }

    public void finishScale() {
        setState(State.IDLE);
        if(pdfViewRenderer != null) {
            pdfViewRenderer.updateQuality();
        }
    }

    public void setDoubleTapScaleEnabled(boolean doubleTapScaleEnabled) {
        this.doubleTapScaleEnabled = doubleTapScaleEnabled;
    }

    public void setFlingEnabled(boolean flingEnabled) {
        this.flingEnabled = flingEnabled;
    }

    public enum  State {
        SCROLL, IDLE, FLING, ZOOM, ZOOM_ANIMATED
    }

    @CallSuper
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(pdfViewRenderer != null) {
            pdfViewRenderer.recycle();
        }
    }

    void scalePdfBy(int focusX, int focusY, float deltaScale) {
        pdfViewRenderer.scaleBy(focusX, focusY, deltaScale);
        postInvalidate();
    }

    public void scalePdfTo(final int focusX, final int focusY, float scale, boolean animated) {
        if(scaleAnimation != null && scaleAnimation.isRunning()) {
            scaleAnimation.cancel();
        }

        if(!doubleTapScaleEnabled) {
            return;
        }

        scaleAnimation = ValueAnimator.ofFloat(pdfViewRenderer.getScale(), scale);
        scaleAnimation.addUpdateListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                pdfViewRenderer.scaleTo(focusX, focusY, (float) animation.getAnimatedValue());
                postInvalidate();
            }
        });
        scaleAnimation.setDuration(animated ? doubleTapAnimationDuration : 0);
        scaleAnimation.addListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setState(State.IDLE);
                pdfViewRenderer.updateQuality();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                setState(State.IDLE);
                pdfViewRenderer.updateQuality();
            }
        });
        if(animated) {
            setState(State.ZOOM_ANIMATED);
        }
        scaleAnimation.start();
    }

    private void setState(State state) {
        this.state = state;
    }

    private void cancelAllAnimations() {
        if(flingTask != null) {
            flingTask.cancel();
        }
        if(scaleAnimation != null && scaleAnimation.isRunning()) {
            scaleAnimation.cancel();
        }
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setNestedScrollingEnabled(true);
        scroller = new OverScroller(getContext());
    }

    public PdfViewConfiguration from(Uri uri) {
        return new PdfViewConfiguration(getContext(), PdfView.this).setUri(uri);
    }

    @Override
    public void onDocumentReady(PdfViewRenderer renderer, PdfViewConfiguration configuration) {
        if(this.pdfViewRenderer != null) {
            pdfViewRenderer.recycle();
        }
        this.pdfViewRenderer = renderer;
        if(surfaceNotSet) {
            surfaceNotSet = false;
        }
        pdfViewRenderer.onViewSizeChanged(getWidth(), getHeight());
        if(pdfState != null) {
            pdfViewRenderer.restoreState(pdfState);
            pdfState = null;
        }
        PdfViewGestureListener gestureListener = new PdfViewGestureListener(this, configuration.getDoubleTapScale());
        dragGestureDetector = new GestureDetector(getContext(), gestureListener);
        scaleDetector = new ScaleGestureDetector(getContext(), gestureListener);
        doubleTapAnimationDuration = configuration.getDoubleTapScaleAnimationDuration();
    }

    public void scrollToPage(int page) {
        if(pdfViewRenderer != null) {
            pdfViewRenderer.scrollToPage(page);
        }
    }

    @Override
    public void onPageUpdated() {
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if(pdfViewRenderer == null || !isEnabled()) {
            return result;
        }
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            scrollOffsetX = 0;
            scrollOffsetY = 0;
            cancelAllAnimations();
        }
        event.offsetLocation(scrollOffsetX, scrollOffsetY);
        result |= scaleDetector.onTouchEvent(event);
        if(state != State.ZOOM) {
            result |= dragGestureDetector.onTouchEvent(event);
        }
        return result;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        nestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return nestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public void stopNestedScroll() {
        setState(State.IDLE);
        if(pdfViewRenderer != null) {
            pdfViewRenderer.updateQuality();
        }
        nestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        setState(State.SCROLL);
        return nestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return nestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        boolean result = nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, scrollOffset);
        scrollOffsetX += scrollOffset[0];
        scrollOffsetY += scrollOffset[1];
        return result;
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        boolean result =  nestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, scrollOffset);
        scrollOffsetX += scrollOffset[0];
        scrollOffsetY += scrollOffset[1];
        return result;
    }


    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public void scrollBy(int x, int y) {
        scrollTo(pdfViewRenderer.getScrollX() + x, pdfViewRenderer.getScrollY() + y);
    }

    @Override
    public void scrollTo(int x, int y) {
        int oldScrollX = pdfViewRenderer.getScrollX();
        int oldScrollY = pdfViewRenderer.getScrollY();
        pdfViewRenderer.scrollTo(x, y);
        int newScrollX = pdfViewRenderer.getScrollX();
        int newScrollY = pdfViewRenderer.getScrollY();
        if(newScrollX == oldScrollX && newScrollY == oldScrollY) {
            return;
        }
        awakenScrollBars();
        postInvalidate();
        onScrollChanged(newScrollX, newScrollY, oldScrollX, oldScrollY);
    }

    public boolean fling(int velocityX, int velocityY) {
        if(flingTask != null) {
            flingTask.cancel();
        }

        if(!flingEnabled) {
            return false;
        }

        if(!ViewCompat.canScrollVertically(this, velocityY)
                && !ViewCompat.canScrollHorizontally(this, velocityX)) {
            return false;
        }
        setState(State.FLING);
        scroller.fling(pdfViewRenderer.getScrollX(), pdfViewRenderer.getScrollY(), velocityX, velocityY,
                0, pdfViewRenderer.getHorizontalScrollRange(),
                0, Integer.MAX_VALUE);
        flingTask = new FlingTask(this, scroller);
        ViewCompat.postOnAnimation(this, flingTask);
        return true;
    }

    public static class FlingTask extends CancelableTask {
        private final OverScroller scroller;
        PdfView pdfView;

        FlingTask(PdfView pdfView, OverScroller scroller) {
            this.pdfView = pdfView;
            this.scroller = scroller;
        }

        @Override
        public void action() {
            scroller.computeScrollOffset();
            if(scroller.isFinished()) {
                pdfView.setState(State.IDLE);
                pdfView.pdfViewRenderer.updateQuality();
                return;
            }
            ViewCompat.postOnAnimation(pdfView, this);
            pdfView.scrollTo(scroller.getCurrX(), scroller.getCurrY());
        }

        @Override
        public void cancel() {
            super.cancel();
            scroller.forceFinished(true);
            pdfView.pdfViewRenderer.updateQuality();
            pdfView.setState(State.IDLE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(pdfViewRenderer != null) {
            pdfViewRenderer.draw(canvas);
        }
    }

    @Override
    public int computeVerticalScrollRange() {
        if(pdfViewRenderer != null) {
            return pdfViewRenderer.getVerticalScrollRange();
        } else {
            return 0;
        }
    }

    @Override
    public int computeVerticalScrollOffset() {
        if(pdfViewRenderer != null) {
            return pdfViewRenderer.getScrollY();
        } else {
            return 0;
        }
    }

    @Override
    public int computeVerticalScrollExtent() {
        if(pdfViewRenderer != null) {
            return pdfViewRenderer.getVerticalScrollExtent();
        } else {
            return 0;
        }
    }

    @Override
    public int computeHorizontalScrollRange() {
        if(pdfViewRenderer != null) {
            return pdfViewRenderer.getHorizontalScrollRange();
        } else {
            return 0;
        }
    }

    @Override
    public int computeHorizontalScrollOffset() {
        if(pdfViewRenderer != null) {
            return pdfViewRenderer.getScrollX();
        } else {
            return 0;
        }
    }

    @Override
    public int computeHorizontalScrollExtent() {
        if(pdfViewRenderer != null) {
            return pdfViewRenderer.getHorizontalScrollExtent();
        } else {
            return 0;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SUPER_STATE, super.onSaveInstanceState());
        if(pdfViewRenderer != null) {
            bundle.putParcelable(RENDERER_STATE, pdfViewRenderer.getCurrentState());
        }
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle stateBundle = (Bundle) state;
        super.onRestoreInstanceState(stateBundle.getParcelable(SUPER_STATE));
        pdfState = stateBundle.getParcelable(RENDERER_STATE);
    }
}

