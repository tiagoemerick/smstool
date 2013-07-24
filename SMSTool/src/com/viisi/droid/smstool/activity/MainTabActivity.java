package com.viisi.droid.smstool.activity;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TabHost;

import com.viisi.droid.smstool.R;

public class MainTabActivity extends TabActivity {
	
	private final static String APP_PNAME = "com.viisi.droid.smstool";
	private Button rate;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		createComponentsView();
		createComponentsListeners();

		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, SendSMSTabActivity.class);

		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost.newTabSpec("tab1").setIndicator(res.getString(R.string.tab1_name), res.getDrawable(R.drawable.ic_tab1)).setContent(intent);
		tabHost.addTab(spec);

		// Do the same for the other tabs
		intent = new Intent().setClass(this, RedirectSMSTabActivity.class);
		spec = tabHost.newTabSpec("tab2").setIndicator(res.getString(R.string.tab2_name), res.getDrawable(R.drawable.ic_tab2)).setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);
	}
	
	private OnClickListener rateListener = new OnClickListener() {
		public void onClick(View arg0) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplicationContext().startActivity(intent);
		}
	};

	private void createComponentsView() {
		rate = (Button) this.findViewById(R.id.rateButton);
	}
	
	private void createComponentsListeners() {
		rate.setOnClickListener(rateListener);
	}

}