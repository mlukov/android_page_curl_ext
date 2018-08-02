package fi.harism.curl;

import android.graphics.Bitmap;

public interface CurlPageProvider {

    /**
     * Called once new bitmap is needed. Width and height are in pixels
     * telling the size it will be drawn on screen and following them
     * ensures that aspect ratio remains. But it's possible to return bitmap
     * of any size though.<br/>
     * <br/>
     * Index is a number between 0 and getBitmapCount() - 1.
     */
    Bitmap getPageBitmap( int width, int height, int pageIndex );

    Bitmap createBitmapFromCache( final int width, final int height, Bitmap.Config config  );

    void recycleBitmap( Bitmap bitmap );

    /**
     * Return number of pages/bitmaps available.
     */
    int getPageCount();

    boolean isPageHardcover( final int pageIndex );
}
