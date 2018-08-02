package fi.harism.curl;


import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class handles disk and memory caching of bitmaps in conjunction with the
 * Use {@link ImageCache#getInstance()} to get an instance of this
 */
public class ImageCache {

    private volatile static ImageCache mInstance;

    public static final String TAG = "ImageCache";

    // Compression settings when writing images to disk cache
    private static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 85;

    // Constants to easily toggle various caches
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;

    private Set< Bitmap > mReusableBitmaps;

    /**
     *
     */
    private ImageCache() {

        init();
    }

    public static ImageCache getInstance() {

        if ( mInstance == null ) {
            synchronized ( ImageCache.class ) {
                if ( mInstance == null )
                    mInstance = new ImageCache();

            }
        }

        return mInstance;
    }

    /**
     * Initialize the cache, providing all parameters.
     */
    private void init() {

        // If we're running on Honeycomb or newer, create a set of reusable bitmaps that can be
        // populated into the inBitmap field of BitmapFactory.Options.
        // From Honeycomb to JellyBean
        // the size would need to be precise, from KitKat onward the size would just need to
        // be the upper bound (due to changes in how inBitmap can re-use bitmaps).
        mReusableBitmaps = Collections.synchronizedSet(new HashSet<Bitmap>());
    }


    public void addInBitmapOptions( final int width, final int height,  BitmapFactory.Options options ) {

        // inBitmap only works with mutable bitmaps, so force the decoder to
        // return mutable bitmaps.
        options.inMutable = true;

        // Try to find a bitmap to use for inBitmap.
        Bitmap inBitmap = getBitmap( width, height, options );

        if ( inBitmap != null ) {
            // If a suitable bitmap has been found, set it as the value of
            // inBitmap.
            options.inBitmap = inBitmap;
        }
    }

    public Bitmap getBitmapExactSize( final int width, final int height, Bitmap.Config config ){

        Bitmap bitmap = null;

        if ( mReusableBitmaps == null || mReusableBitmaps.isEmpty() )
            return null;

        synchronized (mReusableBitmaps) {

            final Iterator<Bitmap> iterator = mReusableBitmaps.iterator();
            Bitmap item;

            while (iterator.hasNext()) {

                item = iterator.next();

                if ( null != item && item.isMutable() ) {
                    // Check to see it the item can be used for inBitmap
                    if ( item.getWidth() == width && item.getHeight() == height
                            && item.getConfig() == config ) {

                        bitmap = item;
                        // Remove from reusable set so it can't be used again
                        iterator.remove();
                        break;
                    }
                }
                else {
                    // Remove from the set if the reference has been cleared.
                    iterator.remove();
                }
            }
        }

        return bitmap;
    }

    /**
     * @param options - BitmapFactory.Options with out* options populated
     * @return Bitmap that case be used for inBitmap
     */
    protected Bitmap getBitmap( final int width, final int height, BitmapFactory.Options options ) {

        Bitmap bitmap = null;

        if (mReusableBitmaps == null || mReusableBitmaps.isEmpty())
            return null;

        synchronized (mReusableBitmaps) {

            final Iterator<Bitmap> iterator = mReusableBitmaps.iterator();
            Bitmap item;

            while (iterator.hasNext()) {

                item = iterator.next();

                if (null != item && item.isMutable()) {
                    // Check to see it the item can be used for inBitmap
                    if ( canUseForInBitmap( item, width, height, options ) ) {
                        bitmap = item;

                        // Remove from reusable set so it can't be used again
                        iterator.remove();
                        break;
                    }
                }
                else {
                    // Remove from the set if the reference has been cleared.
                    iterator.remove();
                }
            }
        }

        return bitmap;
    }

    public void addBitmapToCache( Bitmap bitmap ){

        if ( mReusableBitmaps != null ) {

            synchronized ( mReusableBitmaps ) {

                mReusableBitmaps.add( bitmap );
            }
        }
    }

    /**
     * Clears both the memory and disk cache associated with this ImageCache object. Note that
     * this includes disk access so this should not be executed on the main/UI thread.
     */
    public void clearCache() {

        if ( mReusableBitmaps != null ) {

            synchronized ( mReusableBitmaps ) {

                final Iterator< Bitmap > iterator = mReusableBitmaps.iterator();
                Bitmap item;

                while ( iterator.hasNext() ) {

                    item = iterator.next();

                    if ( null != item && item.isRecycled() == false )
                        item.recycle();
                }

                mReusableBitmaps.clear();
            }
        }

        System.gc();

        if ( BuildConfig.DEBUG ) {
            Log.d( TAG, "Image cache cleared" );
        }
    }


    /**
     * @param candidate - Bitmap to check
     * @param targetOptions - Options that have the out* value populated
     * @return true if <code>candidate</code> can be used for inBitmap re-use with
     *      <code>targetOptions</code>
     */
    @TargetApi( Build.VERSION_CODES.KITKAT )
    private static boolean canUseForInBitmap( Bitmap candidate, final int width, final int height,
                                              BitmapFactory.Options targetOptions) {

        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ) {

            // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
            return candidate.getWidth() == targetOptions.outWidth
                    && candidate.getHeight() == targetOptions.outHeight
                    && targetOptions.inSampleSize == 1;
        }

        // From Android 4.4 (KitKat) onward we can re-use if the byte size of the new bitmap
        // is smaller than the reusable bitmap candidate allocation byte count.
        final int inSampleSize = targetOptions.inSampleSize == 0 ? 1 : targetOptions.inSampleSize;
        final int outWidth  = width / inSampleSize;
        final int outHeight = height / inSampleSize;
        final int byteCount = outWidth * outHeight * getBytesPerPixel(candidate.getConfig());
        return byteCount <= candidate.getAllocationByteCount();
    }

    public int getCacheTotalSize(){

        int totalSize = 0;

        if ( mReusableBitmaps != null ) {

            synchronized ( mReusableBitmaps ) {

                final Iterator< Bitmap > iterator = mReusableBitmaps.iterator();
                Bitmap item;

                while ( iterator.hasNext() ) {

                    item = iterator.next();
                    totalSize += item.getByteCount();
                }
            }
        }

        return totalSize;
    }

    /**
     * Return the byte usage per pixel of a bitmap based on its configuration.
     * @param config The bitmap configuration.
     * @return The byte usage per pixel.
     */
    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

}

