package com.stackbase.mobapp.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.stackbase.mobapp.R;
import com.stackbase.mobapp.camera.BeepManager;
import com.stackbase.mobapp.camera.CameraManager;
import com.stackbase.mobapp.utils.Helper;
import com.stackbase.mobapp.view.ShutterButton;

import java.io.IOException;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the text correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 * <p/>
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback,
        ShutterButton.OnShutterButtonListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    // Context menu
    private static final int SETTINGS_ID = Menu.FIRST;
    private static final int ABOUT_ID = Menu.FIRST + 1;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private View cameraButtonView;
    private boolean hasSurface;
    private BeepManager beepManager;
    private ShutterButton shutterButton;
    private SharedPreferences prefs;
    private boolean isPaused;
    private OnSharedPreferenceChangeListener listener;
    private FinishListener finishListener;

    public FinishListener getFinishListener() {
        return finishListener;
    }

    public SharedPreferences getSharedPreferences() {
        return prefs;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_capture);
        cameraButtonView = findViewById(R.id.camera_button_view);

        handler = null;
        hasSurface = false;
        beepManager = new BeepManager(this);

        // Camera shutter button
        shutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        shutterButton.setOnShutterButtonListener(this);

        cameraManager = new CameraManager(getApplication());

        finishListener = new FinishListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        resetStatusView();

        retrievePreferences();

        // Set up the camera preview surface.
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        surfaceHolder = surfaceView.getHolder();
        if (!hasSurface) {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    /**
     * Called when the shutter button is pressed in continuous mode.
     */
    void onShutterButtonPress() {
        isPaused = true;
        handler.stop();
        beepManager.playBeepSoundAndVibrate();

        // Capture picture and show confirm page
        cameraManager.takePicture(handler);


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");

        if (holder == null) {
            Log.e(TAG, "surfaceCreated gave us a null surface");
        }

        // Only initialize the camera if the OCR engine is ready to go.
        if (!hasSurface) { // && isEngineReady
            Log.d(TAG, "surfaceCreated(): calling initCamera()...");
            initCamera(holder);
        }
        hasSurface = true;
    }

    /**
     * Initializes the camera and starts the handler to begin previewing.
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "initCamera()");
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        try {

            // Open and initialize the camera
            cameraManager.openDriver(surfaceHolder);

            // Creating the handler starts the preview, which can also throw a RuntimeException.
            handler = new CaptureActivityHandler(this, cameraManager);

        } catch (IOException ioe) {
            Helper.showErrorMessage(this, "错误", "不能打开照相机设备, 请重启您的手机或检查权限设置.",
                    finishListener, finishListener);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Helper.showErrorMessage(this, "错误", "不能打开照相机设备, 请重启您的手机或检查权限设置.",
                    finishListener, finishListener);
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
        }

        // Stop using the camera, to avoid conflicting with other camera-based apps
        cameraManager.closeDriver();

        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    void stopHandler() {
        if (handler != null) {
            handler.stop();
        }
    }

    @Override
    protected void onDestroy() {
//        if (baseApi != null) {
//            baseApi.end();
//        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // First check if we're paused in continuous mode, and if so, just unpause.
            if (isPaused) {
                Log.d(TAG, "only resuming continuous recognition, not quitting...");
                resumeContinuousCapture();
                return true;
            }

            // Exit the app if we're not viewing an OCR result.
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            onShutterButtonPress();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS) {
            // Only perform autofocus if user is not holding down the button.
            if (event.getRepeatCount() == 0) {
                cameraManager.requestAutoFocus(500L);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    void resumeContinuousCapture() {
        isPaused = false;
        resetStatusView();
        handler.resetState();
        if (shutterButton != null) {
            shutterButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //    MenuInflater inflater = getMenuInflater();
        //    inflater.inflate(R.menu.options_menu, menu);
        super.onCreateOptionsMenu(menu);
        menu.add(0, SETTINGS_ID, 0, "Settings").setIcon(android.R.drawable.ic_menu_preferences);
//        menu.add(0, ABOUT_ID, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case SETTINGS_ID: {
                intent = new Intent().setClass(this, PreferencesActivity.class);
                startActivity(intent);
                break;
            }
//            case ABOUT_ID: {
//                intent = new Intent(this, HelpActivity.class);
//                intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, HelpActivity.ABOUT_PAGE);
//                startActivity(intent);
//                break;
//            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    /**
     * Resets view elements.
     */
    private void resetStatusView() {
        cameraButtonView.setVisibility(View.VISIBLE);
        shutterButton.setVisibility(View.VISIBLE);
    }

    void setButtonVisibility(boolean visible) {
        if (shutterButton != null && visible == true) {
            shutterButton.setVisibility(View.VISIBLE);
        } else if (shutterButton != null) {
            shutterButton.setVisibility(View.GONE);
        }
    }

    /**
     * Enables/disables the shutter button to prevent double-clicks on the button.
     *
     * @param clickable True if the button should accept a click
     */
    void setShutterButtonClickable(boolean clickable) {
        shutterButton.setClickable(clickable);
    }

    /**
     * Request the viewfinder to be invalidated.
     */
    void drawViewfinder() {
//     viewfinderView.drawViewfinder();
    }

    @Override
    public void onShutterButtonClick(ShutterButton b) {
        onShutterButtonPress();
    }

    @Override
    public void onShutterButtonFocus(ShutterButton b, boolean pressed) {
        requestDelayedAutoFocus();
    }

    /**
     * Requests autofocus after a 350 ms delay. This delay prevents requesting focus when the user
     * just wants to click the shutter button without focusing. Quick button press/release will
     * trigger onShutterButtonClick() before the focus kicks in.
     */
    private void requestDelayedAutoFocus() {
        // Wait 350 ms before focusing to avoid interfering with quick button presses when
        // the user just wants to take a picture without focusing.
        cameraManager.requestAutoFocus(350L);
    }

    /**
     * Gets values from shared preferences and sets the corresponding data members in this activity.
     */
    private void retrievePreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Retrieve from preferences, and set in this Activity, the language preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        prefs.registerOnSharedPreferenceChangeListener(listener);

        beepManager.updatePrefs();
    }

}

