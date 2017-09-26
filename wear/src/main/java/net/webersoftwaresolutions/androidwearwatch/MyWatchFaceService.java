package net.webersoftwaresolutions.androidwearwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 * Created by jaWeber on 9/21/17.
 *
 * From this tutorial: https://code.tutsplus.com/tutorials/creating-an-android-wear-watch-face--cms-23718
 *
 * I followed along and added the comments about what the methods are doing.  Needed to Run -> edit configurations and
 * for launch activity - instead of default activity - choose nothing.  Also compared code from exercise with what
 * author has in github (he was missing a few things).
 */

public class MyWatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new WatchFaceEngine();
    }

    private class WatchFaceEngine extends Engine {

        // ***********************************************************************************
        // The first thing you're going to want to do is implement a set of member variables in your
        // engine to keep track of device states, timer intervals, and attributes for your display.

        // As you can see, we define the TypeFace that we will use for our digital watch text as well
        // as the watch face background color and text color. The Time object is used for, you
        // guessed it, keeping track of the current device time. mUpdateRateMs is used to control a
        // timer that we will need to implement to update our watch face every second (hence the
        // 1000 milliseconds value for mUpdateRateMs), because the standard WatchFaceService only
        // keeps track of time in one minute increments. mXOffset and mYOffset are defined once the
        // engine knows the physical shape of the watch so that our watch face can be drawn without
        // being too close to the top or left of the screen, or being cut off by a rounded corner.
        // The three boolean values are used to keep track of different device and application states.

        // Member variables
        // ***********************************************************************************
        private Typeface WATCH_TEXT_TYPEFACE = Typeface.create( Typeface.SERIF, Typeface.NORMAL );

        private static final int MSG_UPDATE_TIME_ID = 42;
        private static final long DEFAULT_UPDATE_RATE_MS = 1000;
        private long mUpdateRateMs = 1000;

        private Time mDisplayTime;

        private Paint mBackgroundColorPaint;
        private Paint mTextColorPaint;

        private boolean mHasTimeZoneReceiverBeenRegistered = false;
        private boolean mIsInMuteMode;
        private boolean mIsLowBitAmbient;

        private float mXOffset;
        private float mYOffset;

        private int mBackgroundColor = Color.parseColor( "black" );
        private int mTextColor = Color.parseColor( "red" );

        // The next object you will need to define is a broadcast receiver that handles the
        // situation where a user may be traveling and change time zones. This receiver simply
        // clears out the saved time zone and resets the display time.
        final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mDisplayTime.clear( intent.getStringExtra( "time-zone" ) );
                mDisplayTime.setToNow();
            }
        };

        // After your receiver is defined, the final object you will need to create at the top of
        // your engine is a Handler to takes care of updating your watch face every second. This is
        // necessary because of the limitations of WatchFaceService discussed above. If your own
        // watch face only needs to be updated every minute, then you can safely ignore this section.

        // The implementation of the Handler is pretty straightforward. It first checks the message
        // ID. If matches MSG_UPDATE_TIME_ID, it continues to invalidate the current view for
        // redrawing. After the view has been invalidated, the Handler checks to see if the screen
        // is visible and not in ambient mode. If it is visible, it sends a repeat request a second
        // later. The reason we're only repeating the action in the Handler when the watch face is
        // visible and not in ambient mode is that it can be a little battery intensive to keep
        // updating every second. If the user isn't looking at the screen, we simply fall back on
        // the WatchFaceService implementation that updates every minute.
        private final Handler mTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch( msg.what ) {
                    case MSG_UPDATE_TIME_ID: {
                        invalidate();
                        if( isVisible() && !isInAmbientMode() ) {
                            long currentTimeMillis = System.currentTimeMillis();
                            long delay = mUpdateRateMs - ( currentTimeMillis % mUpdateRateMs );
                            mTimeHandler.sendEmptyMessageDelayed( MSG_UPDATE_TIME_ID, delay );
                        }
                        break;
                    }
                }
            }
        };

        // ***********************************************************************************
        // start initializing the watch face. Engine has an onCreate method that should be used
        // for creating objects and other tasks that can take a significant amount of time and
        // battery. You will also want to set a few flags for the WatchFaceStyle here to control
        // how the system interacts with the user when your watch face is active.

        // For the sample app, you'll use setWatchFaceStyle to set the background of your
        // notification cards to briefly show if the card type is set as interruptive. You'll
        // also set the peek mode so that notification cards only take up as much room as necessary.

        // Finally, you'll want to tell the system to not show the default time since you will be
        // displaying it yourself. While these are only a few of the options available, you can
        // find even more information in the official documentation for the WatchFaceStyle.Builder
        // object.

        // After your WatchFaceStyle has been set, you can initialize mDisplayTime as a new Time object.

        // initBackground and initDisplayText allocate the two Paint objects that you defined at the top of the engine.
        // ***********************************************************************************
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle( new WatchFaceStyle.Builder( MyWatchFaceService.this )
                    .setBackgroundVisibility( WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE )
                    .setCardPeekMode( WatchFaceStyle.PEEK_MODE_VARIABLE )
                    .setShowSystemUiTime( false )
                    .build()
            );

            initBackground();
            initDisplayText();

            mDisplayTime = new Time();

        }

        @Override
        public void onDestroy() {
            mTimeHandler.removeMessages( MSG_UPDATE_TIME_ID );
            super.onDestroy();
        }
        // ***********************************************************************************
        // The background and text then have their color set and the text has its typeface and
        // font size set, while also turning on anti-aliasing.
        // ***********************************************************************************
        private void initBackground() {
            mBackgroundColorPaint = new Paint();
            mBackgroundColorPaint.setColor( mBackgroundColor );
        }

        private void initDisplayText() {
            mTextColorPaint = new Paint();
            mTextColorPaint.setColor( mTextColor );
            mTextColorPaint.setTypeface( WATCH_TEXT_TYPEFACE );
            mTextColorPaint.setAntiAlias( true );
            mTextColorPaint.setTextSize( getResources().getDimension( R.dimen.text_size ) );
        }

        // ***********************************************************************************
        // When this method is called, it checks to see whether the watch face is visible or not.
        // If the watch face is visible, it looks to see if the BroadcastReceiver that you defined
        // at the top of the Engine is registered. If it isn't, the method creates an IntentFilter
        // for the ACTION_TIMEZONE_CHANGED action and registers the BroadcastReceiver to listen for it.

        // If the watch face is not visible, this method will check to see if the BroadcastReceiver
        // can be unregistered. Once the BroadcastReceiver has been handled, updateTimer is called
        // to trigger invalidating the watch face and redraw the watch face.
        // ***********************************************************************************
        @Override
        public void onVisibilityChanged( boolean visible ) {
            super.onVisibilityChanged(visible);

            if( visible ) {
                if( !mHasTimeZoneReceiverBeenRegistered ) {

                    IntentFilter filter = new IntentFilter( Intent.ACTION_TIMEZONE_CHANGED );
                    MyWatchFaceService.this.registerReceiver( mTimeZoneBroadcastReceiver, filter );

                    mHasTimeZoneReceiverBeenRegistered = true;
                }

                mDisplayTime.clear( TimeZone.getDefault().getID() );
                mDisplayTime.setToNow();
            } else {
                if( mHasTimeZoneReceiverBeenRegistered ) {
                    MyWatchFaceService.this.unregisterReceiver( mTimeZoneBroadcastReceiver );
                    mHasTimeZoneReceiverBeenRegistered = false;
                }
            }

            updateTimer();
        }

        // ***********************************************************************************
        // updateTimer stops any Handler actions that are pending and checks to see if another
        // should be sent.
        // ***********************************************************************************
        private void updateTimer() {
            mTimeHandler.removeMessages( MSG_UPDATE_TIME_ID );
            if( isVisible() && !isInAmbientMode() ) {
                mTimeHandler.sendEmptyMessage( MSG_UPDATE_TIME_ID );
            }
        }

        // ***********************************************************************************
        // When your service is associated with Android Wear, onApplyWindowInsets is called. This
        // is used to determine if the device your watch face is running on is rounded or squared.
        // This lets you change your watch face to match up with the hardware.

        // When this method is called in the sample application, this method simply checks the
        // device shape and changes the x offset used for drawing the watch face to make sure
        // your watch face is visible on the device.
        // ***********************************************************************************
        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mYOffset = getResources().getDimension( R.dimen.y_offset );

            if( insets.isRound() ) {
                mXOffset = getResources().getDimension( R.dimen.x_offset_round );
            } else {
                mXOffset = getResources().getDimension( R.dimen.x_offset_square );
            }
        }

        // ***********************************************************************************
        // This method is called when the hardware properties for the Wear device are determined,
        // for example, if the device supports burn-in protection or low bit ambient mode.

        // In this method, you check if those attributes apply to the device running your watch
        // face and save them in a member variable defined at the top of your Engine.
        // ***********************************************************************************
        @Override
        public void onPropertiesChanged( Bundle properties ) {
            super.onPropertiesChanged( properties );

            if( properties.getBoolean( PROPERTY_BURN_IN_PROTECTION, false ) ) {
                mIsLowBitAmbient = properties.getBoolean( PROPERTY_LOW_BIT_AMBIENT, false );
            }
        }

        // ***********************************************************************************
        // If the device is in ambient mode, you will want to change the color of your watch face
        // to be black and white to be mindful of the user's battery. When the device is returning
        // from ambient mode, you can reset your watch face's colors. You will also want to be
        // mindful of anti-aliasing for devices that request low bit ambient support. After all of
        // the flag variables are set, you can cause the watch face to invalidate and redraw, and
        // then check if the one second timer should start.
        // ***********************************************************************************
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if( inAmbientMode ) {
                mTextColorPaint.setColor( Color.parseColor( "white" ) );
            } else {
                mTextColorPaint.setColor( Color.parseColor( "red" ) );
            }

            if( mIsLowBitAmbient ) {
                mTextColorPaint.setAntiAlias( !inAmbientMode );
            }

            invalidate();
            updateTimer();
        }

        // ***********************************************************************************
        // onInterruptionFilterChanged is called when the user manually changes the interruption
        // settings on their wearable. When this happens, you will need to check if the device is
        // muted and then alter the user interface accordingly. In this situation, you will change
        // the transparency of your watch face, set your Handler to only update every minute if the
        // device is muted, and then redraw your watch face.
        // ***********************************************************************************
        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean isDeviceMuted = ( interruptionFilter == android.support.wearable.watchface.WatchFaceService.INTERRUPTION_FILTER_NONE );
            if( isDeviceMuted ) {
                mUpdateRateMs = TimeUnit.MINUTES.toMillis( 1 );
            } else {
                mUpdateRateMs = DEFAULT_UPDATE_RATE_MS;
            }

            if( mIsInMuteMode != isDeviceMuted ) {
                mIsInMuteMode = isDeviceMuted;
                int alpha = ( isDeviceMuted ) ? 100 : 255;
                mTextColorPaint.setAlpha( alpha );
                invalidate();
            }
            updateTimer();
        }

        // ***********************************************************************************
        // When your device is in ambient mode, the Handler timer will be disabled. Your watch face
        // can still update with the current time every minute through using the built-in onTimeTick
        // method to invalidate the Canvas.
        // ***********************************************************************************
        @Override
        public void onTimeTick() {
            super.onTimeTick();

            invalidate();
        }

        // ***********************************************************************************
        // Once all of your contingencies are covered, it's time to finally draw out your watch face.
        // CanvasWatchFaceService uses a standard Canvas object, so you will need to add onDraw to
        // your Engine and manually draw out your watch face.

        // In this tutorial we're simply going to draw a text representation of the time, though
        // you could change your onDraw to easily support an analog watch face. In this method,
        // you will want to verify that you are displaying the correct time by updating your Time
        // object and then you can start applying your watch face.
        // ***********************************************************************************
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            mDisplayTime.setToNow();

            drawBackground( canvas, bounds );
            drawTimeText( canvas );
        }

        //drawBackground applies a solid color to the background of the Wear device.
        private void drawBackground( Canvas canvas, Rect bounds ) {
            canvas.drawRect( 0, 0, bounds.width(), bounds.height(), mBackgroundColorPaint );
        }

        //drawTimeText, however, creates the time text that will be displayed with the help of a
        // couple helper methods and then applies it to the canvas at the x and y offset points that
        // you defined in onApplyWindowInsets.
        private void drawTimeText( Canvas canvas ) {
            String timeText = getHourString() + ":" + String.format( "%02d", mDisplayTime.minute );
            if( isInAmbientMode() || mIsInMuteMode ) {
                timeText += ( mDisplayTime.hour < 12 ) ? "AM" : "PM";
            } else {
                timeText += String.format( ":%02d", mDisplayTime.second);
            }
            canvas.drawText( timeText, mXOffset, mYOffset, mTextColorPaint );
        }

        private String getHourString() {
            if( mDisplayTime.hour % 12 == 0 )
                return "12";
            else if( mDisplayTime.hour <= 12 )
                return String.valueOf( mDisplayTime.hour );
            else
                return String.valueOf( mDisplayTime.hour - 12 );
        }

    }
}
