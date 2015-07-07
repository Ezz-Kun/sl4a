/*
 * Copyright (C) 2009 Google Inc.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import python.listview.PinnedSectionListView;
import python.listview.PinnedSectionListView.PinnedSectionListAdapter;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.googlecode.android_scripting.Analytics;
import com.googlecode.android_scripting.BaseApplication;
import com.googlecode.android_scripting.Constants;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.R;
import com.googlecode.android_scripting.dialog.Help;
import com.googlecode.android_scripting.facade.FacadeConfiguration;
import com.googlecode.android_scripting.interpreter.Interpreter;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;
import com.googlecode.android_scripting.language.SupportedLanguages;
import com.googlecode.android_scripting.rpc.MethodDescriptor;
import com.googlecode.android_scripting.rpc.ParameterDescriptor;
import com.googlecode.android_scripting.rpc.RpcDeprecated;
import com.googlecode.android_scripting.rpc.RpcMinSdk;

@SuppressLint("DefaultLocale")
public class ApiBrowser extends ActionBarActivity {

	private boolean searchResultMode = false;

	private static enum RequestCode {
		RPC_PROMPT
	}

	private static enum ContextMenuId {
		INSERT_TEXT, PROMPT_PARAMETERS, HELP;
		public int getId() {
			return ordinal() + Menu.FIRST;
		}
	}

	private List<MethodDescriptor> mMethodDescriptors;
	private PinnedSectionListView pList;
	private Set<Integer> mExpandedPositions;
	private ApiBrowserAdapter mAdapter;
	private boolean mIsLanguageSupported;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.api_browser);
		getSupportActionBar().setTitle("Api Browser");
		getSupportActionBar().setHomeButtonEnabled(true);// 决定左上角的图标是否可以点击
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);// 给左上角图标的左边加上一个返回的图标
		pList = (PinnedSectionListView) findViewById(android.R.id.list);
		pList.setFastScrollEnabled(true);
		pList.setShadowVisible(true);
		mExpandedPositions = new HashSet<Integer>();
		updateAndFilterMethodDescriptors(null);
		String scriptName = getIntent().getStringExtra(
				Constants.EXTRA_SCRIPT_PATH);
		mIsLanguageSupported = SupportedLanguages
				.checkLanguageSupported(scriptName);
		mAdapter = new ApiBrowserAdapter(this, R.layout.api_list_item,
				android.R.id.text1);
		// android.R.layout.simple_list_item_1, android.R.id.text1);
		pList.setAdapter(mAdapter);
		registerForContextMenu(pList);
		Analytics.trackActivity(this);
		setResult(RESULT_CANCELED);
		pList.setOnItemClickListener(itemClickListener);
	}

	private void updateAndFilterMethodDescriptors(final String query) {
		mMethodDescriptors = Lists.newArrayList(Collections2.filter(
				FacadeConfiguration.collectMethodDescriptors(),
				new Predicate<MethodDescriptor>() {
					@Override
					public boolean apply(MethodDescriptor descriptor) {
						Method method = descriptor.getMethod();
						if (method.isAnnotationPresent(RpcDeprecated.class)) {
							return false;
						} else if (method.isAnnotationPresent(RpcMinSdk.class)) {
							int requiredSdkLevel = method.getAnnotation(
									RpcMinSdk.class).value();
							if (FacadeConfiguration.getSdkLevel() < requiredSdkLevel) {
								return false;
							}
						}
						if (query == null) {
							return true;
						}
						return descriptor.getName().toLowerCase()
								.contains(query.toLowerCase());
					}
				}));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			searchResultMode = true;
			final String query = intent.getStringExtra(SearchManager.QUERY);
			// ((TextView) findViewById(R.id.left_text)).setText(query);
			updateAndFilterMethodDescriptors(query);
			if (mMethodDescriptors.size() == 1) {
				mExpandedPositions.add(0);
			} else {
				mExpandedPositions.clear();
			}
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && searchResultMode) {
			searchResultMode = false;
			mExpandedPositions.clear();
			updateAndFilterMethodDescriptors("");
			mAdapter.notifyDataSetChanged();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.apimenu, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.expandall),
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.collapseall),
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
		} else if (id == R.id.expandall) {
			for (int i = 0; i < mMethodDescriptors.size(); i++) {
				mExpandedPositions.add(i);
			}
		} else if (id == R.id.collapseall) {
			mExpandedPositions.clear();
		}
		mAdapter.notifyDataSetInvalidated();
		return true;
	}

	protected OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position,
				long id) {
			if (mExpandedPositions.contains(position)) {
				mExpandedPositions.remove(position);
			} else {
				mExpandedPositions.add(position);
			}
			mAdapter.notifyDataSetInvalidated();
		}
	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		if (!mIsLanguageSupported) {
			return;
		}
		menu.add(Menu.NONE, ContextMenuId.INSERT_TEXT.getId(), Menu.NONE,
				getResources().getString(R.string.s_Insert));
		menu.add(Menu.NONE, ContextMenuId.PROMPT_PARAMETERS.getId(), Menu.NONE,
				getResources().getString(R.string.s_Prompt));
		if (Help.checkApiHelp(this)) {
			menu.add(Menu.NONE, ContextMenuId.HELP.getId(), Menu.NONE,
					getResources().getString(R.string.s_Help));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			Log.e("Bad menuInfo", e);
			return false;
		}

		MethodDescriptor rpc = (MethodDescriptor) ((ApiItem) pList.getAdapter()
				.getItem(info.position)).getMethod();
		if (rpc == null) {
			Log.v("No RPC selected.");
			return false;
		}

		if (item.getItemId() == ContextMenuId.INSERT_TEXT.getId()) {
			// There's no activity to track calls to insert (like there is for
			// prompt) so we track it
			// here instead.
			Analytics.track("ApiInsert");
			insertText(rpc, new String[0]);
		} else if (item.getItemId() == ContextMenuId.PROMPT_PARAMETERS.getId()) {
			Intent intent = new Intent(this, ApiPrompt.class);
			intent.putExtra(Constants.EXTRA_API_PROMPT_RPC_NAME, rpc.getName());
			ParameterDescriptor[] parameters = rpc
					.getParameterValues(new String[0]);
			String[] values = new String[parameters.length];
			int index = 0;
			for (ParameterDescriptor parameter : parameters) {
				values[index++] = parameter.getValue();
			}
			intent.putExtra(Constants.EXTRA_API_PROMPT_VALUES, values);
			startActivityForResult(intent, RequestCode.RPC_PROMPT.ordinal());
		} else if (item.getItemId() == ContextMenuId.HELP.getId()) {
			String help = rpc.getDeclaringClass().getSimpleName() + ".html#"
					+ rpc.getName();
			Help.showApiHelp(this, help);

		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		RequestCode request = RequestCode.values()[requestCode];
		if (resultCode == RESULT_OK) {
			switch (request) {
			case RPC_PROMPT:
				MethodDescriptor rpc = FacadeConfiguration
						.getMethodDescriptor(data
								.getStringExtra(Constants.EXTRA_API_PROMPT_RPC_NAME));
				String[] values = data
						.getStringArrayExtra(Constants.EXTRA_API_PROMPT_VALUES);
				insertText(rpc, values);
				break;
			default:
				break;
			}
		} else {
			switch (request) {
			case RPC_PROMPT:
				break;
			default:
				break;
			}
		}
	}

	private void insertText(MethodDescriptor rpc, String[] values) {
		String scriptText = getIntent().getStringExtra(
				Constants.EXTRA_SCRIPT_TEXT);
		InterpreterConfiguration config = ((BaseApplication) getApplication())
				.getInterpreterConfiguration();

		Interpreter interpreter = config.getInterpreterByName(getIntent()
				.getStringExtra(Constants.EXTRA_INTERPRETER_NAME));
		String rpcHelpText = interpreter.getRpcText(scriptText, rpc, values);

		Intent intent = new Intent();
		intent.putExtra(Constants.EXTRA_RPC_HELP_TEXT, rpcHelpText);
		setResult(RESULT_OK, intent);
		finish();
	}

	private class ApiBrowserAdapter extends ArrayAdapter<ApiItem> implements
			PinnedSectionListAdapter, SectionIndexer {

		// <color name="green_light">#ff99cc00</color>
		// <color name="orange_light">#ffffbb33</color>
		// <color name="blue_light">#ff33b5e5</color>
		// <color name="red_light">#ffff4444</color>
		private final int[] COLORS = new int[] { Color.parseColor("#ff99cc00"),
				Color.parseColor("#ffffbb33"), Color.parseColor("#ff33b5e5"),
				Color.parseColor("#ffff4444") };
		private List<ApiItem> list = new ArrayList<ApiItem>();
		private ApiItem[] aSections;
		private int pos = 0, sectionPosition = 0, listPosition = 0;
		final int sectionsNumber = 'z' - 'a' + 1;

		public ApiBrowserAdapter(Context context, int resource,
				int textViewResourceId) {
			super(context, resource, textViewResourceId);
			prepareSections(sectionsNumber);
			for (MethodDescriptor info : mMethodDescriptors) {
				// Log.i(info.getName());
				list.add(new ApiItem(ApiItem.ITEM, info.getName(), info
						.getHelp(), info));
			}
			for (char i = 0; i < sectionsNumber; i++) {
				// Log.i(String.valueOf((char) ('a' + i)));
				String value = String.valueOf((char) ('a' + i));
				list.add(new ApiItem(ApiItem.SECTION, value, value, null));
			}
			// 排序
			Collections.sort(list, new Comparator<ApiItem>() {
				@Override
				public int compare(ApiItem arg0, ApiItem arg1) {
					return arg0.getName().compareTo(arg1.getName());
				}
			});

			// 设置sectionPosition 和 listPosition
			for (int i = 0; i < list.size(); i++) {
				ApiItem item = list.get(i);
				if (item.getName().length() == 1) {
					// 字母
					pos = sectionPosition;
					item.sectionPosition = sectionPosition;
					item.listPosition = listPosition++;
					sectionAdded(item, sectionPosition);
					add(item);
					sectionPosition++;
				} else {
					// 函数名
					item.sectionPosition = pos;
					item.listPosition = listPosition++;
					add(item);
				}
			}
		}

		public void prepareSections(int sectionsNumber) {
			// Log.i("prepareSections: " + Integer.toString(sectionsNumber));
			aSections = new ApiItem[sectionsNumber];// 26个
		}

		public void sectionAdded(ApiItem section, int sectionPosition) {
			// Log.i("sectionAdded: " + Integer.toString(sectionPosition));
			aSections[sectionPosition] = section;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			TextView view = (TextView) super.getView(position, convertView,
					parent);
			view.setTextColor(Color.WHITE);
			view.setTag("" + position);
			ApiItem item = getItem(position);
			if (item.type == ApiItem.SECTION) {
				view.setGravity(Gravity.CENTER);
				view.setBackgroundColor(COLORS[item.sectionPosition
						% COLORS.length]);
			}
			if (mExpandedPositions.contains(position)) {
				view.setText(item.getHelp());
			} else {
				view.setText(item.getName());
			}
			return view;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return getItem(position).type;
		}

		@Override
		public boolean isItemViewTypePinned(int viewType) {
			return viewType == ApiItem.SECTION;
		}

		@Override
		public ApiItem[] getSections() {
			return aSections;
		}

		@Override
		public int getPositionForSection(int section) {
			if (section >= aSections.length) {
				section = aSections.length - 1;
			}
			return aSections[section].listPosition;
		}

		@Override
		public int getSectionForPosition(int position) {
			if (position >= getCount()) {
				position = getCount() - 1;
			}
			return getItem(position).sectionPosition;
		}

	}

}
