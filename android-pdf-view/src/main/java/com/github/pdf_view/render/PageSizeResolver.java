package com.github.pdf_view.render;

/**
 * Created by Oleg on 10/11/2016.
 */

public interface PageSizeResolver {

    int getWidth(RenderInfo info);

    int getHeight(RenderInfo info);

    int getNormalizedWidth(RenderInfo info);

    int getNormalizedHeight(RenderInfo info);

    float getOptimalPageScale(RenderInfo info);

    int getRenderLeftOffset(RenderInfo renderInfo);

    int getOriginalHeight(RenderInfo renderInfo);
}
