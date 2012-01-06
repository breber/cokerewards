package com.brianreber.cokerewards;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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

public class CokeRewardsActivity extends Activity {
	public static final String COKE_REWARDS = "CokeRewards";

	public static final String EMAIL_ADDRESS = "emailAddress";
	public static final String PASSWORD = "password";
	public static final String LOGGED_IN = "loggedIn";
	public static final String POINTS = "points";
	public static final String SCREEN_NAME = "screenName";

	private static final String URL = "https://secure.mycokerewards.com/xmlrpc";

	private Handler handler = new Handler();

	private String mResult;

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


	/** Called when the activity is first created. */
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
								String code = ((EditText) findViewById(R.id.code)).getText().toString();
								getData(CokeRewardsRequest.createCodeRequestBody(CokeRewardsActivity.this, code), updateUIRunnable);
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

	private boolean isLoggedIn() {
		SharedPreferences prefs = getSharedPreferences(COKE_REWARDS, Context.MODE_WORLD_READABLE);
		return prefs.contains(EMAIL_ADDRESS) && prefs.contains(PASSWORD) && prefs.contains(LOGGED_IN);
	}

	private void getData(String postValue, Runnable mRunnable) throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
		java.net.URL url = new java.net.URL(URL);
		HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
		https.setHostnameVerifier(DO_NOT_VERIFY);
		https.setDoOutput(true);
		https.setDoInput(true);

		https.setRequestMethod("POST");
		https.setRequestProperty("Content-Length", postValue.getBytes().length + "");

		OutputStream output = https.getOutputStream();
		output.write(postValue.getBytes());
		output.close();

		InputStream input = https.getInputStream();

		mResult = readStreamAsString(input);

		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		builderFactory.setNamespaceAware(true);
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(mResult)));
		XPath xpath = XPathFactory.newInstance().newXPath();

		SharedPreferences prefs = getSharedPreferences(COKE_REWARDS, Context.MODE_WORLD_READABLE);
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


	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};
}