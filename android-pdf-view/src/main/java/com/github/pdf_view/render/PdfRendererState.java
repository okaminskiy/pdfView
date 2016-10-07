package com.github.pdf_view.render;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author <a href="mailto:okaminskyi@intropro.com">Oleh Kaminskyi</a>
 * @since Oct 05, 2016
 */
public class PdfRendererState  implements Parcelable {
    private final float scale;
    private final Uri uri;
    private final int firstRenderedPage;
    private final int pageOriginalScrollY;
    private final int pageOriginalScrollX;

    public PdfRendererState(Uri uri, float scale, int firstRenderedPage, int pageOriginalScrollX, int pageOriginalScrollY) {
        this.uri = uri;
        this.firstRenderedPage = firstRenderedPage;
        this.pageOriginalScrollY = pageOriginalScrollY;
        this.scale = scale;
        this.pageOriginalScrollX = pageOriginalScrollX;
    }

    PdfRendererState(Parcel in) {
        uri = in.readParcelable(ClassLoader.getSystemClassLoader());
        firstRenderedPage = in.readInt();
        pageOriginalScrollY = in.readInt();
        pageOriginalScrollX = in.readInt();
        scale = in.readFloat();
    }

    public static final Creator<PdfRendererState> CREATOR = new Creator<PdfRendererState>() {
        @Override
        public PdfRendererState createFromParcel(Parcel in) {
            return new PdfRendererState(in);
        }

        @Override
        public PdfRendererState[] newArray(int size) {
            return new PdfRendererState[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeInt(firstRenderedPage);
        dest.writeInt(pageOriginalScrollY);
        dest.writeInt(pageOriginalScrollX);
        dest.writeFloat(scale);
    }

    public int getFirstRenderedPage() {
        return firstRenderedPage;
    }

    public int getOriginalPageScrollY() {
        return pageOriginalScrollY;
    }

    public float getScale() {
        return scale;
    }

    public float getOriginalPageScrollX() {
        return pageOriginalScrollX;
    }

    public Uri getUri() {
        return uri;
    }
}

