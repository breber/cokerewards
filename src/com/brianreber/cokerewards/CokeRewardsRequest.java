package com.brianreber.cokerewards;

import java.io.StringWriter;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Xml;

/**
 * Build requests that should be sent in POST requests
 * to the MyCokeRewards website.
 * 
 * @author breber
 */
public class CokeRewardsRequest {

	/**
	 * Create a basic retrieve points balance request
	 * 
	 * @param ctx with which we can retrieve preferences
	 * @return a POST body
	 */
	public static String createLoginRequestBody(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(CokeRewardsActivity.COKE_REWARDS, Context.MODE_WORLD_READABLE);

		String emailAddress = prefs.getString(CokeRewardsActivity.EMAIL_ADDRESS, "");
		String password = prefs.getString(CokeRewardsActivity.PASSWORD, "");
		String screenName = prefs.getString(CokeRewardsActivity.SCREEN_NAME, "");

		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", "methodCall");
			serializer.startTag("", "methodName");
			serializer.text("points.pointsBalance");
			serializer.endTag("", "methodName");

			serializer.startTag("", "params");
			serializer.startTag("", "param");
			serializer.startTag("", "value");
			serializer.startTag("", "struct");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("emailAddress");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text(emailAddress);
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("password");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text(password);
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("screenName");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text(screenName);
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("VERSION");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text("4.1");
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.endTag("", "struct");
			serializer.endTag("", "value");
			serializer.endTag("", "param");
			serializer.endTag("", "params");

			serializer.endTag("", "methodCall");
			serializer.endDocument();
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a request to send a new code to the server
	 * 
	 * @param ctx with which we can retrieve preferences
	 * @return a POST body
	 */
	public static String createCodeRequestBody(Context ctx, String code) {
		SharedPreferences prefs = ctx.getSharedPreferences(CokeRewardsActivity.COKE_REWARDS, Context.MODE_WORLD_READABLE);

		String emailAddress = prefs.getString(CokeRewardsActivity.EMAIL_ADDRESS, "");
		String password = prefs.getString(CokeRewardsActivity.PASSWORD, "");
		String screenName = prefs.getString(CokeRewardsActivity.SCREEN_NAME, "");

		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", "methodCall");
			serializer.startTag("", "methodName");
			serializer.text("points.enterCode");
			serializer.endTag("", "methodName");

			serializer.startTag("", "params");
			serializer.startTag("", "param");
			serializer.startTag("", "value");
			serializer.startTag("", "struct");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("emailAddress");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text(emailAddress);
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("password");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text(password);
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("screenName");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text(screenName);
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("capCode");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text(code);
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.startTag("", "member");
			serializer.startTag("", "name");
			serializer.text("VERSION");
			serializer.endTag("", "name");
			serializer.startTag("", "value");
			serializer.startTag("", "string");
			serializer.text("4.1");
			serializer.endTag("", "string");
			serializer.endTag("", "value");
			serializer.endTag("", "member");

			serializer.endTag("", "struct");
			serializer.endTag("", "value");
			serializer.endTag("", "param");
			serializer.endTag("", "params");

			serializer.endTag("", "methodCall");
			serializer.endDocument();
			return writer.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
