package com.github.pdf_view.render;

/**
 * Created by Oleg on 10/11/2016.
 */

public class StubPageSizeResolver implements PageSizeResolver{

    public StubPageSizeResolver() {
    }

    @Override
    public int getWidth(RenderInfo info) {
        return (int) (info.getRenderWidth() * info.getScale());
    }

    @Override
    public int getHeight(RenderInfo info) {
        return (int) (info.getRenderHeight() * info.getScale());
    }

    @Override
    public int getNormalizedWidth(RenderInfo info) {
        return info.getRenderWidth();
    }

    @Override
    public int getNormalizedHeight(RenderInfo info) {
        return info.getRenderHeight();
    }

    @Override
    public float getOptimalPageScale(RenderInfo info) {
        return info.getNormalizeScale();
    }

    @Override
    public int getRenderLeftOffset(RenderInfo renderInfo) {
        return 0;
    }

    public int getOriginalHeight(RenderInfo renderInfo) {
        return (int) (renderInfo.getRenderHeight() / renderInfo.getNormalizeScale());
    }
}
