package com.github.pdf_view.render;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

/**
 * Created by Oleg on 10/9/2016.
 */

public class PagesManager {
    private final RenderInfo renderInfo;
    private final PdfDocument pdfDocument;
    private final PdfiumCore pdfiumCore;
    private final int pageCount;
    private final int pagesMargin;
    private int viewWidth;
    private int viewHeight;

    private void init(int pageIndex, int viewWidth, int viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    public PagesManager(RenderInfo renderInfo, PdfDocument document, PdfiumCore core, int pagesMargin) {
        this.renderInfo = renderInfo;
        this.pdfiumCore = core;
        this.pdfDocument = document;
        this.pageCount = core.getPageCount(document);
        this.pagesMargin = pagesMargin;
    }

    public interface OnOptimalScaleChanged {
        void onOptimalScaleChanged(float newOptimalScale);
    }
}
