package com.github.pdf_view.render;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Oct 06, 2016
 */
public interface RenderInfo extends PagesManager.OnOptimalScaleChanged {
    int getScrollX();
    int getScrollY();
    float getScale();
    float getNormalizeScale();

    int getPageSpacing();

    int getRenderOffsetLeft();
    int getRenderOffsetTop();

    int getPartWidth();
    int getPartHeight();

    int getRenderWidth();

    int getRenderHeight();
}
