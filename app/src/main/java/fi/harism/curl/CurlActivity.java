/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.curl;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Simple Activity for curl testing.
 * 
 * @author harism
 */
public class CurlActivity extends Activity implements CurlView.CurlViewIndexChangedListener {

	public static final String TAG = CurlActivity.class.getSimpleName();

	public static final float WIDE_PAGE_MIN_RATIO = 1.2f;

	private CurlView mCurlView;

	private float                           mCurlViewYMargin = 0.32f;
	private float                           mPageRatio = 1.778f;
	private boolean                         mLandscape;
	private Point                           mLastCurlViewPress = new Point( 0,0 );
	private int                             mCurlViewWidth;
	private int                             mCurlViewHeight;
	private int                             mCurlViewMargin;
	private int                         	mPageIndex;
	private DisplayMetrics 					mMetrics;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		mMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics( mMetrics );

		mLandscape          = mMetrics.widthPixels > mMetrics.heightPixels;

		mCurlView = findViewById(R.id.curl);

		Point animationAreaSize = getAnimationAreaSize();

		RelativeLayout.LayoutParams layoutParamsCurl = (RelativeLayout.LayoutParams )mCurlView.getLayoutParams();
		layoutParamsCurl.width = mCurlViewWidth = animationAreaSize.x;
		layoutParamsCurl.height = mCurlViewHeight =  animationAreaSize.y;
		mCurlViewMargin =  (int)( ( (float)mCurlViewHeight * mCurlViewYMargin) / 2f );

		if( mPageRatio > WIDE_PAGE_MIN_RATIO )
			layoutParamsCurl.addRule( RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE );

		mCurlView.setLayoutParams( layoutParamsCurl );

		mCurlView.setCurrentIndex( mPageIndex );
		mCurlView.setBackgroundColor( Color.TRANSPARENT );
		mCurlView.set2PagesLandscape( true );
		mCurlView.setIndexChangedListener( this );

		int index = 0;
		if (getLastNonConfigurationInstance() != null) {
			index = (Integer) getLastNonConfigurationInstance();
		}

		mCurlView.setCurlPageProvider(new PageProvider());
		mCurlView.setSizeChangedObserver(new SizeChangedObserver());

		mCurlView.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( View v ) {

				if ( mLastCurlViewPress.x <= 0 || mLastCurlViewPress.y <= 0 )
					return;

				// right item
				if ( mLastCurlViewPress.x > mCurlViewWidth / 2 ) {
					//onRightPageClicked();
				}
				else{ // left item

					//onLeftPageClicked();
				}
			}
		} );

		mCurlView.setOnTouchListener( new View.OnTouchListener() {
			@Override
			public boolean onTouch( View v, MotionEvent event ) {

				try {
					if ( event.getAction() == MotionEvent.ACTION_DOWN ) {

						mLastCurlViewPress.x = ( int ) event.getX();
						mLastCurlViewPress.y = ( int ) event.getY();

						if ( mLastCurlViewPress.y < mCurlViewMargin
								|| mCurlViewHeight - mLastCurlViewPress.y < mCurlViewMargin ) {

							mLastCurlViewPress.x = 0;
							mLastCurlViewPress.y = 0;
							return true;
						} else
							mCurlView.onTouch( v, event );

					}//if
					else {

						mCurlView.onTouch( v, event );

						Point currentPoint = new Point( ( int ) event.getX(), ( int ) event.getY() );

						if ( event.getAction() == MotionEvent.ACTION_MOVE
								&& ( Math.abs( mLastCurlViewPress.x - currentPoint.x ) > 10
								|| Math.abs( mLastCurlViewPress.y - currentPoint.y ) > 10 ) ) {

							mLastCurlViewPress.x = 0;
							mLastCurlViewPress.y = 0;
						}//if
					}
					return false;
				}
				catch( Throwable ex ){
					return false;
				}
			}
		} );

		// This is something somewhat experimental. Before uncommenting next
		// line, please see method comments in CurlView.
		// mCurlView.setEnableTouchPressure(true);
	}

	@Override
	public void onPause() {
		super.onPause();
		mCurlView.onPause();
		ImageCache.getInstance().clearCache();
	}

	@Override
	public void onResume() {
		super.onResume();
		mCurlView.onResume();
	}

	@Override
	public void onCurlViewIndexChanged( int rightItemIndex ) {

		mPageIndex = rightItemIndex;

		Log.i( TAG, "Page index changed:" +mPageIndex );
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mCurlView.getCurrentIndex();
	}

	private Point getAnimationAreaSize(){

		Resources resources = getResources();
		final float margin            = 10;
		final float navButtonsHeight  = 10;

		Point point = new Point();
		// In landscape mode, display two pages, view will be two times wider it's height
		// minus it's Y-axis margins
		final float decreaseInHeight = ( navButtonsHeight * 2f ) + ( margin * 6f ) ;
		final float viewMaxHeight   = mMetrics.heightPixels;

		final float decreaseInWidth = mPageRatio > WIDE_PAGE_MIN_RATIO || !mLandscape ? margin * 4f : 0f;
		final float viewMaxWidth    = mMetrics.widthPixels - decreaseInWidth;
		final float halfViewWidth   = viewMaxWidth / 2f ;

		float viewWidth = viewMaxWidth;
		if( halfViewWidth / mPageRatio > viewMaxHeight - decreaseInHeight )
			viewWidth = ( viewMaxHeight - decreaseInHeight ) * 2f * mPageRatio;

		final float viewHeight = ( viewWidth / 2f ) / mPageRatio;

		mCurlViewYMargin = ( viewMaxHeight - viewHeight ) / viewMaxHeight;

		point.x  = (int)viewWidth;
		point.y = (int)viewMaxHeight;

		return point;
	}
	/**
	 * Bitmap provider.
	 */
	private class PageProvider implements CurlPageProvider {

		// Bitmap resources.
		private int[] mBitmapIds = {
				R.drawable.by_the_sea, R.drawable.embroidery,
				R.drawable.lighthouse_, R.drawable.lights,
				R.drawable.mountain_, R.drawable.reef_,
				R.drawable.sea_of_fl, R.drawable.w_and_balconies,

				R.drawable.by_the_sea, R.drawable.embroidery,
				R.drawable.lighthouse_, R.drawable.lights,
				R.drawable.mountain_, R.drawable.reef_,
				R.drawable.sea_of_fl, R.drawable.w_and_balconies
		};

		@Override
		public int getPageCount() {

			return mBitmapIds.length/2;
		}

		@Override
		public Bitmap getPageBitmap( int width, int height, int pageIndex ){

			return loadBitmap( width, height, pageIndex );
		}

		@Override
		public boolean isPageHardcover( final int pageIndex ){

			if( pageIndex == 1 || pageIndex == (mBitmapIds.length/2) - 2  )
				return true;

			return false;
		}

		@Override
		public void recycleBitmap( Bitmap bitmap ){

			ImageCache.getInstance().addBitmapToCache( bitmap );
		}

		private Bitmap loadBitmap( int width, int height, int index) {

			Bitmap b = getBitmapFromCache( width, height, Bitmap.Config.ARGB_8888,
					ImageCache.getInstance() );

			Log.i( TAG, "Image Cache Size: " +ImageCache.getInstance().getCacheTotalSize() );

			b.eraseColor(0xFFFFFFFF );
			Canvas c = new Canvas(b);
			Drawable d = getResources().getDrawable( mBitmapIds[index] );

			int margin = 7;
			int border = 3;
			Rect r = new Rect(margin, margin, width - margin, height - margin);

			int imageWidth = r.width() - (border * 2);
			int imageHeight = imageWidth * d.getIntrinsicHeight()
					/ d.getIntrinsicWidth();
			if (imageHeight > r.height() - (border * 2)) {
				imageHeight = r.height() - (border * 2);
				imageWidth = imageHeight * d.getIntrinsicWidth()
						/ d.getIntrinsicHeight();
			}

			r.left += ((r.width() - imageWidth) / 2) - border;
			r.right = r.left + imageWidth + border + border;
			r.top += ((r.height() - imageHeight) / 2) - border;
			r.bottom = r.top + imageHeight + border + border;

			Paint p = new Paint();
			p.setColor(0xFFC0C0C0);
			c.drawRect(r, p);
			r.left += border;
			r.right -= border;
			r.top += border;
			r.bottom -= border;

			d.setBounds(r);
			d.draw(c);

			return b;
		}

		@Override
		public Bitmap createBitmapFromCache( final int width, final int height, Bitmap.Config config  ){

			return getBitmapFromCache( width, height, config, ImageCache.getInstance() );
		}

		public final Bitmap getBitmapFromCache( final int width, final int height, Bitmap.Config config, ImageCache cache ) {

			Bitmap bitmap = null;

			// If we're running on Honeycomb or newer, try to use inBitmap.
			if ( cache != null )
				bitmap = cache.getBitmapExactSize( width, height, config );

			if ( bitmap == null )
				bitmap = Bitmap.createBitmap( width, height, config );

			return bitmap;
		}

	}

	/**
	 * CurlView size changed observer.
	 */
	private class SizeChangedObserver implements CurlView.SizeChangedObserver {
		@Override
		public void onSizeChanged(int w, int h) {

			final float halfHeightMargin = ( mCurlViewYMargin ) / 2;
			mCurlView.setViewMode( CurlView.SHOW_TWO_PAGES );
			mCurlView.setMargins( 0f, halfHeightMargin, 0f, halfHeightMargin );
		}
	}

}