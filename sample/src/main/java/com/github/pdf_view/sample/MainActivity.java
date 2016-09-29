package com.github.pdf_view.sample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.github.pdf_view.PdfView;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private PdfView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
            ).setDoubleTapScaleAnimationDuration(500).setFirstPage(2).maxScale(8F).minScale(0.25F).load();
        }
    }
}
