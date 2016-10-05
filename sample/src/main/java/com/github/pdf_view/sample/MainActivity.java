package com.github.pdf_view.sample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pdf_view.PdfView;
import com.github.pdf_view.PdfViewConfiguration;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
        implements PdfViewConfiguration.onPageChangedListener, PdfViewConfiguration.OnLoadListener, PdfViewConfiguration.OnErrorListener, PdfViewConfiguration.OnScaleListener {

    private static final int PICK_PDF_REQUEST = 1;
    private PdfView pdfView;
    private int pageCount;
    private TextView pagesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pagesText = (TextView) findViewById(R.id.pagesText);
        pdfView = (PdfView) findViewById(R.id.pdfView);
    }

    public void pickPdf(View view) {
        Intent intentPDF = new Intent(Intent.ACTION_GET_CONTENT);
        intentPDF.setType("application/pdf");
        intentPDF.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intentPDF, PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode != PICK_PDF_REQUEST) {
            return;
        }

        if(resultCode == RESULT_OK) {
            pdfView.from(data.getData()).setDoubleTapScale(4f).setPageSpacing(
                    getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin)
            ).setDoubleTapScaleAnimationDuration(500)
                    .setPassword("12345")
                    .setStartPage(2)
                    .setOnPageChangeListener(this)
                    .setOnLoadListener(this)
                    .setOnErrorListener(this)
                    .setOnScaleListener(this)
                    .setMaxScale(8F).setMinScale(0.5F).load();
        }
    }

    @Override
    public void onPageChanged(int startPage, int endPage) {
        String pagesChangedText = String.format(getString(R.string.pageChangedFormat),
                startPage, endPage, pageCount);
        pagesText.setText(pagesChangedText);
    }

    @Override
    public void onLoad(int pageCount) {
        Toast.makeText(this, getString(R.string.documentLoaded), Toast.LENGTH_LONG).show();
        this.pageCount = pageCount;
    }

    @Override
    public void onError(IOException e) {
        Toast.makeText(this, getString(R.string.cannotLoadDocument), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onScale(float oldScale, float newScale, int oldScrollX, int newScrollX, int oldScrollY, int newScrollY) {
    }
}
