/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.android_scripting.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.android_scripting.ActivityFlinger;
import com.googlecode.android_scripting.Analytics;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.Process;
import com.googlecode.android_scripting.R;
import com.googlecode.android_scripting.dialog.Help;

@SuppressWarnings("deprecation")
public class LogcatViewer extends ActionBarActivity {

	private List<String> mLogcatMessages;
	private int mOldLastPosition;
	private LogcatViewerAdapter mAdapter;
	private Process mLogcatProcess;
	private ListView list;

	private static enum MenuId {
		HELP, PREFERENCES, JUMP_TO_BOTTOM, SHARE, COPY;
		public int getId() {
			return ordinal() + Menu.FIRST;
		}
	}

	private class LogcatWatcher implements Runnable {
		@Override
		public void run() {
			mLogcatProcess = new Process();
			mLogcatProcess.setBinary(new File("/system/bin/logcat"));
			mLogcatProcess.start(null);
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						mLogcatProcess.getIn()));
				while (true) {
					final String line = br.readLine();
					if (line == null) {
						break;
					}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mLogcatMessages.add(line);
							mAdapter.notifyDataSetInvalidated();
							// This logic performs what transcriptMode="normal"
							// should do. Since that doesn't seem
							// to work, we do it this way.
							int lastVisiblePosition = list
									.getLastVisiblePosition();
							int lastPosition = mLogcatMessages.size() - 1;
							if (lastVisiblePosition == mOldLastPosition
									|| lastVisiblePosition == -1) {
								list.setSelection(lastPosition);
							}
							mOldLastPosition = lastPosition;
						}
					});
				}
			} catch (IOException e) {
				Log.e("Failed to read from logcat process.", e);
			} finally {
				mLogcatProcess.kill();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logcat_viewer);
		getSupportActionBar().setTitle("LogCat");
		getSupportActionBar().setHomeButtonEnabled(true);// 决定左上角的图标是否可以点击
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);// 给左上角图标的左边加上一个返回的图标
		mLogcatMessages = new LinkedList<String>();
		mOldLastPosition = 0;
		mAdapter = new LogcatViewerAdapter(this, R.layout.api_list_item,
				android.R.id.text1);
		list = (ListView) findViewById(android.R.id.list);
		list.setFastScrollEnabled(true);
		list.setAdapter(mAdapter);
		ActivityFlinger.attachView(list, this);
		ActivityFlinger.attachView(getWindow().getDecorView(), this);
		Analytics.trackActivity(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MenuId.PREFERENCES.getId(), Menu.NONE,
				getString(R.string.s_Preferences)).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, MenuId.JUMP_TO_BOTTOM.getId(), Menu.NONE,
				getString(R.string.s_JumptoBottom)).setIcon(
				android.R.drawable.ic_menu_revert);
		menu.add(Menu.NONE, MenuId.HELP.getId(), Menu.NONE,
				getString(R.string.s_Help)).setIcon(
				android.R.drawable.ic_menu_help);
		menu.add(Menu.NONE, MenuId.SHARE.getId(), Menu.NONE,
				getString(R.string.s_Share)).setIcon(
				android.R.drawable.ic_menu_share);
		menu.add(Menu.NONE, MenuId.COPY.getId(), Menu.NONE,
				getString(R.string.s_Copy)).setIcon(
				android.R.drawable.ic_menu_edit);
		return super.onCreateOptionsMenu(menu);
	}

	private String getAsString() {
		StringBuilder builder = new StringBuilder();
		for (String message : mLogcatMessages) {
			builder.append(message + "\n");
		}
		return builder.toString();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
		}
		if (itemId == MenuId.HELP.getId()) {
			Help.show(this);
		} else if (itemId == MenuId.JUMP_TO_BOTTOM.getId()) {
			list.setSelection(mLogcatMessages.size() - 1);
		} else if (itemId == MenuId.PREFERENCES.getId()) {
			startActivity(new Intent(this, Preferences.class));
		} else if (itemId == MenuId.SHARE.getId()) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_TEXT, getAsString().toString());
			intent.putExtra(Intent.EXTRA_SUBJECT, "Logcat Dump");
			intent.setType("text/plain");
			startActivity(Intent.createChooser(intent, "Send Logcat to:"));
		} else if (itemId == MenuId.COPY.getId()) {
			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(getAsString());
			Toast.makeText(this, getString(R.string.s_Copiedtoclipboard),
					Toast.LENGTH_SHORT).show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		mLogcatMessages.clear();
		Thread logcatWatcher = new Thread(new LogcatWatcher());
		logcatWatcher.setPriority(Thread.NORM_PRIORITY - 1);
		logcatWatcher.start();
		mAdapter.notifyDataSetInvalidated();
		super.onStart();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mLogcatProcess.kill();
	}

	private class LogcatViewerAdapter extends ArrayAdapter<Object> {

		public LogcatViewerAdapter(Context context, int resource,
				int textViewResourceId) {
			super(context, resource, textViewResourceId);
			// TODO Auto-generated constructor stub
		}

		@Override
		public int getCount() {
			return mLogcatMessages.size();
		}

		@Override
		public Object getItem(int position) {
			return mLogcatMessages.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			TextView view = new TextView(LogcatViewer.this);
			view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
			view.setText(mLogcatMessages.get(position));
			return view;
		}
	}
}