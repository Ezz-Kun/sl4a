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

import static com.hipipal.texteditor.common.FileUtils.copyFile;
import static com.hipipal.texteditor.common.FileUtils.createFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import python.listview.JazzyHelper;
import python.listview.JazzyListView;
import zce.app.crash.CrashEmail;
import zce.app.sdpath.GetPath;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.googlecode.android_scripting.ActivityFlinger;
import com.googlecode.android_scripting.Analytics;
import com.googlecode.android_scripting.BaseApplication;
import com.googlecode.android_scripting.Constants;
import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.IntentBuilders;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.R;
import com.googlecode.android_scripting.ScriptListAdapter;
import com.googlecode.android_scripting.ScriptStorageAdapter;
import com.googlecode.android_scripting.dialog.Decompile;
import com.googlecode.android_scripting.dialog.Help;
import com.googlecode.android_scripting.dialog.UsageTrackingConfirmation;
import com.googlecode.android_scripting.facade.FacadeConfiguration;
import com.googlecode.android_scripting.interpreter.Interpreter;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration.ConfigurationObserver;

import de.keyboardsurfer.mobile.app.android.widget.crouton.Crouton;
import de.keyboardsurfer.mobile.app.android.widget.crouton.Style;

/**
 * Manages creation, deletion, and execution of stored scripts.
 * 
 * @author Damon Kohler (damonkohler@gmail.com) changed ざ凍結の→愛
 *         (892768447@qq.com)
 */

@SuppressLint("DefaultLocale")
public class ScriptManager extends ActionBarActivity {

	private final static String EMPTY = "";
	private List<File> mScripts;
	private JazzyListView mList;
	private ScriptManagerAdapter mAdapter;
	private SharedPreferences mPreferences;
	private HashMap<Integer, Interpreter> mAddMenuIds;
	private ScriptListObserver mObserver;
	private InterpreterConfiguration mConfiguration;
	// private SearchManager mManager;
	private boolean mInSearchResultMode = false;
	private String mQuery = EMPTY;
	private File mCurrentDir;
	private File mBaseDir;
	private final Handler mHandler = new Handler();
	private File mCurrent;
	private final int[] effects = { JazzyHelper.STANDARD, JazzyHelper.GROW,
			JazzyHelper.CARDS, JazzyHelper.CURL, JazzyHelper.WAVE,
			JazzyHelper.FLIP, JazzyHelper.FLY, JazzyHelper.REVERSE_FLY,
			JazzyHelper.HELIX, JazzyHelper.FAN, JazzyHelper.TILT,
			JazzyHelper.ZIPPER, JazzyHelper.FADE, JazzyHelper.TWIRL,
			JazzyHelper.SLIDE_IN };

	private static enum MenuId {
		BUILD, DECOMPILE, DELETE, HELP, FOLDER_ADD, QRCODE_ADD, INTERPRETER_MANAGER, PREFERENCES, LOGCAT_VIEWER, TRIGGER_MANAGER, REFRESH, SEARCH, RENAME, EXTERNAL;
		public int getId() {
			return ordinal() + Menu.FIRST;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.script_manager);
		setTitle("sl4a");
		File sl4a = null;
		mBaseDir = new File(new GetPath().path(this) + "/sl4a/scripts/");
		Log.i("mBaseDir: " + mBaseDir.getAbsolutePath());
		sl4a = mBaseDir.getParentFile();
		if (!sl4a.exists()) {
			sl4a.mkdirs();
			try {
				FileUtils.chmod(sl4a, 0755);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		File tfile = new File(mBaseDir.getAbsolutePath() + "/template/");
		if (!tfile.exists()) {
			tfile.mkdirs();// 创建模版路径
		}
		if (!FileUtils.makeDirectories(mBaseDir, 0755)) {
			new AlertDialog.Builder(this)
					.setTitle("Error")
					.setMessage(
							getString(R.string.s_Failedtocreate) + "\n"
									+ mBaseDir + "\n"
									+ getString(R.string.s_PleaseCheck))
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setPositiveButton("Ok", null).show();
		}

		mCurrentDir = mBaseDir;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mAdapter = new ScriptManagerAdapter(this);
		mObserver = new ScriptListObserver();
		mAdapter.registerDataSetObserver(mObserver);
		mConfiguration = ((BaseApplication) getApplication())
				.getInterpreterConfiguration();
		// mManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		mList = (JazzyListView) findViewById(android.R.id.list);
		registerForContextMenu(mList);
		updateAndFilterScriptList(mQuery);
		mList.setAdapter(mAdapter);
		ActivityFlinger.attachView(mList, this);
		ActivityFlinger.attachView(getWindow().getDecorView(), this);
		startService(IntentBuilders.buildTriggerServiceIntent());
		UsageTrackingConfirmation.show(this);
		Analytics.trackActivity(this);
		setupJazziness(JazzyHelper.HELIX);
		mList.setOnItemClickListener(listItemClick);
		mList.setOnScrollListener(scrollListener);
		new CrashEmail(this,"892768447@qq.com");
	}

	protected OnScrollListener scrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_IDLE:
				// 当不滚动时
				// 判断是否滚动到底部
				if (mList.getLastVisiblePosition() == (mList.getCount() - 1)) {
					setupJazziness(getEffects());
				}
				// 判断是否滚动到顶部
				if (mList.getFirstVisiblePosition() == 1) {
					setupJazziness(getEffects());
				}
			}
		}

		@Override
		public void onScroll(AbsListView absListView, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
		}
	};

	protected OnItemClickListener listItemClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long id) {
			final File file = (File) mList.getItemAtPosition(position);
			mCurrent = file;
			if (file.isDirectory()) {
				mCurrentDir = file;
				mAdapter.notifyDataSetInvalidated();
				return;
			}
			if (FacadeConfiguration.getSdkLevel() <= 3
					|| !mPreferences.getBoolean("use_quick_menu", true)) {
				doDialogMenu();
				return;
			}

			final QuickAction actionMenu = new QuickAction(view);

			ActionItem terminal = new ActionItem();
			terminal.setIcon(getResources().getDrawable(R.drawable.terminal));
			terminal.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(ScriptManager.this,
							ScriptingLayerService.class);
					intent.setAction(Constants.ACTION_LAUNCH_FOREGROUND_SCRIPT);
					intent.putExtra(Constants.EXTRA_SCRIPT_PATH, file.getPath());
					startService(intent);
					dismissQuickActions(actionMenu);
				}
			});

			final ActionItem background = new ActionItem();
			background.setIcon(getResources()
					.getDrawable(R.drawable.background));
			background.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(ScriptManager.this,
							ScriptingLayerService.class);
					intent.setAction(Constants.ACTION_LAUNCH_BACKGROUND_SCRIPT);
					intent.putExtra(Constants.EXTRA_SCRIPT_PATH, file.getPath());
					startService(intent);
					dismissQuickActions(actionMenu);
				}
			});

			final ActionItem edit = new ActionItem();
			edit.setIcon(getResources().getDrawable(
					android.R.drawable.ic_menu_edit));
			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					editScript(file);
					dismissQuickActions(actionMenu);
				}
			});

			final ActionItem delete = new ActionItem();
			delete.setIcon(getResources().getDrawable(
					android.R.drawable.ic_menu_delete));
			delete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					delete(file);
					dismissQuickActions(actionMenu);
				}
			});

			final ActionItem rename = new ActionItem();
			rename.setIcon(getResources().getDrawable(
					android.R.drawable.ic_menu_save));
			rename.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					rename(file);
					dismissQuickActions(actionMenu);
				}
			});

			final ActionItem external = new ActionItem();
			external.setIcon(getResources().getDrawable(
					android.R.drawable.ic_menu_directions));
			external.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// externalEditor(file);
					dismissQuickActions(actionMenu);
					if (!file.getAbsolutePath().endsWith(".pyc")
							&& !file.isDirectory()) {
						// 非文件夹或者非pyc文件提示无法反编译
						Crouton.cancelAllCroutons();
						Crouton.showText(ScriptManager.this,
								getString(R.string.s_Cannotdecompile),
								Style.ALERT);
					} else {
						Decompile.show(ScriptManager.this,
								file.getAbsolutePath());
					}
				}
			});

			actionMenu.addActionItems(terminal, background, edit, rename,
					delete, external);
			actionMenu.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);
			actionMenu.show();
		}
	};

	private void setupJazziness(int effect) {
		mList.setTransitionEffect(effect);
	}

	@SuppressWarnings("serial")
	public void updateAndFilterScriptList(final String query) {
		List<File> scripts;
		if (mPreferences.getBoolean("show_all_files", false)) {
			scripts = ScriptStorageAdapter.listAllScripts(mCurrentDir);
		} else {
			scripts = ScriptStorageAdapter.listExecutableScripts(mCurrentDir,
					mConfiguration);
		}
		mScripts = Lists.newArrayList(Collections2.filter(scripts,
				new Predicate<File>() {
					@Override
					public boolean apply(File file) {
						return file.getName().toLowerCase()
								.contains(query.toLowerCase());
					}
				}));

		synchronized (mQuery) {
			if (!mQuery.equals(query)) {
				if (query == null || query.equals(EMPTY)) {
					// ((TextView) findViewById(R.id.left_text))
					// .setText("Scripts");
				} else {
					// ((TextView) findViewById(R.id.left_text)).setText(query);
				}
				mQuery = query;
			}
		}

		if (mScripts.size() == 0) {
			((TextView) findViewById(android.R.id.empty)).setText("");
		} else {
			((TextView) findViewById(android.R.id.empty)).setText("");
		}

		// TODO(damonkohler): Extending the File class here seems odd.
		if (!mCurrentDir.equals(mBaseDir)) {
			mScripts.add(0, new File(mCurrentDir.getParent()) {
				@Override
				public boolean isDirectory() {
					return true;
				}

				@Override
				public String getName() {
					return "..";
				}
			});
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, MenuId.RENAME.getId(), Menu.NONE,
				getString(R.string.s_Rename));
		menu.add(Menu.NONE, MenuId.DELETE.getId(), Menu.NONE,
				getString(R.string.s_Delete));
		menu.add(Menu.NONE, MenuId.DECOMPILE.getId(), Menu.NONE,
				getString(R.string.s_Decompile));
		menu.add(Menu.NONE, MenuId.BUILD.getId(), Menu.NONE,
				getString(R.string.s_Build));
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
		File file = (File) mAdapter.getItem(info.position);
		int itemId = item.getItemId();
		if (itemId == MenuId.DELETE.getId()) {
			delete(file);
			return true;
		} else if (itemId == MenuId.RENAME.getId()) {
			rename(file);
			return true;
		} else if (itemId == MenuId.DECOMPILE.getId()) {
			if (!file.getAbsolutePath().endsWith(".pyc") && !file.isDirectory()) {
				// 非文件夹或者非pyc文件提示无法反编译
				Crouton.cancelAllCroutons();
				Crouton.showText(this, getString(R.string.s_Cannotdecompile),
						Style.ALERT);
				return true;
			}
			Decompile.show(this, file.getAbsolutePath());
			return true;
		} else if (itemId == MenuId.BUILD.getId()) {
			// 打包项目
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mInSearchResultMode) {
				mInSearchResultMode = false;
				mAdapter.notifyDataSetInvalidated();
				return true;
			}
			if (!mCurrentDir.equals(mBaseDir)) {// 返回键返回上级目录
				mCurrent = mCurrent.getParentFile();
				mCurrentDir = mCurrent;
				mAdapter.notifyDataSetInvalidated();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onStop() {
		super.onStop();
		mConfiguration.unregisterObserver(mObserver);
	}

	@Override
	public void onStart() {
		super.onStart();
		mConfiguration.registerObserver(mObserver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!mInSearchResultMode || mScripts.isEmpty()) {
			((TextView) findViewById(android.R.id.empty))
					.setText(R.string.no_scripts_message);
		} else {
			((TextView) findViewById(android.R.id.empty)).setText("");
		}
		updateAndFilterScriptList(mQuery);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.script_menu, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.script_add),
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setActionProvider(menu.findItem(R.id.script_add),
				new AddActionProvider(this));// 自定义子菜单

		MenuItemCompat.setShowAsAction(menu.findItem(R.id.script_view),
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.script_setting),
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItem item = menu.findItem(R.id.script_search);
		SearchView searchView = new SearchView(this.getSupportActionBar()
				.getThemedContext());
		MenuItemCompat.setShowAsAction(item,
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setActionView(item, searchView);// 设置搜索窗口
		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				mInSearchResultMode = true;
				updateAndFilterScriptList(query);
				mAdapter.notifyDataSetChanged();
				return false;
			}

			@Override
			public boolean onQueryTextChange(String query) {
				mInSearchResultMode = true;
				updateAndFilterScriptList(query);
				mAdapter.notifyDataSetChanged();
				return false;
			}
		});
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.e("onPrepareOptionsMenu");
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.script_help) {
			Help.show(this);
		} else if (id == R.id.script_example) {
			InstallExampleDialog installExampleDialog = new InstallExampleDialog(
					this, this, new AlertDialog.Builder(this));
			installExampleDialog.setDestPath(mBaseDir.getAbsolutePath());
			installExampleDialog.start();// 解压例子
		} else if (id == R.id.script_about) {
			startActivity(new Intent(this, AboutActivity.class));
		} else if (id == R.id.script_interpreters) {
			// Show interpreter manger.
			startActivity(new Intent(this, InterpreterManager.class));
		} else if (id == R.id.script_preferences) {
			startActivity(new Intent(this, Preferences.class));
		} else if (id == R.id.script_triggers) {
			startActivity(new Intent(this, TriggerManager.class));
		} else if (id == R.id.script_logcat) {
			startActivity(new Intent(this, LogcatViewer.class));
		} else if (id == R.id.script_refresh) {
			updateAndFilterScriptList(mQuery);
			mAdapter.notifyDataSetChanged();
		}
		return true;
	}

	private void menuItemClick(int itemId) {
		if (itemId == MenuId.FOLDER_ADD.getId()) {
			addFolder();
			return;
		}
		if (mAddMenuIds.containsKey(itemId)) {
			// Add a new script.
			newfile(itemId);
			// Intent intent = new Intent(Constants.ACTION_EDIT_SCRIPT);
			// Interpreter interpreter = mAddMenuIds.get(itemId);
			// intent.putExtra(Constants.EXTRA_SCRIPT_PATH,
			// new File(mCurrentDir.getPath(), interpreter.getExtension())
			// .getPath());
			// intent.putExtra(Constants.EXTRA_SCRIPT_CONTENT,
			// interpreter.getContentTemplate());
			// intent.putExtra(Constants.EXTRA_IS_NEW_SCRIPT, true);
			// startActivity(intent);
			// synchronized (mQuery) {
			// mQuery = EMPTY;
			// }
		}
	}

	// Quickedit chokes on sdk 3 or below, and some Android builds. Provides
	// alternative menu.
	private void doDialogMenu() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final CharSequence[] menuList = { getString(R.string.s_RunForeground),
				getString(R.string.s_RunBackground),
				getString(R.string.s_Edit), getString(R.string.s_Delete),
				getString(R.string.s_Rename),
				getString(R.string.s_ExternalEditor) };
		builder.setTitle(mCurrent.getName());
		builder.setItems(menuList, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent;
				switch (which) {
				case 0:
					intent = new Intent(ScriptManager.this,
							ScriptingLayerService.class);
					intent.setAction(Constants.ACTION_LAUNCH_FOREGROUND_SCRIPT);
					intent.putExtra(Constants.EXTRA_SCRIPT_PATH,
							mCurrent.getPath());
					startService(intent);
					break;
				case 1:
					intent = new Intent(ScriptManager.this,
							ScriptingLayerService.class);
					intent.setAction(Constants.ACTION_LAUNCH_BACKGROUND_SCRIPT);
					intent.putExtra(Constants.EXTRA_SCRIPT_PATH,
							mCurrent.getPath());
					startService(intent);
					break;
				case 2:
					editScript(mCurrent);
					break;
				case 3:
					delete(mCurrent);
					break;
				case 4:
					rename(mCurrent);
					break;
				case 5:
					// if (!mCurrent.getAbsolutePath().endsWith(".pyc")
					// && !mCurrent.isDirectory()) {
					// // 非文件夹或者非pyc文件提示无法反编译
					// Crouton.cancelAllCroutons();
					// Crouton.showText(ScriptManager.this,
					// getString(R.string.s_Cannotdecompile),
					// Style.ALERT);
					// } else {
					// Decompile.show(ScriptManager.this,
					// mCurrent.getAbsolutePath());
					// }
					externalEditor(mCurrent);
					break;
				}
			}
		});
		builder.show();
	}

	protected void externalEditor(File file) {
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setDataAndType(Uri.fromFile(file), "text/plain");
		try {
			startActivity(intent);
		} catch (Exception e) {
			Crouton.showText(this,
					getString(R.string.s_UnableOpeneditor) + e.toString(),
					Style.ALERT);
		}
	}

	private void dismissQuickActions(final QuickAction action) {
		// HACK(damonkohler): Delay the dismissal to avoid an otherwise
		// noticeable flicker.
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				action.dismiss();
			}
		}, 1);
	}

	/**
	 * Opens the script for editing.
	 * 
	 * @param script
	 *            the name of the script to edit
	 */
	private void editScript(File script) {
		Intent i = new Intent(Constants.ACTION_EDIT_SCRIPT);
		i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		i.putExtra(Constants.EXTRA_SCRIPT_PATH, script.getAbsolutePath());
		startActivity(i);
	}

	private void delete(final File file) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Delete");
		alert.setMessage(getString(R.string.s_Woulddelete) + file.getName()
				+ "?");
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				FileUtils.delete(file);
				mScripts.remove(file);
				mAdapter.notifyDataSetChanged();
			}
		});
		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Ignore.
			}
		});
		alert.show();
	}

	private void addFolder() {
		final EditText folderName = new EditText(this);
		folderName.setHint(getString(R.string.s_FolderName));
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.s_AddFolder));
		alert.setView(folderName);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String name = folderName.getText().toString();
				if (name.length() == 0) {
					Log.e(ScriptManager.this, getString(R.string.s_Nameempty));
					return;
				} else {
					for (File f : mScripts) {
						if (f.getName().equals(name)) {
							Log.e(ScriptManager.this, String.format(
									getString(R.string.s_Exists), name));
							return;
						}
					}
				}
				File dir = new File(mCurrentDir, name);
				if (!FileUtils.makeDirectories(dir, 0755)) {
					Log.e(ScriptManager.this, String.format(
							getString(R.string.s_Cannot_create), name));
				}
				mAdapter.notifyDataSetInvalidated();
			}
		});
		alert.show();
	}

	private void newfile(final int itemId) {
		final EditText newName = new EditText(this);
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.s_New_File));
		alert.setView(newName);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Interpreter interpreter = mAddMenuIds.get(itemId);
				String ext = interpreter.getExtension();
				String name = newName.getText().toString();
				if (name.length() == 0) {
					Log.e(ScriptManager.this, getString(R.string.s_Nameempty));
					return;
				} else {
					for (File f : mScripts) {
						if (f.getName().equals(name)) {
							Crouton.showText(ScriptManager.this, String.format(
									getString(R.string.s_Exists), name),
									Style.ALERT);
							return;
						}
					}
				}
				if (name.endsWith(ext)) {
					// name = name.replace(ext, "");//去掉后缀
				} else {
					name = name + ext;
				}
				// 检测pyevet是否存在,不存在则复制
				File pyevent = new File(mCurrentDir, "pyevent.pyc");
				if (!pyevent.exists()) {
					try {
						copyFile(getAssets().open("pyevent.pyc"), pyevent);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				pyevent = null;
				if (!createFile(mCurrentDir, name)) {
					// 创建文件
					Crouton.showText(ScriptManager.this,
							getString(R.string.s_Cannot_create) + name,
							Style.ALERT);
					return;
				}
				mAdapter.notifyDataSetInvalidated();
				Intent intent = new Intent(Constants.ACTION_EDIT_SCRIPT);
				intent.putExtra(Constants.EXTRA_SCRIPT_PATH, new File(
						mCurrentDir.getPath(), name).getPath());
				intent.putExtra(Constants.EXTRA_SCRIPT_CONTENT,
						interpreter.getContentTemplate());
				intent.putExtra(Constants.EXTRA_IS_NEW_SCRIPT, true);
				startActivity(intent);
				synchronized (mQuery) {
					mQuery = EMPTY;
				}
			}
		});
		alert.show();
	}

	private void rename(final File file) {
		final EditText newName = new EditText(this);
		newName.setText(file.getName());
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.s_Rename));
		alert.setView(newName);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String name = newName.getText().toString();
				if (name.length() == 0) {
					Log.e(ScriptManager.this, getString(R.string.s_Nameempty));
					return;
				} else {
					for (File f : mScripts) {
						if (f.getName().equals(name)) {
							Log.e(ScriptManager.this, String.format(
									getString(R.string.s_Exists), name));
							return;
						}
					}
				}
				if (!FileUtils.rename(file, name)) {
					throw new RuntimeException(String.format(
							getString(R.string.s_Cannotrename), file.getPath()));
				}
				mAdapter.notifyDataSetInvalidated();
			}
		});
		alert.show();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mConfiguration.unregisterObserver(mObserver);
		// mManager.setOnCancelListener(null);
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(1);
	}

	public int getEffects() {
		return effects[(int) (Math.random() * effects.length)];
	}

	public void updatemAdapter() {
		mAdapter.notifyDataSetChanged();
	}

	private void buildMenuIdMaps() {
		mAddMenuIds = new LinkedHashMap<Integer, Interpreter>();
		int i = MenuId.values().length + Menu.FIRST;
		List<Interpreter> installed = mConfiguration.getInstalledInterpreters();
		Collections.sort(installed, new Comparator<Interpreter>() {
			@Override
			public int compare(Interpreter interpreterA,
					Interpreter interpreterB) {
				return interpreterA.getNiceName().compareTo(
						interpreterB.getNiceName());
			}
		});
		for (Interpreter interpreter : installed) {
			mAddMenuIds.put(i, interpreter);
			++i;
		}
	}

	private void buildAddMenu(SubMenu subMenu) {
		subMenu.add(getString(R.string.s_Folder)).setOnMenuItemClickListener(
				new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						menuItemClick(MenuId.FOLDER_ADD.getId());
						return false;
					}
				});
		for (final Entry<Integer, Interpreter> entry : mAddMenuIds.entrySet()) {
			subMenu.add(entry.getValue().getNiceName())
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							menuItemClick(entry.getKey());
							return false;
						}
					});

		}
	}

	private class AddActionProvider extends ActionProvider {

		public AddActionProvider(Context context) {
			super(context);
		}

		@Override
		public View onCreateActionView() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void onPrepareSubMenu(SubMenu subMenu) {
			// TODO Auto-generated method stub
			subMenu.clear();
			buildMenuIdMaps();
			buildAddMenu(subMenu);
			super.onPrepareSubMenu(subMenu);
		}

		@Override
		public boolean hasSubMenu() {
			// TODO Auto-generated method stub
			return true;
		}
	}

	private class ScriptListObserver extends DataSetObserver implements
			ConfigurationObserver {
		@Override
		public void onInvalidated() {
			updateAndFilterScriptList(EMPTY);
		}

		@Override
		public void onConfigurationChanged() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateAndFilterScriptList(mQuery);
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	private class ScriptManagerAdapter extends ScriptListAdapter {
		public ScriptManagerAdapter(Context context) {
			super(context);
		}

		@Override
		protected List<File> getScriptList() {
			return mScripts;
		}
	}
}
