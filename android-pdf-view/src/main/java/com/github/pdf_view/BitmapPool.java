package com.github.pdf_view;

import android.graphics.Bitmap;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Oct 03, 2016
 */
public class BitmapPool {
    private int poolSize;
    private LinkedList<Bitmap> bitmaps = new LinkedList<>();

    public BitmapPool(int poolSize) {
        this.poolSize = poolSize;
    }

    public synchronized Bitmap peek() {
        try {
            return bitmaps.removeFirst();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public synchronized void put(Bitmap bmp) {
        if(poolSize == bitmaps.size()) {
           peek().recycle();
        } else {
            bitmaps.add(bmp);
        }
    }

    public synchronized void recycle() {
        for (Bitmap bmp : bitmaps) {
            bmp.recycle();
        }
    }
}

