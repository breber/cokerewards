package com.brianreber.cokerewards;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;

/**
 * Build requests that should be sent in POST requests
 * to the MyCokeRewards website.
 * 
 * @author breber
 */
public class CokeRewardsRequest {

	/**
	 * Secure URL to POST requests to
	 */
	private static final String SECURE_URL = "https://www.mycokerewards.com/xmlrpc";

	/**
	 * Create a basic retrieve points balance request
	 * 
	 * @param ctx with which we can retrieve preferences
	 * @return a POST body
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> createLoginRequestBody(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(CokeRewardsActivity.COKE_REWARDS, 0);

		String emailAddress = prefs.getString(CokeRewardsActivity.EMAIL_ADDRESS, "");
		String password = prefs.getString(CokeRewardsActivity.PASSWORD, "");
		String screenName = prefs.getString(CokeRewardsActivity.SCREEN_NAME, "");

		try {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("emailAddress", emailAddress);
			data.put("password", password);
			data.put("screenName", screenName);
			data.put("VERSION", "4.1");

			XMLRPCClient client = new XMLRPCClient(new URL(SECURE_URL), XMLRPCClient.FLAGS_APACHE_WS);
			Object[] temp = (Object[]) client.call("points.pointsBalance", data);
			if (temp != null) {
				return (Map<String, Object>) temp[0];
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}

		return new HashMap<String, Object>();
	}

	/**
	 * Create a request to send a new code to the server
	 * 
	 * @param ctx with which we can retrieve preferences
	 * @return a POST body
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> createCodeRequestBody(Context ctx, String code) {
		SharedPreferences prefs = ctx.getSharedPreferences(CokeRewardsActivity.COKE_REWARDS, 0);

		String emailAddress = prefs.getString(CokeRewardsActivity.EMAIL_ADDRESS, "");
		String password = prefs.getString(CokeRewardsActivity.PASSWORD, "");
		String screenName = prefs.getString(CokeRewardsActivity.SCREEN_NAME, "");

		try {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("emailAddress", emailAddress);
			data.put("password", password);
			data.put("screenName", screenName);
			data.put("capCode", code);
			data.put("VERSION", "4.1");

			XMLRPCClient client = new XMLRPCClient(new URL(SECURE_URL), XMLRPCClient.FLAGS_APACHE_WS);
			Object[] temp = (Object[]) client.call("points.enterCode", data);
			if (temp != null) {
				return (Map<String, Object>) temp[0];
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}

		return new HashMap<String, Object>();
	}

}
