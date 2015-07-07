package com.hipipal.texteditor;

import static com.hipipal.texteditor.common.TextFileUtils.readTextFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import zce.app.sdpath.GetPath;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.R;

import de.keyboardsurfer.mobile.app.android.widget.crouton.Crouton;
import de.keyboardsurfer.mobile.app.android.widget.crouton.Style;

public class TemplateActivity extends ActionBarActivity {

	private ListView listview;
	private Animation expandall;
	private Animation collapseall;
	private ListAsyncTask listTask;
	private boolean isRun;
	private boolean isPause;
	private Set<Integer> mExpandedPositions;
	private List<TemplateItem> templateList;
	private TArrayAdapter templateAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.template_layout);
		getSupportActionBar().setTitle("Template");
		getSupportActionBar().setHomeButtonEnabled(true);// 决定左上角的图标是否可以点击
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);// 给左上角图标的左边加上一个返回的图标
		Crouton.showText(this, R.string.s_Longtouchtoinsert, Style.INFO);

		isRun = false;// 线程未运行
		isPause = false;// Activity 是否pause

		initView();

		getTemplates();
	}

	@Override
	protected void onDestroy() {
		Log.i("onDestroy");
		super.onDestroy();
	}

	@Override
	public void finish() {
		Log.i("finish");
		isPause = true;// 当程序进入后台时
		if (listTask != null) {
			// 如果线程在运行则停止
			isRun = false;
		}
		super.finish();
	}

	@Override
	protected void onPause() {
		Log.i("onPause");
		isPause = true;// 当程序进入后台时
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.i("onResume");
		isPause = false;
		super.onResume();
	}

	// @Override
	// public boolean onKeyUp(int keyCode, KeyEvent event) {
	// if (keyCode == KeyEvent.KEYCODE_BACK) {
	// isPause = true;
	// if (listTask != null) {
	// // 如果线程在运行则停止
	// isRun = false;
	// }
	// }
	// return super.onKeyUp(keyCode, event);
	// }

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
			listview.startAnimation(expandall);
			mExpandedPositions.clear();
			for (int i = 0; i < templateList.size(); i++) {
				mExpandedPositions.add(i);
			}
		} else if (id == R.id.collapseall) {
			listview.startAnimation(collapseall);
			mExpandedPositions.clear();
		}
		templateAdapter.notifyDataSetInvalidated();
		return true;
	}

	private void initView() {
		listview = (ListView) findViewById(android.R.id.list);
		templateList = new ArrayList<TemplateItem>();
		mExpandedPositions = new HashSet<Integer>();
		templateAdapter = new TArrayAdapter(this,
				R.layout.simple_dropdown_item, templateList);
		listview.setAdapter(templateAdapter);
		listview.setOnItemClickListener(itemClickListener);
		listview.setOnItemLongClickListener(itemLongClickListener);
		collapseall = new RotateAnimation(+360f, 0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		collapseall.setDuration(200);
		collapseall.setFillAfter(true);
		expandall = new RotateAnimation(0f, +360f, Animation.RELATIVE_TO_SELF,
				0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		expandall.setDuration(200);
		expandall.setFillAfter(true);
	}

	/**
	 * 获取模版
	 */
	private void getTemplates() {
		if (listTask == null) {
			// 如果是第一次则初始化
			templateList.clear();
			templateAdapter.clear();
			mExpandedPositions.clear();
			listTask = new ListAsyncTask();
			isRun = true;// 线程运行
			listTask.execute("");
			Log.i("listTask start");
		}
	}

	/**
	 * 模版数据展开与关闭
	 */
	private OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adv, View view, int position,
				long id) {
			if (mExpandedPositions.contains(position)) {
				mExpandedPositions.remove(position);
			} else {
				mExpandedPositions.add(position);
			}
			templateAdapter.notifyDataSetInvalidated();
		}
	};

	/**
	 * 长按插入数据
	 */
	private OnItemLongClickListener itemLongClickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> adv, View view,
				int position, long id) {
			Intent intent = new Intent();
			intent.putExtra("template", templateList.get(position).toString());
			setResult(RESULT_OK, intent);
			// Log.i(templateList.get(position).toString());
			finish();
			return false;
		}
	};

	private class TArrayAdapter extends ArrayAdapter<TemplateItem> {

		private TArrayAdapter(Context context, int resource,
				List<TemplateItem> object) {
			super(context, resource, object);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = (TextView) super.getView(position, convertView,
					parent);
			if (mExpandedPositions.contains(position)) {
				// view.setHeight(100);
				// view.setLayoutParams(params);
				String context = getItem(position).toString();
				if (context.length() > 500) {
					view.setText(context.subSequence(0, 500));// 只显示一部分
				} else {
					view.setText(context);
				}
			} else {
				// view.setHeight(height);
				view.setText(getItem(position).getName());
			}
			return view;
		}
	}

	public class ListAsyncTask extends AsyncTask<String, String, Void> {

		/**
		 * 获取模版
		 */
		@Override
		protected Void doInBackground(String... arg) {
			File path = new File(new GetPath().path(getApplicationContext())
					.getAbsolutePath()
					+ File.separator
					+ "sl4a/scripts/template/");
			if (!path.exists()) {
				return null;
			}
			for (File file : path.listFiles()) {
				if (file.isFile() && file.getName().endsWith(".py")) {
					String text = readTextFile(file);
					if (text != null) {
						templateList
								.add(new TemplateItem(file.getName(), text));
					}
				}
			}
			while (isRun) {
				if (isPause) {
					Log.i("isPause");
					// 如果Activity进入后台了,不更新界面
					continue;
				} else {
					Log.i("not Pause");
					// 否则更新界面
					publishProgress("");
				}
			}
			return null;
		}

		/**
		 * 进度显示
		 */
		@Override
		protected void onProgressUpdate(String... values) {
			templateAdapter.notifyDataSetChanged();
			isRun = false;
			super.onProgressUpdate(values);
		}

		/**
		 * 完成
		 */
		@Override
		protected void onPostExecute(Void result) {
			if (templateList.isEmpty()) {
				Crouton.showText(TemplateActivity.this,
						R.string.s_NotFindTemplate, Style.ALERT);
			}
			super.onPostExecute(result);
		}

	}

}
