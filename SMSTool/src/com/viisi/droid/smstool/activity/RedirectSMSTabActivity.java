package com.viisi.droid.smstool.activity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.viisi.droid.smstool.R;
import com.viisi.droid.smstool.entity.Contact;

public class RedirectSMSTabActivity extends Activity {

	private static final int PICK_CONTACT = 1;

	private Contact contact;
	private String sourceContactListener;

	private RelativeLayout.LayoutParams p;
	private RadioGroup radioFilter;
	private int checkedRadioId;

	private RelativeLayout filterNumberFields;
	private RelativeLayout filterMsgFields;
	private RelativeLayout commonFields;

	private EditText textFromNumberFields;
	private EditText textToNumberFields;
	private Button pickContactFromNumberFields;
	private Button pickContactToNumberFields;

	private EditText textPatternMsgFields;
	private EditText textToMsgFields;
	private Button pickContactToMsgFields;

	private Button saveButton;
	private Button clearButton;
	private CheckBox checkSaveOutbox;
	private CheckBox checkDisplayNotification;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.redirect_sms_tab);

		createComponentsView();
		createComponentsListeners();
	}

	private OnCheckedChangeListener radioFilterChangedListener = new OnCheckedChangeListener() {
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			int fieldBelow = 0;
			checkedRadioId = checkedId;

			if (isNumberRadioSelected()) {
				filterNumberFields.setVisibility(ViewGroup.VISIBLE);
				filterMsgFields.setVisibility(ViewGroup.GONE);

				fieldBelow = R.id.filterNumberFields;

			} else {
				filterNumberFields.setVisibility(ViewGroup.GONE);
				filterMsgFields.setVisibility(ViewGroup.VISIBLE);

				fieldBelow = R.id.filterMsgFields;
			}
			p.addRule(RelativeLayout.BELOW, fieldBelow);
			commonFields.setLayoutParams(p);
		}
	};

	private OnClickListener contactFromListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
			sourceContactListener = "sourceFrom";

			startActivityForResult(intent, PICK_CONTACT);
		}
	};

	private OnClickListener contactToListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
			sourceContactListener = "sourceTo";

			startActivityForResult(intent, PICK_CONTACT);
		}
	};

	private OnClickListener contactToMsgTypeListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
			sourceContactListener = "sourceToMsgType";

			startActivityForResult(intent, PICK_CONTACT);
		}
	};

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		if (!TextUtils.isEmpty(sourceContactListener)) {
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
								String finalNumberFormated = numberP.replaceAll("\\D", "");

								if (sourceContactListener.equals("sourceFrom")) {
									textFromNumberFields.setText(textFromNumberFields.getText() + finalNumberFormated + ",");
								} else if (sourceContactListener.equals("sourceTo")) {
									textToNumberFields.setText(textToNumberFields.getText() + finalNumberFormated + ",");
								} else if (sourceContactListener.equals("sourceToMsgType")) {
									textToMsgFields.setText(textToMsgFields.getText() + finalNumberFormated + ",");
								}
							}
						});
						AlertDialog alert = builder.create();
						alert.show();
					} else if (contact.getPhones().size() == 1) {
						String numberP = new String(contact.getPhones().get(0));
						String finalNumberFormated = numberP.replaceAll("\\D", "");

						if (sourceContactListener.equals("sourceFrom")) {
							textFromNumberFields.setText(textFromNumberFields.getText() + finalNumberFormated + ",");
						} else if (sourceContactListener.equals("sourceTo")) {
							textToNumberFields.setText(textToNumberFields.getText() + finalNumberFormated + ",");
						} else if (sourceContactListener.equals("sourceToMsgType")) {
							textToMsgFields.setText(textToMsgFields.getText() + finalNumberFormated + ",");
						}
					}
				}
			}
		}
	}

	private boolean isNumberRadioSelected() {
		if (checkedRadioId == 0 || checkedRadioId == R.id.radioNumber) {
			return true;
		}
		return false;
	}

	private OnClickListener clearPatterns = new OnClickListener() {
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(RedirectSMSTabActivity.this);

			builder.setMessage(R.string.confirm_delete_filter_alert);
			builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					final String FILENAME = "redirect_file";
					if (deleteFile(FILENAME)) {
						Toast.makeText(getBaseContext(), R.string.message_delete_success, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getBaseContext(), R.string.message_delete_not_success, Toast.LENGTH_SHORT).show();
					}
				}

			});
			builder.setNegativeButton(R.string.no, null);
			builder.show();
		}
	};

	private OnClickListener saveRedirectMessages = new OnClickListener() {
		public void onClick(View v) {
			if (isNumberRadioSelected()) {
				saveRedirectDataNumberTypeToFile();
			} else {
				saveRedirectDataMsgTypeToFile();
			}
		}

		private void saveRedirectDataNumberTypeToFile() {
			String numberFrom = textFromNumberFields.getText().toString();
			String numberTo = textToNumberFields.getText().toString();

			if (!TextUtils.isEmpty(numberTo)) {
				saveRedirectDataToFile(numberFrom, numberTo);
			} else {
				Toast.makeText(getBaseContext(), R.string.fill_required_destination_fields, Toast.LENGTH_SHORT).show();
			}
		}

		private void saveRedirectDataMsgTypeToFile() {
			String messagePattern = textPatternMsgFields.getText().toString();
			String numberTo = textToMsgFields.getText().toString();

			if (!TextUtils.isEmpty(messagePattern) || !TextUtils.isEmpty(numberTo)) {
				saveRedirectDataToFile(messagePattern, numberTo);
			} else {
				Toast.makeText(getBaseContext(), R.string.fill_required_blank_fields, Toast.LENGTH_SHORT).show();
			}
		}

		/**
		 * Padrão de ordem de dados salvos no arquivo: [0] - numberFom ou
		 * messagePattern digitado pelo usuario. [1] - numberTo digitado pelo
		 * usuario. [2] - Boolean indicando se é para salvar no outbox [3] -
		 * String number_pattern que indica que o arquivo é para ser lido como
		 * parao de mensagens e nao por numeros
		 */
		private void saveRedirectDataToFile(String numberFromOrMessagePattern, String numberTo) {
			final String FILENAME = "redirect_file";
			StringBuilder dados = new StringBuilder();

			if (isNumberRadioSelected()) {
				if (numberFromOrMessagePattern == null || numberFromOrMessagePattern.trim().compareTo("") == 0) {
					final String ALL_NUMBERS_SIMBOL = "*";
					numberFromOrMessagePattern = ALL_NUMBERS_SIMBOL;
				}

				dados.append(numberFromOrMessagePattern);
				dados.append(";");
				dados.append(numberTo);
				dados.append(";");
				dados.append(checkSaveOutbox.isChecked());
				dados.append(";");
				dados.append(checkDisplayNotification.isChecked());
				dados.append(";");
				dados.append("number_pattern");
			} else {
				dados.append(numberFromOrMessagePattern);
				dados.append(";");
				dados.append(numberTo);
				dados.append(";");
				dados.append(checkSaveOutbox.isChecked());
				dados.append(";");
				dados.append(checkDisplayNotification.isChecked());
				dados.append(";");
				dados.append("message_pattern");
			}

			FileOutputStream fos = null;
			try {
				fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
				fos.write(dados.toString().getBytes());
				fos.close();
				
				Toast.makeText(getBaseContext(), R.string.message_pattern_saved, Toast.LENGTH_SHORT).show();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	private void getContactInfo(Intent intent) {
		contact = new Contact();

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
//		cursor.close();
	}

	private void createComponentsView() {
		p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		radioFilter = (RadioGroup) findViewById(R.id.radioFilter);

		// Fields
		filterNumberFields = (RelativeLayout) findViewById(R.id.filterNumberFields);
		filterNumberFields.setVisibility(View.VISIBLE);

		filterMsgFields = (RelativeLayout) findViewById(R.id.filterMsgFields);
		filterMsgFields.setVisibility(View.GONE);

		commonFields = (RelativeLayout) findViewById(R.id.commonFields);
		p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		p.addRule(RelativeLayout.BELOW, R.id.filterNumberFields);
		commonFields.setLayoutParams(p);

		// NumberField
		textFromNumberFields = (EditText) findViewById(R.id.textFromNumberFields);
		textToNumberFields = (EditText) findViewById(R.id.textToNumberFields);
		saveButton = (Button) this.findViewById(R.id.saveButton);
		clearButton = (Button) this.findViewById(R.id.clearButton);
		pickContactFromNumberFields = (Button) this.findViewById(R.id.pickContactFromNumberFields);
		pickContactToNumberFields = (Button) this.findViewById(R.id.pickContactToNumberFields);

		// MsgField
		textPatternMsgFields = (EditText) findViewById(R.id.textPatternMsgFields);
		textToMsgFields = (EditText) findViewById(R.id.textToMsgFields);
		pickContactToMsgFields = (Button) this.findViewById(R.id.pickContactToMsgFields);

		// CommonField
		checkSaveOutbox = (CheckBox) this.findViewById(R.id.checkSaveOutbox);
		checkSaveOutbox.setChecked(Boolean.TRUE);
		checkDisplayNotification = (CheckBox) this.findViewById(R.id.checkDisplayNotification);
		checkDisplayNotification.setChecked(Boolean.TRUE);
	}

	private void createComponentsListeners() {
		radioFilter.setOnCheckedChangeListener(radioFilterChangedListener);
		pickContactFromNumberFields.setOnClickListener(contactFromListener);
		pickContactToNumberFields.setOnClickListener(contactToListener);
		pickContactToMsgFields.setOnClickListener(contactToMsgTypeListener);
		saveButton.setOnClickListener(saveRedirectMessages);
		clearButton.setOnClickListener(clearPatterns);
	}

}