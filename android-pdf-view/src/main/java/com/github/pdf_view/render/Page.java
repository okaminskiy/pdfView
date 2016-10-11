package com.github.pdf_view.render;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Oct 07, 2016
 */
public class Page {

    private RenderInfo renderInfo;
    private PagePart thumbnail;
    private float lastUpdatedScale;
    private int pageOffsetTop;
    private int width;
    private int pageOffsetLeft;
    private int height;
    private int index;
    List<PagePart> parts = new ArrayList<>();

    public Page (int pageIndex, int width, int height, RenderInfo renderInfo) {
        this.index = pageIndex;
        this.width = width;
        this.height = height;
        this.renderInfo = renderInfo;
    }


    public int getBottom() {
        return getTop() + getHeight();
    }

    private int getHeight() {
        return (int) (height * renderInfo.getScale() * renderInfo.getNormalizeScale());
    }

    private int getWidth() {
        return (int) (width * renderInfo.getScale() * renderInfo.getNormalizeScale());
    }

    public int getTop() {
        return (int) ((pageOffsetTop * renderInfo.getNormalizeScale() + renderInfo.getPageSpacing() * index) * renderInfo.getScale());
    }

    public float getScale() {
        return renderInfo.getScale();
    }

    public int getRenderTop() {
        return renderInfo.getRenderOffsetTop() + getTop() - renderInfo.getScrollY();
    }

    public int getRenderLeft() {
        return (int) (pageOffsetLeft * renderInfo.getScale() + renderInfo.getRenderOffsetLeft() - renderInfo.getScrollX());
    }

    public List<PagePart> getParts() {
        return parts;
    }

    public void prepareActualParts() {
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
        return thumbnail;
    }

    public void createThumbnail() {
        thumbnail = new PagePart(
                new Rect(0, 0, getWidth(), getHeight()), index,
                getWidth(), getHeight(), getScale());
    }

    public int getPageOffsetLeft() {
        return (int) (pageOffsetLeft * getScale());
    }

    public int getOriginalWidth() {
        return width;
    }

    public int getPageOffsetTop() {
        return pageOffsetTop;
    }

    public int getOriginalHeight() {
        return height;
    }

    public void setPageOffsetTop(int pageOffsetTop) {
        this.pageOffsetTop = pageOffsetTop;
    }

    public void setPageOffsetLeft(int pageOffsetLeft) {
        this.pageOffsetLeft = pageOffsetLeft;
    }
}

