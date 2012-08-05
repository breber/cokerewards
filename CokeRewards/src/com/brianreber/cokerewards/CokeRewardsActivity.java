package com.brianreber.cokerewards;

import java.util.Map;

import net.robotmedia.billing.BillingController;
import net.robotmedia.billing.BillingRequest.ResponseCode;
import net.robotmedia.billing.helper.AbstractBillingObserver;
import net.robotmedia.billing.model.Transaction.PurchaseState;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdView;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * Main activity for the app
 * 
 * @author breber
 */
public class CokeRewardsActivity extends Activity {

	/**
	 * Request code for starting our RegisterActivity
	 */
	private static final int REGISTER_REQUEST_CODE = Math.abs(RegisterActivity.class.hashCode());

	/**
	 * LogCat tag
	 */
	protected static final String TAG = CokeRewardsActivity.class.getSimpleName();

	/**
	 * Handler to use for the threads
	 */
	private static Handler handler = new Handler();

	/**
	 * Google Analytics tracker
	 */
	private GoogleAnalyticsTracker tracker;

	/**
	 * Dialog to display that we are submitting a code
	 */
	private ProgressDialog dlg;

	/**
	 * The Observer that listens for information about in-app billing
	 */
	private AbstractBillingObserver mBillingObserver;

	/**
	 * Represents whether the device supports in-app billing
	 */
	private boolean supportBilling = true;

	/**
	 * Represents whether the user has purchased the app or not
	 */
	private boolean isRegistered = false;

	/**
	 * A Runnable that will update the UI with values stored
	 * in SharedPreferences
	 */
	private Runnable updateUIRunnable = new Runnable() {
		@Override
		public void run() {
			SharedPreferences prefs = getSharedPreferences(Constants.PREFS_COKE_REWARDS, 0);

			if (dlg != null && dlg.isShowing()) {
				dlg.dismiss();
			}

			TextView tv = (TextView) findViewById(R.id.numPoints);
			tv.setText("Number of Points: " + prefs.getInt(Constants.POINTS, 0));

			tv = (TextView) findViewById(R.id.screenName);
			String name = ("".equals(prefs.getString(Constants.SCREEN_NAME, "")) ?
					prefs.getString(Constants.EMAIL_ADDRESS, "") : prefs.getString(Constants.SCREEN_NAME, ""));

			tv.setText("Welcome " + name + "!");
		}
	};

	/**
	 * A Runnable that will update the UI with values stored
	 * in SharedPreferences
	 */
	private Runnable codeUpdateRunnable = new Runnable() {
		@Override
		public void run() {
			SharedPreferences prefs = getSharedPreferences(Constants.PREFS_COKE_REWARDS, 0);

			if (dlg != null && dlg.isShowing()) {
				dlg.dismiss();
			}

			if (prefs.getBoolean(Constants.ENTER_CODE_RESULT, false)) {
				EditText tv = (EditText) findViewById(R.id.code);
				tv.setText("");

				int numPointsEarned = prefs.getInt(Constants.POINTS_EARNED_RESULT, 0);
				Toast.makeText(getApplicationContext(), "Code submitted successfully. " + numPointsEarned + " points earned!", Toast.LENGTH_SHORT).show();

				try {
					tracker.trackEvent("SuccessfulCode", "SuccessfulCode", "SuccessfulCodeSubmission", 0);
				} catch (Exception e) { }
			} else {
				String messages = prefs.getString(Constants.MESSAGES, "");
				Toast.makeText(getApplicationContext(), "Code submission failed..." + messages, Toast.LENGTH_SHORT).show();

				try {
					tracker.trackEvent("FailedCode", "FailedCode", "Code submission failed: " + messages, 0);
				} catch (Exception e) { }
			}

			getNumberOfPoints();
		}
	};

	/**
	 * A Runnable that will show an error Toast
	 */
	private Runnable errorRunnable = new Runnable() {
		@Override
		public void run() {
			if (dlg != null && dlg.isShowing()) {
				dlg.dismiss();
			}

			Toast.makeText(CokeRewardsActivity.this, "An error has occurred. Please try again.", Toast.LENGTH_SHORT).show();
		}
	};


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Set up all the in-app billing stuff
		setupInAppBilling();

		// Start the tracker in manual dispatch mode...
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.startNewSession("UA-3673402-16", 20, this);
		tracker.setAnonymizeIp(true);

		// If we are logged in, refresh the point count, and show an ad if the user
		// hasn't purchased the app
		if (isLoggedIn()) {
			getNumberOfPoints();
			handler.post(updateUIRunnable);
		} else {
			// If the user isn't logged in, bring them to the register activity
			Intent register = new Intent(this, RegisterActivity.class);
			startActivityForResult(register, REGISTER_REQUEST_CODE);
		}

		// Set up the submitting dialog box so it is ready when we need it
		dlg = new ProgressDialog(this);
		dlg.setCancelable(false);
		dlg.setMessage(getResources().getText(R.string.submitting));

		Button submitCode = (Button) findViewById(R.id.submitCode);
		submitCode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText tv = (EditText) findViewById(R.id.code);
				String code = tv.getText().toString();

				code = code.replace(" ", "");
				final String finalCode = code.toUpperCase();

				if (finalCode.length() < 10) {
					Toast.makeText(CokeRewardsActivity.this, "Code not long enough", Toast.LENGTH_SHORT).show();
					return;
				}

				dlg.show();

				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							tracker.trackEvent("Button", "SubmitCode", "Submit Code button clicked", 0);

							Map<String, Object> result = CokeRewardsRequest.createCodeRequestBody(CokeRewardsActivity.this, finalCode);
							parseResult(CokeRewardsActivity.this, codeUpdateRunnable, result);
						} catch (Exception e) {
							try {
								tracker.trackEvent("Exception", "ExceptionSubmitCode", "Submit Code encountered an exception: " + e.getMessage(), 0);
							} catch (Exception ex) { }

							handler.post(errorRunnable);
							e.printStackTrace();
						}
					}
				}).start();
			}
		});

		Button visitWebsite = (Button) findViewById(R.id.showWebsite);
		visitWebsite.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse("http://m.mycokerewards.com/"));
				startActivityForResult(i, REGISTER_REQUEST_CODE);
			}
		});
	}

	/**
	 * Set up all the in-app billing stuff
	 */
	private void setupInAppBilling() {
		BillingController.setDebug(true);
		BillingController.setConfiguration(new Security());

		mBillingObserver = new AbstractBillingObserver(this) {
			@Override
			public void onBillingChecked(boolean supported) {
				if (supported) {
					restoreTransactions();
				}

				supportBilling = supported;
			}

			@Override
			public void onPurchaseStateChanged(String itemId, PurchaseState state) {
				Log.i(TAG, "onPurchaseStateChanged() itemId: " + itemId + " --> " + state);
				isRegistered = BillingController.isPurchased(CokeRewardsActivity.this, Constants.PRODUCT_ID);

				hideAds();
			}

			@Override
			public void onRequestPurchaseResponse(String itemId, ResponseCode response) {

			}

			@Override
			public void onSubscriptionChecked(boolean supported) {

			}
		};

		BillingController.registerObserver(mBillingObserver);
		BillingController.checkBillingSupported(this);

		isRegistered = BillingController.isPurchased(CokeRewardsActivity.this, Constants.PRODUCT_ID);
		hideAds();
	}

	/**
	 * Restores previous transactions, if any. This happens if the application
	 * has just been installed or the user wiped data. We do not want to do this
	 * on every startup, rather, we want to do only when the database needs to
	 * be initialized.
	 */
	private void restoreTransactions() {
		if (!mBillingObserver.isTransactionsRestored()) {
			BillingController.restoreTransactions(this);
		}
	}

	/**
	 * If we are registered, hide the ads
	 */
	private void hideAds() {
		if (isRegistered) {
			AdView adView = (AdView) findViewById(R.id.adView);

			if (adView != null) {
				adView.setVisibility(View.GONE);
				adView.destroy();
			}
		}
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		try {
			// Stop the tracker when it is no longer needed.
			tracker.stopSession();
		} catch (Exception e) { }

		BillingController.unregisterObserver(mBillingObserver);

		super.onDestroy();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();

		if (isLoggedIn()) {
			getNumberOfPoints();
		}

		hideAds();
	}

	/**
	 * Get the number of points from the MyCokeRewards server
	 */
	private void getNumberOfPoints() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					tracker.trackEvent("CountFetch", "CountFetch", "Fetched number of points", 0);

					Map<String, Object> result = CokeRewardsRequest.createLoginRequestBody(CokeRewardsActivity.this);
					parseResult(CokeRewardsActivity.this, updateUIRunnable, result);
				} catch (Exception e) {
					try {
						tracker.trackEvent("Exception", "ExceptionGetNumPoints", "Get number of points encountered an exception: " + e.getMessage(), 0);
					} catch (Exception ex) { }

					handler.post(errorRunnable);
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * Do we have a logged in user?
	 * 
	 * @return whether we have a valid email address, password, and have successfully
	 * logged in with the most recent request
	 */
	private boolean isLoggedIn() {
		return isLoggedIn(this);
	}

	/**
	 * Do we have a logged in user?
	 * 
	 * @return whether we have a valid email address, password, and have successfully
	 * logged in with the most recent request
	 */
	public static boolean isLoggedIn(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(Constants.PREFS_COKE_REWARDS, 0);
		return prefs.contains(Constants.EMAIL_ADDRESS) &&
				prefs.contains(Constants.PASSWORD) &&
				prefs.getBoolean(Constants.LOGGED_IN, false);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int menuId = item.getItemId();

		if (menuId == R.id.logout) {
			SharedPreferences prefs = getSharedPreferences(Constants.PREFS_COKE_REWARDS, 0);
			Editor edit = prefs.edit();

			edit.clear();
			edit.commit();

			Intent register = new Intent(this, RegisterActivity.class);
			startActivityForResult(register, REGISTER_REQUEST_CODE);
		} else if (menuId == R.id.hideAds) {
			BillingController.requestPurchase(CokeRewardsActivity.this, Constants.PRODUCT_ID, true, null);
		}

		return super.onMenuItemSelected(featureId, item);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menus, menu);
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If billing isn't supported or the user has already
		// purchased the app, hid the menu button
		if (!supportBilling || isRegistered) {
			menu.removeItem(R.id.hideAds);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (isLoggedIn()) {
			getNumberOfPoints();
		} else {
			finish();
		}
	}

	/**
	 * Make a server request and update preferences accordingly.
	 * 
	 * @param ctx The application context
	 * @param runnable The Runnable to start after we are done processing
	 * @param data The data received from the server
	 */
	public static void parseResult(Context ctx, Runnable runnable, Map<String, Object> data) {
		final String RESULT_POINTS        = "POINTS";
		final String RESULT_LOGIN         = "LOGIN_RESULT";
		final String RESULT_SCREEN_NAME   = "SCREEN_NAME";
		final String RESULT_ENTER_CODE    = "ENTER_CODE_RESULT";
		final String RESULT_POINTS_EARNED = "POINTS_EARNED";
		final String RESULT_MESSAGES      = "MESSAGES";

		SharedPreferences prefs = ctx.getSharedPreferences(Constants.PREFS_COKE_REWARDS, 0);
		Editor edit = prefs.edit();

		if (data.containsKey(RESULT_POINTS)) {
			edit.putInt(Constants.POINTS, (Integer) data.get(RESULT_POINTS));
		}

		if (data.containsKey(RESULT_LOGIN)) {
			edit.putBoolean(Constants.LOGGED_IN, Boolean.parseBoolean((String) data.get(RESULT_LOGIN)));
		}

		if (data.containsKey(RESULT_SCREEN_NAME)) {
			String screenName = (String) data.get(RESULT_SCREEN_NAME);

			if (screenName != null && !"".equals(screenName)) {
				edit.putString(Constants.SCREEN_NAME, screenName);
			}
		}

		if (data.containsKey(RESULT_ENTER_CODE)) {
			edit.putBoolean(Constants.ENTER_CODE_RESULT, Boolean.parseBoolean((String) data.get(RESULT_ENTER_CODE)));
		}

		if (data.containsKey(RESULT_POINTS_EARNED)) {
			edit.putInt(Constants.POINTS_EARNED_RESULT, (Integer) data.get(RESULT_POINTS_EARNED));
		}

		if (data.containsKey(RESULT_MESSAGES)) {
			String messages = (String) ((Object[]) data.get(RESULT_MESSAGES))[0];
			edit.putString(Constants.MESSAGES, messages);
		}

		edit.commit();

		handler.post(runnable);
	}
}