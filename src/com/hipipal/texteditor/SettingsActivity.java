package com.hipipal.texteditor;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import com.googlecode.android_scripting.Analytics;
import com.googlecode.android_scripting.R;
import com.hipipal.texteditor.common.Constants;
import com.hipipal.texteditor.common.Settings;
import com.hipipal.texteditor.ui.view.AdvancedEditText;

@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements Constants,
		OnSharedPreferenceChangeListener {

	protected AdvancedEditText mSampleTED;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		getPreferenceManager().setSharedPreferencesName(PREFERENCES_NAME);

		addPreferencesFromResource(R.xml.ted_prefs);
		setContentView(R.layout.layout_prefs);

		getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

		mSampleTED = (AdvancedEditText) findViewById(R.id.sampleEditor);

		Settings.updateFromPreferences(getPreferenceManager()
				.getSharedPreferences());
		mSampleTED.updateFromSettings("");
		mSampleTED.setEnabled(false);
		updateSummaries();
		Analytics.trackActivity(this);
	}

	/**
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences,
	 *      java.lang.String)
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Settings.updateFromPreferences(sharedPreferences);
		mSampleTED.updateFromSettings("");
		updateSummaries();
	}

	/**
	 * Updates the summaries for every list
	 */
	protected void updateSummaries() {
		ListPreference listPref;

		listPref = (ListPreference) findPreference(PREFERENCE_COLOR_THEME);
		listPref.setSummary(listPref.getEntry());

		listPref = (ListPreference) findPreference(PREFERENCE_TEXT_SIZE);
		listPref.setSummary(listPref.getEntry());

		listPref = (ListPreference) findPreference(PREFERENCE_END_OF_LINES);
		listPref.setSummary(listPref.getEntry());

		listPref = (ListPreference) findPreference(PREFERENCE_ENCODING);
		listPref.setSummary(listPref.getEntry());

		listPref = (ListPreference) findPreference(PREFERENCE_MAX_UNDO_STACK);
		listPref.setSummary(listPref.getEntry());
	}

}
