/*
 * Copyright (C) 2008 ZXing authors
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brianreber.cokerewards.ocr;


import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.brianreber.cokerewards.R;
import com.brianreber.cokerewards.ocr.camera.CameraManager;
import com.brianreber.cokerewards.ocr.camera.ShutterButton;
import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback,
ShutterButton.OnShutterButtonListener {

	private static final String TAG = CaptureActivity.class.getSimpleName();

	// Note: These constants will be overridden by any default values defined in preferences.xml.
	/** The default page segmentation mode to use. */
	public static final String DEFAULT_PAGE_SEGMENTATION_MODE = "Auto";

	/** Resource to use for data file downloads. */
	static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";

	/** Download filename for orientation and script detection (OSD) data. */
	static final String OSD_FILENAME = "tesseract-ocr-3.01.osd.tar";

	/** Destination filename for orientation and script detection (OSD) data. */
	static final String OSD_FILENAME_BASE = "osd.traineddata";

	private CameraManager cameraManager;
	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private TextView statusViewBottom;
	private TextView statusViewTop;
	private View cameraButtonView;
	private OcrResult lastResult;
	private boolean hasSurface;
	private TessBaseAPI baseApi; // Java interface for the Tesseract OCR engine
	private String sourceLanguageCodeOcr = "eng"; // ISO 639-3 language code
	private String sourceLanguageReadable = "English"; // Language name, for example, "English"
	private int pageSegmentationMode = TessBaseAPI.PSM_AUTO;
	private ShutterButton shutterButton;
	//  private ToggleButton torchButton;
	private ProgressDialog dialog; // for initOcr - language download & unzip
	private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
	private boolean isEngineReady;

	Handler getHandler() {
		return handler;
	}

	TessBaseAPI getBaseApi() {
		return baseApi;
	}

	CameraManager getCameraManager() {
		return cameraManager;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.capture);
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		cameraButtonView = findViewById(R.id.camera_button_view);

		statusViewBottom = (TextView) findViewById(R.id.status_view_bottom);
		registerForContextMenu(statusViewBottom);
		statusViewTop = (TextView) findViewById(R.id.status_view_top);
		registerForContextMenu(statusViewTop);

		handler = null;
		lastResult = null;
		hasSurface = false;

		// Camera shutter button
		shutterButton = (ShutterButton) findViewById(R.id.shutter_button);
		shutterButton.setOnShutterButtonListener(this);

		cameraManager = new CameraManager(getApplication());
		viewfinderView.setCameraManager(cameraManager);

		// Set listener to change the size of the viewfinder rectangle.
		viewfinderView.setOnTouchListener(new View.OnTouchListener() {
			int lastX = -1;
			int lastY = -1;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastX = -1;
					lastY = -1;
					return true;
				case MotionEvent.ACTION_MOVE:
					int currentX = (int) event.getX();
					int currentY = (int) event.getY();

					try {
						Rect rect = cameraManager.getFramingRect();

						final int BUFFER = 50;
						final int BIG_BUFFER = 60;
						if (lastX >= 0) {
							// Adjust the size of the viewfinder rectangle. Check if the touch event occurs in the corner areas first, because the regions overlap.
							if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
									&& ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
								// Top left corner: adjust both top and left sides
								cameraManager.adjustFramingRect( 2 * (lastX - currentX), 2 * (lastY - currentY));
								viewfinderView.removeResultText();
							} else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
									&& ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
								// Top right corner: adjust both top and right sides
								cameraManager.adjustFramingRect( 2 * (currentX - lastX), 2 * (lastY - currentY));
								viewfinderView.removeResultText();
							} else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
									&& ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
								// Bottom left corner: adjust both bottom and left sides
								cameraManager.adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
								viewfinderView.removeResultText();
							} else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
									&& ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
								// Bottom right corner: adjust both bottom and right sides
								cameraManager.adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
								viewfinderView.removeResultText();
							} else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER))
									&& ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
								// Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
								cameraManager.adjustFramingRect(2 * (lastX - currentX), 0);
								viewfinderView.removeResultText();
							} else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER))
									&& ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
								// Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
								cameraManager.adjustFramingRect(2 * (currentX - lastX), 0);
								viewfinderView.removeResultText();
							} else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER))
									&& ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
								// Adjusting top side: event falls within BUFFER pixels of top side, and between left and right side limits
								cameraManager.adjustFramingRect(0, 2 * (lastY - currentY));
								viewfinderView.removeResultText();
							} else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER))
									&& ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
								// Adjusting bottom side: event falls within BUFFER pixels of bottom side, and between left and right side limits
								cameraManager.adjustFramingRect(0, 2 * (currentY - lastY));
								viewfinderView.removeResultText();
							}
						}
					} catch (NullPointerException e) {
						Log.e(TAG, "Framing rect not available", e);
					}
					v.invalidate();
					lastX = currentX;
					lastY = currentY;
					return true;
				case MotionEvent.ACTION_UP:
					lastX = -1;
					lastY = -1;
					return true;
				}
				return false;
			}
		});

		isEngineReady = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		resetStatusView();

		// Set up the camera preview surface.
		surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		surfaceHolder = surfaceView.getHolder();
		if (!hasSurface) {
			surfaceHolder.addCallback(this);
		}

		// Comment out the following block to test non-OCR functions without an SD card

		// Do OCR engine initialization, if necessary
		if (baseApi == null) {
			// Initialize the OCR engine
			File storageDirectory = getStorageDirectory();
			if (storageDirectory != null) {
				initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
			}
		} else {
			// We already have the engine initialized, so just start the camera.
			resumeOCR();
		}
	}

	/**
	 * Method to start or restart recognition after the OCR engine has been initialized,
	 * or after the app regains focus. Sets state related settings and OCR engine parameters,
	 * and requests camera initialization.
	 */
	void resumeOCR() {
		Log.d(TAG, "resumeOCR()");

		// This method is called when Tesseract has already been successfully initialized, so set
		// isEngineReady = true here.
		isEngineReady = true;
		if (baseApi != null) {
			baseApi.setPageSegMode(pageSegmentationMode);
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, OcrCharacterHelper.getBlacklist());
			baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, OcrCharacterHelper.getWhitelist());
		}

		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated()");

		if (holder == null) {
			Log.e(TAG, "surfaceCreated gave us a null surface");
		}

		// Only initialize the camera if the OCR engine is ready to go.
		if (!hasSurface && isEngineReady) {
			Log.d(TAG, "surfaceCreated(): calling initCamera()...");
			initCamera(holder);
		}
		hasSurface = true;
	}

	/** Initializes the camera and starts the handler to begin previewing. */
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
			showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
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
		if (baseApi != null) {
			baseApi.end();
		}
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Exit the app if we're not viewing an OCR result.
			if (lastResult == null) {
				setResult(RESULT_CANCELED);
				finish();
				return true;
			} else {
				// Go back to previewing in regular OCR mode.
				resetStatusView();
				if (handler != null) {
					handler.sendEmptyMessage(R.id.restart_preview);
				}
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			handler.hardwareShutterButtonClick();
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

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	/** Finds the proper location on the SD card where we can save files. */
	private File getStorageDirectory() {
		String state = null;
		try {
			state = Environment.getExternalStorageState();
		} catch (RuntimeException e) {
			Log.e(TAG, "Is the SD card visible?", e);
			showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
		}

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			try {
				return getExternalFilesDir(Environment.MEDIA_MOUNTED);
			} catch (NullPointerException e) {
				// We get an error here if the SD card is visible, but full
				Log.e(TAG, "External storage is unavailable");
				showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
			}
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			Log.e(TAG, "External storage is read-only");
			showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			// to know is we can neither read nor write
			Log.e(TAG, "External storage is unavailable");
			showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
		}
		return null;
	}

	/**
	 * Requests initialization of the OCR engine with the given parameters.
	 * 
	 * @param storageRoot Path to location of the tessdata directory to use
	 * @param languageCode Three-letter ISO 639-3 language code for OCR
	 * @param languageName Name of the language for OCR, for example, "English"
	 */
	private void initOcrEngine(File storageRoot, String languageCode, String languageName) {
		isEngineReady = false;

		// Set up the dialog box for the thermometer-style download progress indicator
		if (dialog != null) {
			dialog.dismiss();
		}
		dialog = new ProgressDialog(this);

		// Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
		indeterminateDialog = new ProgressDialog(this);
		indeterminateDialog.setTitle("Please wait");
		indeterminateDialog.setMessage("Initializing...");
		indeterminateDialog.setCancelable(false);
		indeterminateDialog.show();

		if (handler != null) {
			handler.quitSynchronously();
		}

		// Start AsyncTask to install language data and init OCR
		baseApi = new TessBaseAPI();
		new OcrInitAsyncTask(this, baseApi, dialog, indeterminateDialog).execute(storageRoot.toString());
	}

	/**
	 * Displays information relating to the result of OCR, and requests a translation if necessary.
	 * 
	 * @param ocrResult Object representing successful OCR results
	 * @return True if a non-null result was received for OCR
	 */
	boolean handleOcrDecode(OcrResult ocrResult) {
		lastResult = ocrResult;

		// Test whether the result is null
		if (ocrResult.getText() == null || ocrResult.getText().equals("")) {
			Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP, 0, 0);
			toast.show();
			return false;
		} else {
			Intent result = new Intent();
			result.putExtra("result", ocrResult.getText());
			setResult(RESULT_OK, result);
			finish();
			return true;
		}
	}

	/**
	 * Resets view elements.
	 */
	private void resetStatusView() {
		viewfinderView.setVisibility(View.VISIBLE);
		cameraButtonView.setVisibility(View.VISIBLE);
		shutterButton.setVisibility(View.VISIBLE);
		lastResult = null;
		viewfinderView.removeResultText();
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

	/** Request the viewfinder to be invalidated. */
	void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	@Override
	public void onShutterButtonClick(ShutterButton b) {
		if (handler != null) {
			handler.shutterButtonClick();
		}
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

	void displayProgressDialog() {
		// Set up the indeterminate progress dialog box
		indeterminateDialog = new ProgressDialog(this);
		indeterminateDialog.setTitle("Please wait");
		indeterminateDialog.setMessage("Performing OCR using Cube and Tesseract...");
		indeterminateDialog.setCancelable(false);
		indeterminateDialog.show();
	}

	ProgressDialog getProgressDialog() {
		return indeterminateDialog;
	}

	/**
	 * Displays an error message dialog box to the user on the UI thread.
	 * 
	 * @param title The title for the dialog box
	 * @param message The error message to be displayed
	 */
	void showErrorMessage(String title, String message) {
		new AlertDialog.Builder(this)
		.setTitle(title)
		.setMessage(message)
		.setOnCancelListener(new FinishListener(this))
		.setPositiveButton( "Done", new FinishListener(this))
		.show();
	}
}
