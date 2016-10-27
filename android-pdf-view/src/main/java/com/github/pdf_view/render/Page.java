package com.github.pdf_view.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Oct 07, 2016
 */
public class Page {

    private static final String TAG = Page.class.getName();
    private PageSizeResolver resolver;
    private RenderInfo renderInfo;
    private PagePart thumbnail;
    private float lastUpdatedScale;
    private Page nextPage;
    private int index;
    List<PagePart> parts = new ArrayList<>();
    private boolean isDefault = true;
    private int topPosition;

    public Page (int pageIndex, RenderInfo renderInfo) {
        this.index = pageIndex;
        resolver = new StubPageSizeResolver();
        this.renderInfo = renderInfo;
    }


    public int getBottom() {
        return getTop() + getHeight();
    }

    private int getHeight() {
        return resolver.getHeight(renderInfo);
    }

    private int getWidth() {
        return resolver.getWidth(renderInfo);
    }

    public int getTop() {
        return (int) ((topPosition * renderInfo.getNormalizeScale() + renderInfo.getPageSpacing() * index) * renderInfo.getScale());
    }

    private int getPageOriginalBottom() {
        return topPosition + resolver.getOriginalHeight(renderInfo);
    }

    public float getScale() {
        return renderInfo.getScale();
    }

    public int getRenderTop() {
        return renderInfo.getRenderOffsetTop() + getTop() - renderInfo.getScrollY();
    }

    public int getRenderLeft() {
        return resolver.getRenderLeftOffset(renderInfo) + renderInfo.getRenderOffsetLeft() - renderInfo.getScrollX();
    }

    public List<PagePart> getParts() {
        return parts;
    }

    public void prepareActualParts() {
        if(isDefault) {
            return;
        }
        int renderWidth = renderInfo.getRenderWidth();
        int renderHeight = renderInfo.getRenderHeight();
        int partWidth = renderInfo.getPartWidth();
        int partHeight = renderInfo.getPartHeight();
        int scrollX = renderInfo.getScrollX();
        int scrollY = renderInfo.getScrollY();
        float scale = renderInfo.getScale();

        int visiblePageTop = Math.max(scrollY - getTop(), 0);
        int visiblePageLeft = Math.max(scrollX - getPageOffsetLeft(), 0);
        int visiblePageRight = Math.min(renderWidth + visiblePageLeft, getWidth());
        int visiblePageBottom = visiblePageTop + Math.min(renderHeight - Math.max(0, getTop() - scrollY), getHeight() - visiblePageTop);
        removeUnusedParts(visiblePageLeft, visiblePageTop, visiblePageRight, visiblePageBottom);
        int left = (visiblePageLeft / partWidth) * partWidth;
        int top = (visiblePageTop / partHeight) * partHeight;
        while (top < visiblePageBottom) {
            int tempLeft = left;
            while (tempLeft < visiblePageRight) {
                Rect partBounds = new Rect(tempLeft, top,
                        Math.min(getWidth(), tempLeft + partWidth),
                        Math.min(top + partHeight, getHeight()));
                PagePart pagePart = new PagePart(partBounds, index, getWidth(),
                        getHeight(), scale);
                if (!parts.contains(pagePart)) {
                    parts.add(pagePart);
                }
                tempLeft += partWidth;
            }
            top += partHeight;
        }
    }

    private boolean isOverlaps(Rect rect1, Rect rect2) {
        boolean result = rect1.left < rect2.left + rect2.width()
                && rect1.left + rect1.width() > rect2.left
                && rect1.top < rect2.top + rect2.height()
                && rect1.top + rect1.height() > rect2.top;
        return result;
    }

    private void removeUnusedParts(int left, int top, int right, int bottom) {
        Rect visibleBounds = new Rect(left, top, right, bottom);
        float scale = renderInfo.getScale();
        if (lastUpdatedScale != scale) {
            parts.clear();
            lastUpdatedScale = scale;
            return;
        }
        List<PagePart> actualLeft = new ArrayList<>();
        for (PagePart pPart : parts) {
            if (isOverlaps(pPart.getScaledBounds(scale), visibleBounds)) {
                actualLeft.add(pPart);
            }
        }
        parts = actualLeft;
    }

    public PagePart getThumbnail() {
        if(isDefault) {
            return null;
        }
        if(thumbnail == null) {
            createThumbnail();
        }
        return thumbnail;
    }

    public void preparePage() {
        if(!isDefault) {
            return;
        }
        Log.i(TAG, "Page " + index + " is ready");
        Point pageSize = renderInfo.getPageSize(index);
        int oldBottom = getPageOriginalBottom();
        resolver = new RealPageSizeResolver(pageSize.x, pageSize.y);
        isDefault = false;
        if(nextPage != null) {
            offsetBy(getPageOriginalBottom() - oldBottom);
        }
        Log.wtf("Okaminskyi", "Page is prepared " + index);
    }

    private void offsetBy(int dy) {
        Page next = nextPage;
        while (next != null) {
            next.topPosition += dy;
            next = next.nextPage;
        }
    }

    private void createThumbnail() {
        thumbnail = new PagePart(
                new Rect(0, 0, resolver.getNormalizedWidth(renderInfo),
                        resolver.getNormalizedHeight(renderInfo)), index,
                resolver.getNormalizedWidth(renderInfo), resolver.getNormalizedHeight(renderInfo), 1);
    }

    public int getPageOffsetLeft() {
        return resolver.getRenderLeftOffset(renderInfo);
    }

    public float getOptimalPageScale() {
        return resolver.getOptimalPageScale(renderInfo);
    }

    public void setNextPage(Page nextPage) {
        this.nextPage = nextPage;
    }

    public void drawThumbnail(Canvas canvas) {
        if(thumbnail != null) {
            thumbnail.drawPart(canvas, getScale(), getRenderLeft(), getRenderTop(), true);
        } else {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            canvas.drawRect(getRenderLeft(), getRenderTop(), getRenderLeft() + getWidth(), getBottom(), paint);
        }
    }

    public boolean isNotStub() {
        return !isDefault;
    }

    public int getIndex() {
        return index;
    }

    public void setPreviousPage(Page page) {
        topPosition = page.getPageOriginalBottom();
    }
}

