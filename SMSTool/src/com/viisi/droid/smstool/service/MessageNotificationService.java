package com.viisi.droid.smstool.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;

import com.viisi.droid.smstool.R;
import com.viisi.droid.smstool.activity.MainTabActivity;

public class MessageNotificationService extends Service {

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				int notifID = extras.getInt("NotifID");
				String statusMessageURI = extras.getString("statusMessageURI");
				
				Intent i = new Intent(getBaseContext(), MainTabActivity.class);
				i.putExtra("NotifID", notifID);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				
				PendingIntent detailsIntent = PendingIntent.getActivity(this, 0, i, 0);
				NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				
				StringBuilder tickerText = new StringBuilder();
				StringBuilder message = new StringBuilder();
				if (TextUtils.isEmpty(statusMessageURI) || statusMessageURI.compareTo(SMSManagerService.URI_SMS_FAILED) != 0) {
					tickerText.append(getResources().getString(R.string.app_name));
					tickerText.append(" - ");
					tickerText.append(getResources().getString(R.string.sms_send));
					
					message.append(getResources().getString(R.string.message_success_notification));
				} else {
					tickerText.append(getResources().getString(R.string.app_name));
					tickerText.append(" - ");
					tickerText.append(getResources().getString(R.string.sms_not_send));
					
					message.append(getResources().getString(R.string.message_error_notification));
				}
				String tickerTextString = tickerText.toString();
				
				// PendingIntent to launch activity if the user selects
				// the notification
				Notification notif = new Notification(R.drawable.ic_tab2_grey, tickerTextString, System.currentTimeMillis());
				notif.flags = Notification.FLAG_AUTO_CANCEL;
				
				notif.setLatestEventInfo(this, tickerTextString, message.toString(), detailsIntent);
				
				// 100ms delay, vibrate for 250ms, pause for 100 ms and
				// then vibrate for 500ms
				notif.vibrate = new long[] { 100, 250, 100, 500 };
				nm.notify(notifID, notif);
			}
			
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}