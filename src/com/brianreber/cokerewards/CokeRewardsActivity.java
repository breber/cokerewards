package com.brianreber.cokerewards;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Login/Main activity for the app
 * 
 * @author breber
 */
public class CokeRewardsActivity extends Activity {
	/**
	 * SharedPreference name
	 */
	public static final String COKE_REWARDS = "CokeRewards";

	/**
	 * Email address preference key
	 */
	public static final String EMAIL_ADDRESS = "emailAddress";

	/**
	 * Password preference key
	 */
	public static final String PASSWORD = "password";

	/**
	 * Logged in state preference key
	 */
	public static final String LOGGED_IN = "loggedIn";

	/**
	 * Point count preference key
	 */
	public static final String POINTS = "points";

	/**
	 * Screen name preference key
	 */
	public static final String SCREEN_NAME = "screenName";

	/**
	 * URL to POST requests to
	 */
	private static final String URL = "http://www.mycokerewards.com/xmlrpc";

	/**
	 * Handler to use for the threads
	 */
	private Handler handler = new Handler();

	/**
	 * The request response
	 */
	private String mResult;

	/**
	 * A Runnable that will update the UI with values stored
	 * in SharedPreferences
	 */
	private Runnable updateUIRunnable = new Runnable() {
		@Override
		public void run() {
			SharedPreferences prefs = getSharedPreferences(COKE_REWARDS, Context.MODE_WORLD_READABLE);

			TextView tv = (TextView) findViewById(R.id.numPoints);

			if (isLoggedIn() && tv == null) {
				setContentView(R.layout.main);
				tv = (TextView) findViewById(R.id.numPoints);
			}


			tv.setText("Number of Points: " + prefs.getString(POINTS, ""));

			tv = (TextView) findViewById(R.id.screenName);
			String name = "".equals(prefs.getString(SCREEN_NAME, "")) ?
					prefs.getString(EMAIL_ADDRESS, "") : prefs.getString(SCREEN_NAME, "");
					tv.setText("Welcome " + name + "!");
		}
	};


	/**
	 * Set up the basic UI elements
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (isLoggedIn()) {
			setContentView(R.layout.main);

			getNumberOfPoints();

			Button submitCode = (Button) findViewById(R.id.submitCode);
			submitCode.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								EditText tv = (EditText) findViewById(R.id.code);
								String code = tv.getText().toString();

								code = code.replace(" ", "");
								code = code.toUpperCase();
								getData(CokeRewardsRequest.createCodeRequestBody(CokeRewardsActivity.this, code), updateUIRunnable);

								tv.setText("");
								getNumberOfPoints();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
			});

		} else {
			setContentView(R.layout.login);

			Button login = (Button) findViewById(R.id.performLogin);
			login.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getNumberOfPoints();
				}
			});
		}
	}

	/**
	 * Get the number of points from the MyCokeRewards server
	 */
	private void getNumberOfPoints() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					getData(CokeRewardsRequest.createLoginRequestBody(CokeRewardsActivity.this), updateUIRunnable);
				} catch (Exception e) {
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
		SharedPreferences prefs = getSharedPreferences(COKE_REWARDS, Context.MODE_WORLD_READABLE);
		return prefs.contains(EMAIL_ADDRESS) && prefs.contains(PASSWORD) && prefs.contains(LOGGED_IN);
	}

	/**
	 * Make a server request and update preferences accordingly.
	 * 
	 * @param postValue The value to POST to the server
	 * @param mRunnable The Runnable to start after we are done processing
	 * @throws IOException
	 * @throws XPathExpressionException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	private void getData(String postValue, Runnable mRunnable) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(URL);

		StringEntity se = new StringEntity(postValue);
		se.setContentType("text/xml");
		httppost.setEntity(se);

		HttpResponse response = httpclient.execute(httppost);

		InputStream input = response.getEntity().getContent();

		mResult = readStreamAsString(input);

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setNamespaceAware(true);
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(mResult)));
		XPath xpath = XPathFactory.newInstance().newXPath();

		SharedPreferences prefs = getSharedPreferences(COKE_REWARDS, Context.MODE_WORLD_WRITEABLE);
		Editor edit = prefs.edit();

		NodeList nodes = (NodeList) xpath.evaluate("/methodResponse//member/value[../name/text()='POINTS']", document, XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			edit.putString(POINTS, n.getTextContent());
		}

		nodes = (NodeList) xpath.evaluate("/methodResponse//member/value[../name/text()='LOGIN_RESULT']", document, XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			edit.putBoolean(LOGGED_IN, Boolean.parseBoolean(n.getTextContent()));
		}

		nodes = (NodeList) xpath.evaluate("/methodResponse//member/value[../name/text()='SCREEN_NAME']", document, XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			edit.putString(SCREEN_NAME, n.getTextContent());
		}

		edit.commit();

		handler.post(mRunnable);

		Log.d("RESULT", "Result: " + mResult);
	}

	/**
	 * Reads an entire input stream as a String. Closes the input stream.
	 */
	public static String readStreamAsString(InputStream in) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
			byte[] buffer = new byte[1024];
			int count;
			do {
				count = in.read(buffer);
				if (count > 0) {
					out.write(buffer, 0, count);
				}
			} while (count >= 0);
			return out.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("The JVM does not support the compiler's default encoding.", e);
		} catch (IOException e) {
			return null;
		} finally {
			try {
				in.close();
			} catch (IOException ignored) {	}
		}
	}
}