package com.github.pdf_view.utils;

/**
 * Created by Oleg on 3/2/2016.
 */
public abstract class CancelableTask implements Runnable {
    private boolean isCanceled;

    public abstract void action();

    @Override
    public void run() {
        if(isCanceled) {
            return;
        }

        action();
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void cancel() {
        isCanceled = true;
    }
}
