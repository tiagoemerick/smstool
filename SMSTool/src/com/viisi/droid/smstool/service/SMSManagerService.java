package com.viisi.droid.smstool.service;

import java.util.Random;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.viisi.droid.smstool.R;
import com.viisi.droid.smstool.util.SendSMSBugCorrection;

public class SMSManagerService extends Service {

	protected static final String URI_SMS_SENT = "content://sms/sent";
	protected static final String URI_SMS_FAILED = "content://sms/failed";
	protected static final String URI_SMS_INBOX = "content://sms/inbox";
	protected static final String URI_SMS_QUEUED = "content://sms/queued";
	protected static final String URI_SMS_DRAFT = "content://sms/draft";
	protected static final String URI_SMS_OUTBOX = "content://sms/outbox";
	protected static final String URI_SMS_UNDELIVERED = "content://sms/undelivered";
	protected static final String URI_SMS_ALL = "content://sms/all";
	protected static final String URI_SMS_CONVERSATIONS = "content://sms/conversations";

	private PendingIntent sentPI;
	private PendingIntent deliveredPI;

	private String statusMessageURI = URI_SMS_SENT;
	private Boolean sendNotification;
	private Boolean saveOutbox;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle intentExtras = intent.getExtras();
		if (intentExtras != null) {
			String phoneNumber = intentExtras.getString("celNumber");
			String message = intentExtras.getString("textMessage");

			if (isContentValid(phoneNumber, message)) {
				sendNotification = intentExtras.getBoolean("addNotification");
				saveOutbox = intentExtras.getBoolean("saveOutbox");

				sendSMS(phoneNumber, message);
			} else {
				Toast.makeText(getBaseContext(), R.string.fill_all_blank_fields, Toast.LENGTH_SHORT).show();
			}
		}

		return super.onStartCommand(intent, PendingIntent.FLAG_ONE_SHOT, startId);
	}

	private void saveOutboxSMS(String phoneNumber, String message) {
		if (saveOutbox != null && saveOutbox) {
			String[] phonesToSend = phoneNumber.split(",");
			for (String phone : phonesToSend) {
				ContentValues values = new ContentValues();
				values.put("address", phone.replaceAll("\\D", ""));
				values.put("body", message);
				getContentResolver().insert(Uri.parse(statusMessageURI), values);
			}
		}
	}

	private boolean isContentValid(String phoneNumber, String message) {
		if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(message)) {
			return false;
		}
		return true;
	}

	private void addNotification() {
		if (statusMessageURI.compareTo(URI_SMS_FAILED) == 0 || (sendNotification != null && sendNotification)) {
			Intent i = new Intent(getBaseContext(), MessageNotificationService.class);
			i.putExtra("NotifID", 1);
			i.putExtra("statusMessageURI", statusMessageURI);

			sentPI = PendingIntent.getService(getBaseContext(), 0, i, 0);
			try {
				sentPI.send();
			} catch (CanceledException e) {
				Toast.makeText(getBaseContext(), R.string.sms_not_send, Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void sendSMS(String phoneNumber, String message) {
		sentPI = registroMensagemEnviada(phoneNumber, message);
		deliveredPI = registroMensagemEntregue();

		SmsManager sms = SmsManager.getDefault();

		String[] phonesToSend = phoneNumber.split(",");
		for (String phone : phonesToSend) {
			SendSMSBugCorrection.send(phone.replaceAll("\\D", ""), message.trim(), sentPI, deliveredPI, sms);
			// sms.sendTextMessage(phone.replaceAll("\\D", ""), null, message, sentPI, deliveredPI);
		}

	}

	/**
	 * Cria um evento que sera acionado quando o serviço de mensagem tiver
	 * <b>enviado</b> o SMS
	 * 
	 * @param message
	 *            to save outbox
	 * @param phoneNumber
	 * 
	 * @return PendingIntent
	 */
	private PendingIntent registroMensagemEnviada(String phoneNumber, String message) {
		Random s = new Random(System.currentTimeMillis());
		StringBuilder SENT = new StringBuilder("SMS_SENT_TOOL");
		SENT.append(s.nextLong());

		String SENT_STRING = SENT.toString();
		PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT_STRING), PendingIntent.FLAG_ONE_SHOT);

		final String phoneNumberFinal = phoneNumber;
		final String messageFinal = message;

		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), R.string.sms_send, Toast.LENGTH_SHORT).show();
					statusMessageURI = URI_SMS_SENT;
					break;
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					Toast.makeText(getBaseContext(), R.string.sms_not_send, Toast.LENGTH_SHORT).show();
					statusMessageURI = URI_SMS_FAILED;
					break;
				case SmsManager.RESULT_ERROR_NO_SERVICE:
					Toast.makeText(getBaseContext(), "Sem serviço", Toast.LENGTH_SHORT).show();
					statusMessageURI = URI_SMS_FAILED;
					break;
				case SmsManager.RESULT_ERROR_NULL_PDU:
					Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
					statusMessageURI = URI_SMS_FAILED;
					break;
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
					statusMessageURI = URI_SMS_FAILED;
					break;
				}
				addNotification();
				saveOutboxSMS(phoneNumberFinal, messageFinal);
			}
		}, new IntentFilter(SENT_STRING));

		return sentPI;
	}

	/**
	 * Cria um evento que sera acionado quando o serviço de mensagem tiver
	 * <b>entregue</b> o SMS
	 * 
	 * @return PendingIntent
	 */
	private PendingIntent registroMensagemEntregue() {
		Random s = new Random(System.currentTimeMillis());
		StringBuilder DELIVERED = new StringBuilder("SMS_DELIVERED_TOOL");
		DELIVERED.append(s.nextLong());

		String DELIVERED_STRING = DELIVERED.toString();

		PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED_STRING), PendingIntent.FLAG_ONE_SHOT);

		final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				switch (getResultCode()) {
				case Activity.RESULT_OK:
					Toast.makeText(getBaseContext(), R.string.sms_sent, Toast.LENGTH_SHORT).show();

					vibrator.vibrate(100);
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
					vibrator.vibrate(100);
					break;
				case Activity.RESULT_CANCELED:
					Toast.makeText(getBaseContext(), R.string.sms_not_sent, Toast.LENGTH_SHORT).show();
					vibrator.vibrate(500);
					break;
				}
			}
		}, new IntentFilter(DELIVERED_STRING));

		return deliveredPI;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}