package com.github.pdf_view;

import android.support.v4.view.ViewCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Sep 14, 2016
 */
public class PdfViewGestureListener extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
    private final PdfView pdfView;
    private final float doubleTapScale;

    public PdfViewGestureListener(PdfView pdfView, PdfViewConfiguration config) {
        this.pdfView = pdfView;
        this.doubleTapScale = config.getDoubleTapScale();
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if(pdfView.getPdfScale() != 1) {
            pdfView.scalePdfTo((int) e.getX(), (int) e.getY(), 1, true);
        } else {
            pdfView.scalePdfTo((int) e.getX(), (int) e.getY(), doubleTapScale, true);
        }
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        pdfView.scalePdfBy((int) scaleGestureDetector.getFocusX(), (int) scaleGestureDetector.getFocusY(),
                scaleGestureDetector.getScaleFactor());
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        pdfView.beginScale();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        pdfView.finishScale();
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    public boolean onSingleTapUp(MotionEvent motionEvent) {
        ViewCompat.stopNestedScroll(pdfView);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float scrollX, float scrollY) {
        if(pdfView.getState() == PdfView.State.SCROLL) {
            return handleScroll((int) scrollX, (int) scrollY);
        } else {
            initScroll();
            return onScroll(e1, e2, scrollX, scrollY);
        }
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        if(pdfView.getState() != PdfView.State.SCROLL) {
            return false;
        }
        boolean result = handleFling(-v, -v1) ;
        ViewCompat.stopNestedScroll(pdfView);
        return result;
    }

    private boolean handleScroll(int dx, int dy) {
        int[] consumed = new int[2];
        int[] offset = new int[2];
        if(ViewCompat.dispatchNestedPreScroll(pdfView, dx, dy, consumed, offset)){
            dx -= consumed[0];
            dy -= consumed[1];
        }
        pdfView.scrollBy(dx, dy, consumed);
        return ViewCompat.dispatchNestedScroll(pdfView, consumed[0], consumed[1], dx - consumed[0], dy - consumed[1], offset);
    }

    private void initScroll() {
        ViewCompat.startNestedScroll(pdfView, (ViewCompat.SCROLL_AXIS_HORIZONTAL| ViewCompat.SCROLL_AXIS_VERTICAL));
    }

    private boolean handleFling(float velocityX, float velocityY) {
        boolean handled;
        if(!ViewCompat.dispatchNestedPreFling(pdfView,velocityX, velocityY)) {
            handled = pdfView.fling((int) velocityX, (int) velocityY);
        }  else {
            handled = true;
        }
        return pdfView.dispatchNestedFling(velocityX, velocityY, handled);
    }
}

