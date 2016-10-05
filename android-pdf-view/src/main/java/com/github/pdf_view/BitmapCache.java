package com.github.pdf_view;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.v4.graphics.BitmapCompat;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Oct 03, 2016
 */
public class BitmapCache {

    private LinkedHashMap<Point, LinkedList<Bitmap>> sizedCache = new LinkedHashMap<>(0, 0.75f, true);

    private final int maxSize;

    public BitmapCache(int maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized Bitmap peek(Point size) {
        if(!sizedCache.containsKey(size)) {
            return null;
        }
        LinkedList<Bitmap> cached = sizedCache.get(size);
        Bitmap result = cached.removeFirst();
        if(cached.size() == 0) {
            sizedCache.remove(size);
        } else {
            sizedCache.remove(size);
            sizedCache.put(size, cached);
        }
        return result;
    }

    public synchronized void put(Point size, Bitmap bmp) {
        if(sizedCache.containsKey(size)) {
            sizedCache.get(size).push(bmp);
        } else {
            LinkedList<Bitmap> cached = new LinkedList<>();
            cached.add(bmp);
            sizedCache.remove(size);
            sizedCache.put(size, cached);
        }
        trimToSize(maxSize);
    }

    private void trimToSize(int maxSize) {
        long size = computeSize();
        if(size < maxSize || sizedCache.isEmpty()) {
            return;
        }

        Map.Entry<Point, LinkedList<Bitmap>> toEvict
                = sizedCache.entrySet().iterator().next();
        Bitmap bmp = toEvict.getValue().removeFirst();
        bmp.recycle();
        if(toEvict.getValue().size() == 0) {
            sizedCache.remove(toEvict.getKey());
        }
        trimToSize(maxSize);
    }

    private long computeSize() {
        int size = 0;
        for (Map.Entry<Point, LinkedList<Bitmap>> entries : sizedCache.entrySet()) {
            for (Bitmap bitmap : entries.getValue()) {
               size += BitmapCompat.getAllocationByteCount(bitmap);
            }
        }
        return size / 1024;
    }

    public synchronized void evictAll() {
        for (Map.Entry<Point, LinkedList<Bitmap>> entries : sizedCache.entrySet()) {
            for (Bitmap bitmap : entries.getValue()) {
                bitmap.recycle();
            }
        }
        sizedCache.clear();
    }
}

