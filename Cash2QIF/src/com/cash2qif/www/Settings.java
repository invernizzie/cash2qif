package com.cash2qif.www;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {
	public static final String SETTINGS_NAME = "com.cash2qif.www_preferences";
	public static final String EMAIL_ADDRESS = "email_address";
	public static final String LAST_EXPORT_DATE = "last_export_date";
	public static final String SEND_EMAIL = "send_email";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.settings);
    }
}