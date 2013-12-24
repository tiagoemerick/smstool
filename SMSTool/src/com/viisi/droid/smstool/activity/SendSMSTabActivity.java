package com.viisi.droid.smstool.activity;

import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.viisi.droid.smstool.R;
import com.viisi.droid.smstool.entity.Contact;
import com.viisi.droid.smstool.service.SMSManagerService;

public class SendSMSTabActivity extends Activity {

	private final static String APP_PNAME = "com.viisi.droid.smstool";
	private Button rate;

	private static final int PICK_CONTACT = 1;

	private Contact contact;

	private EditText number;
	private EditText message;
	private Button sendButton;
	private Button clearButton;
	private Button pickContact;
	private CheckBox checkSaveOutbox;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_sms_tab);

		createComponentsView();
		createComponentsListeners();
	}

	private OnClickListener contactListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
			startActivityForResult(intent, PICK_CONTACT);
		}
	};

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		if (reqCode == PICK_CONTACT) {
			if (resultCode == Activity.RESULT_OK) {
				getContactInfo(data);

				// Mais de um telefone no contato
				if (contact.getPhones().size() > 1) {
					final CharSequence[] items = new CharSequence[contact.getPhones().size()];

					int i = 0;
					for (String phone : contact.getPhones()) {
						items[i] = phone;
						i++;
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(contact.getName());
					builder.setItems(items, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							String numberP = new String(items[item].toString());
							number.setText(number.getText() + numberP.replaceAll("\\D", "") + ",");
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				} else if (contact.getPhones().size() == 1) {
					String numberP = new String(contact.getPhones().get(0));
					number.setText(number.getText() + numberP.replaceAll("\\D", "") + ",");
				}
			}
		}
	}

	private OnClickListener okListener = new OnClickListener() {
		public void onClick(View v) {
			String celNumber = number.getText().toString();
			String textMessage = message.getText().toString();

			sendSMS(celNumber, textMessage);
		}

		private void sendSMS(String celNumber, String textMessage) {
			Context baseContext = getApplicationContext();

			Random s = new Random(System.currentTimeMillis());
			String START_SENDING = "START_SENDING_TOOL" + s.nextLong();

			Intent intentSMS = new Intent(START_SENDING, null, baseContext, SMSManagerService.class);

			intentSMS.putExtra("celNumber", celNumber);
			intentSMS.putExtra("textMessage", textMessage);
			intentSMS.putExtra("addNotification", Boolean.FALSE);
			intentSMS.putExtra("saveOutbox", checkSaveOutbox.isChecked());

			PendingIntent pendingIntent = PendingIntent.getService(baseContext, 0, intentSMS, PendingIntent.FLAG_ONE_SHOT);
			try {
				pendingIntent.send();
			} catch (CanceledException e) {
			}

			// startService(intentSMS);
		}
	};

	private OnClickListener clearListener = new OnClickListener() {
		public void onClick(View v) {
			number.setText("");
			message.setText("");
			Toast.makeText(getBaseContext(), R.string.toast_cleared, Toast.LENGTH_SHORT).show();
		}
	};

	private void getContactInfo(Intent intent) {
		contact = new Contact();

		@SuppressWarnings("deprecation")
		Cursor cursor = managedQuery(intent.getData(), null, null, null, null);
		while (cursor.moveToNext()) {
			String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
			contact.setId(Long.valueOf(contactId));

			String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
			contact.setName(name);

			String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
			if (hasPhone.equalsIgnoreCase("1")) {
				hasPhone = "true";
			} else {
				hasPhone = "false";
			}

			if (Boolean.parseBoolean(hasPhone)) {
				Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
				while (phones.moveToNext()) {
					StringBuilder formatedPhoneNumber = new StringBuilder();
					formatedPhoneNumber.append(phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

					final String data_type = "data2";
					int type = phones.getInt(phones.getColumnIndex(data_type));

					switch (type) {
					case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
						final String data_label_name = "data3";
						formatedPhoneNumber.append(" - ");
						formatedPhoneNumber.append(phones.getString(phones.getColumnIndex(data_label_name)));
						break;
					case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
						formatedPhoneNumber.append(" - Fax");
						break;
					case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
						formatedPhoneNumber.append(" - Fax");
						break;
					case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
						formatedPhoneNumber.append(" - ");
						formatedPhoneNumber.append(getResources().getString(R.string.type_phone_home));
						break;
					case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
						formatedPhoneNumber.append(" - ");
						formatedPhoneNumber.append(getResources().getString(R.string.type_phone_mobile));
						break;
					case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
						formatedPhoneNumber.append(" - ");
						formatedPhoneNumber.append(getResources().getString(R.string.type_phone_work));
						break;
					case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
						formatedPhoneNumber.append(" - ");
						formatedPhoneNumber.append(getResources().getString(R.string.type_phone_mobile_work));
						break;
					}
					contact.getPhones().add(formatedPhoneNumber.toString());
				}
				phones.close();
			}
		}
		// cursor.close();
	}

	private OnClickListener rateListener = new OnClickListener() {
		public void onClick(View arg0) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplicationContext().startActivity(intent);
		}
	};

	private void createComponentsView() {
		number = (EditText) findViewById(R.id.text);
		message = (EditText) findViewById(R.id.textTouch);
		sendButton = (Button) this.findViewById(R.id.button1);
		clearButton = (Button) this.findViewById(R.id.button2);
		pickContact = (Button) this.findViewById(R.id.pickContact);
		rate = (Button) this.findViewById(R.id.rateButton);

		checkSaveOutbox = (CheckBox) this.findViewById(R.id.checkSaveOutbox);
		checkSaveOutbox.setChecked(Boolean.TRUE);
	}

	private void createComponentsListeners() {
		sendButton.setOnClickListener(okListener);
		clearButton.setOnClickListener(clearListener);
		pickContact.setOnClickListener(contactListener);
		rate.setOnClickListener(rateListener);
	}

}