package com.github.pdf_view.render;

/**
 * Created by Oleg on 10/11/2016.
 */

public class RealPageSizeResolver implements PageSizeResolver {

    private int width;
    private int height;

    public RealPageSizeResolver(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth(RenderInfo info) {
        return (int) (width * info.getNormalizeScale() * info.getScale());
    }

    @Override
    public int getHeight(RenderInfo info) {
        return (int) (height * info.getNormalizeScale() * info.getScale());
    }

    @Override
    public int getNormalizedWidth(RenderInfo info) {
        return (int) (width * info.getNormalizeScale());
    }

    @Override
    public int getNormalizedHeight(RenderInfo info) {
        return (int) (height * info.getNormalizeScale());
    }

    @Override
    public float getOptimalPageScale(RenderInfo info) {
        return (float) info.getRenderWidth() / width;
    }

    @Override
    public int getRenderLeftOffset(RenderInfo renderInfo) {
        if(getOptimalPageScale(renderInfo) != renderInfo.getNormalizeScale()) {
            return (int) ((renderInfo.getRenderWidth() *
                                renderInfo.getScale() - getWidth(renderInfo)) / 2);
        }
        return 0;
    }

    @Override
    public int getOriginalHeight(RenderInfo renderInfo) {
        return height;
    }
}
