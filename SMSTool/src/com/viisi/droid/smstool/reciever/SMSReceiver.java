package com.viisi.droid.smstool.reciever;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import com.viisi.droid.smstool.service.SMSManagerService;

public class SMSReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;
			StringBuilder originalMessage = new StringBuilder();
			String originalPhone = null;
			
			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];
				for (int i = 0; i < msgs.length; i++) {
					msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					if (i == 0) {
						originalPhone = msgs[i].getOriginatingAddress();
					}
					originalMessage.append(msgs[i].getMessageBody().toString());
					if (i != (msgs.length - 1)) {
						originalMessage.append("\n");
					}
				}
				
				final String FILENAME = "redirect_file";
				
				int ch;
				FileInputStream fis = null;
				StringBuilder strContent = new StringBuilder();
				
				try {
					fis = context.openFileInput(FILENAME);
					while ((ch = fis.read()) != -1) {
						strContent.append((char) ch);
					}
					fis.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (!TextUtils.isEmpty(strContent)) {
					if (isRedirectNumberType(strContent)) {
						sendSMSByNumberType(context, originalMessage, originalPhone, strContent);
					} else {
						sendSMSByMsgType(context, originalMessage, strContent);
					}
				}
			}
		}
	}

	private void sendSMSByMsgType(Context context, StringBuilder originalMessage, StringBuilder strContent) {
		String[] contentFileSplited = strContent.toString().split(";");
		String originalMessageFormated = originalMessage.toString().toLowerCase().replace(" ", "#");

		messageFor: for (String wordFromMessage : originalMessageFormated.split("#")) {
			for (String wordFromPattern : contentFileSplited[0].toLowerCase().trim().split(",")) {
				if (wordFromMessage.contentEquals(wordFromPattern)) {

					Boolean saveOutbox = new Boolean(contentFileSplited[2]);
					String numbersTo = contentFileSplited[1];
					sendSMS(numbersTo, originalMessage.toString(), context, saveOutbox);

					break messageFor;
				}
			}
		}
	}

	private void sendSMSByNumberType(Context context, StringBuilder originalMessage, String originalPhone, StringBuilder strContent) {
		String[] phoneFromAndTo = strContent.toString().split(";");
		if (phoneFromAndTo != null && !TextUtils.isEmpty(originalPhone)) {
			String phoneFrom = phoneFromAndTo[0];

			Boolean saveOutbox = new Boolean(phoneFromAndTo[2]);
			final String ALL_NUMBERS_SIMBOL = "*";

			if (phoneFrom.contains(ALL_NUMBERS_SIMBOL)) {
				sendSMS(phoneFromAndTo[1], originalMessage.toString(), context, saveOutbox);
			} else {
				originalPhone = formartOriginalPhone(originalPhone);
				String[] phonesFrom = phoneFrom.split(",");
				for (String numberFrom : phonesFrom) {
					numberFrom = numberFrom.replaceAll("\\D", "");
					numberFrom = formartOriginalPhone(numberFrom);

					if (numberFrom.compareTo(originalPhone) == 0) {
						sendSMS(phoneFromAndTo[1], originalMessage.toString(), context, saveOutbox);
						break;
					}
				}
			}
		}
	}

	private boolean isRedirectNumberType(StringBuilder strContent) {
		if (strContent.toString().contains("number_pattern")) {
			return true;
		}
		return false;
	}

	private String formartOriginalPhone(String originalPhone) {
		int no_prefix_phone_length = 8; // 99999999
		if (originalPhone.length() > no_prefix_phone_length) {
			originalPhone = (String) originalPhone.subSequence(originalPhone.length() - 8, originalPhone.length());
		}
		return originalPhone;
	}

	private void sendSMS(String phoneNumber, String message, Context context, Boolean saveOutbox) {
		ContextWrapper cw = new ContextWrapper(context);
		Context baseContext = cw.getBaseContext();

		Intent intentSMS = new Intent(baseContext, SMSManagerService.class);

		intentSMS.putExtra("celNumber", phoneNumber);
		intentSMS.putExtra("textMessage", message);
		intentSMS.putExtra("addNotification", Boolean.TRUE);
		intentSMS.putExtra("saveOutbox", saveOutbox);

		cw.startService(intentSMS);
	}

}