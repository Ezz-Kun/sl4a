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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.googlecode.android_scripting.Analytics;
import com.googlecode.android_scripting.Constants;
import com.googlecode.android_scripting.R;
import com.googlecode.android_scripting.facade.FacadeConfiguration;
import com.googlecode.android_scripting.rpc.MethodDescriptor;

/**
 * Prompts for API parameters.
 * 
 * <p>
 * This activity is started by {@link ApiBrowser} to prompt user for RPC call
 * parameters. Input/output interface is RPC name and explicit parameter values.
 * 
 * @author igor.v.karp@gmail.com (Igor Karp)
 */
class ApiPrompt extends ActionBarActivity {
	private MethodDescriptor mRpc;
	private String[] mHints;
	private String[] mValues;
	private ApiPromptAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.api_prompt);
		getSupportActionBar().setHomeButtonEnabled(true);// 决定左上角的图标是否可以点击
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);// 给左上角图标的左边加上一个返回的图标
		mRpc = FacadeConfiguration.getMethodDescriptor(getIntent()
				.getStringExtra(Constants.EXTRA_API_PROMPT_RPC_NAME));
		mHints = mRpc.getParameterHints();
		mValues = getIntent().getStringArrayExtra(
				Constants.EXTRA_API_PROMPT_VALUES);
		mAdapter = new ApiPromptAdapter();
		((ListView) findViewById(R.id.list)).setAdapter(mAdapter);
		((Button) findViewById(R.id.done))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent();
						intent.putExtra(Constants.EXTRA_API_PROMPT_RPC_NAME,
								mRpc.getName());
						intent.putExtra(Constants.EXTRA_API_PROMPT_VALUES,
								mValues);
						setResult(RESULT_OK, intent);
						finish();
					}
				});
		Analytics.trackActivity(this);
		setResult(RESULT_CANCELED);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			Intent intent = new Intent();
			intent.putExtra(Constants.EXTRA_API_PROMPT_RPC_NAME,
					mRpc.getName());
			intent.putExtra(Constants.EXTRA_API_PROMPT_VALUES,
					mValues);
			setResult(RESULT_OK, intent);
			finish();
		}
		return true;
	}

	private class ApiPromptAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return mHints.length;
		}

		@Override
		public Object getItem(int position) {
			return mValues[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View item;
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				item = inflater
						.inflate(R.layout.api_prompt_item, parent, false);
			} else {
				item = convertView;
			}
			TextView description = (TextView) item
					.findViewById(R.id.api_prompt_item_description);
			EditText value = (EditText) item
					.findViewById(R.id.api_prompt_item_value);
			description.setText(mHints[position]);
			value.setText(mValues[position]);
			value.addTextChangedListener(new ValueWatcher(position));
			return item;
		}
	}

	private class ValueWatcher implements TextWatcher {
		private final int mPosition;

		public ValueWatcher(int position) {
			mPosition = position;
		}

		@Override
		public void afterTextChanged(Editable e) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int before,
				int count) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			mValues[mPosition] = s.toString();
		}
	}
}
