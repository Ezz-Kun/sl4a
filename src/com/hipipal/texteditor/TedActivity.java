package com.hipipal.texteditor;

import static com.hipipal.texteditor.common.FileUtils.deleteItem;
import static com.hipipal.texteditor.common.FileUtils.getCanonizePath;
import static com.hipipal.texteditor.common.FileUtils.renameItem;
import static com.hipipal.texteditor.common.TextFileUtils.readTextFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.biovamp.widget.CircleLayout;
import zce.app.sdpath.GetPath;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.googlecode.android_scripting.Analytics;
import com.googlecode.android_scripting.BaseApplication;
import com.googlecode.android_scripting.BuildConfig;
import com.googlecode.android_scripting.Constants;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.R;
import com.googlecode.android_scripting.activity.ApiBrowser;
import com.googlecode.android_scripting.activity.ScriptingLayerService;
import com.googlecode.android_scripting.interpreter.Interpreter;
import com.googlecode.android_scripting.interpreter.InterpreterConfiguration;
import com.hipipal.texteditor.common.ColorPicker;
import com.hipipal.texteditor.common.Settings;
import com.hipipal.texteditor.common.TextFileUtils;
import com.hipipal.texteditor.ui.view.AdvancedEditText;
import com.hipipal.texteditor.ui.view.ToolRelativeLayout;
import com.hipipal.texteditor.undo.TextChangeWatcher;

import de.keyboardsurfer.mobile.app.android.widget.crouton.Crouton;
import de.keyboardsurfer.mobile.app.android.widget.crouton.Style;

public class TedActivity extends ActionBarActivity implements Constants,
		OnClickListener, TextWatcher {

	/**
	 * 文本编辑器
	 */
	protected AdvancedEditText mEditor;
	/**
	 * 当前打开文件的路径
	 */
	protected String mCurrentFilePath;
	/**
	 * 当前打开的文件的名称
	 */
	protected String mCurrentFileName;
	/**
	 * is dirty ?
	 */
	protected boolean mDirty;
	/**
	 * 是否只读?
	 */
	protected boolean mReadOnly;
	/**
	 * 搜索布局
	 */
	protected LinearLayout mSearchLayout;
	/**
	 * 搜索输入
	 */
	protected EditText mSearchInput;
	/**
	 * 工具按钮
	 */
	protected ImageButton toolBtn;
	/**
	 * 圆形工具菜单
	 */
	protected CircleLayout toolLayout;
	/**
	 * 撤销监视器
	 */
	protected TextChangeWatcher mWatcher;
	protected boolean mInUndo;
	protected boolean mWarnedShouldQuit;
	protected static Dialog dialog;
	protected InterpreterConfiguration mConfiguration;

	protected Pattern pattern = Pattern.compile("\\s?line\\s?(\\d+),");

	private static enum RequestCode {
		RPC_HELP, TEMPLATE
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportActionBar().setHomeButtonEnabled(true);// 决定左上角的图标是否可以点击
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);// 给左上角图标的左边加上一个返回的图标

		Settings.updateFromPreferences(getSharedPreferences(
				com.hipipal.texteditor.common.Constants.PREFERENCES_NAME,
				MODE_PRIVATE));// SharedPreferences轻量级存储

		initEdit();
		initLayout();

		mWarnedShouldQuit = false;
		mWatcher = new TextChangeWatcher();// 初始化撤销监视器
		mConfiguration = ((BaseApplication) getApplication())
				.getInterpreterConfiguration();
		Analytics.trackActivity(this);
		doIntent(getIntent());
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Log.i("onResume");
		updateTitle();
		if (mCurrentFilePath != null && mCurrentFilePath.endsWith(".py")) {
			mEditor.updateFromSettings("py");
		} else {
			mEditor.updateFromSettings("");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (Settings.FORCE_AUTO_SAVE && mDirty && (!mReadOnly)) {
			if ((mCurrentFilePath == null) || (mCurrentFilePath.length() == 0)) {
				doAutoSaveFile(true);
			} else if (Settings.AUTO_SAVE_OVERWRITE) {
				doSaveFile(mCurrentFilePath, true);
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public void afterTextChanged(Editable s) {
		if (!mDirty) {
			mDirty = true;
			updateTitle();
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		if (Settings.UNDO && (!mInUndo) && (mWatcher != null))
			mWatcher.beforeChange(s, start, count, after);
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if (mInUndo) {
			return;
		}
		if (Settings.UNDO && (!mInUndo) && (mWatcher != null)) {
			mWatcher.afterChange(s, start, before, count);
		}

	}

	@Override
	public void onClick(View v) {
		mWarnedShouldQuit = false;
		int id = v.getId();
		if (id == R.id.buttonSearchClose) {
			// 关闭搜索栏
			search();
		} else if (id == R.id.buttonSearchNext) {
			searchNextOrPre(false);
		} else if (id == R.id.buttonSearchPrev) {
			searchNextOrPre(true);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (toolLayout.getVisibility() != LinearLayout.GONE) {
				toolLayout.setVisibility(LinearLayout.GONE);// 隐藏工具
				toolBtn.setVisibility(LinearLayout.VISIBLE);// 显示拖动工具按钮
			} else if (mSearchLayout.getVisibility() != LinearLayout.GONE) {
				search();
			} else if (mDirty) {
				mWarnedShouldQuit = true;
				warnOrQuit();
			} else {
				quit();
			}
			return true;// 处理到这里直接返回
		case KeyEvent.KEYCODE_SEARCH:
			search();
			mWarnedShouldQuit = false;
			return true;
		}
		mWarnedShouldQuit = false;
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.editor_menu, menu);
		MenuItemCompat.setShowAsAction(menu.findItem(R.id.menu_back),
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);// 撤销
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mWarnedShouldQuit = false;
		// super.onOptionsItemSelected(item);
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();// 左上角图标
		} else if (id == R.id.menu_back) {
			if (!undo()) {
				Toast.makeText(getApplicationContext(),
						R.string.toast_warn_no_undo, Toast.LENGTH_SHORT).show();
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		RequestCode request = RequestCode.values()[requestCode];
		if (resultCode == RESULT_OK) {
			switch (request) {
			case RPC_HELP:
				String rpcText = data
						.getStringExtra(Constants.EXTRA_RPC_HELP_TEXT);
				insertContent(rpcText);
				break;
			case TEMPLATE:
				Log.i("TEMPLATE");
				String templateText = data.getStringExtra("template");
				Log.i(templateText);
				if (templateText != null) {
					mEditor.getText().insert(mEditor.getSelectionStart(),
							templateText);// 插入
				}
			default:
				break;
			}
		} else {
			switch (request) {
			case RPC_HELP:
				break;
			default:
				break;
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		doIntent(intent);
	}

	/**
	 * 设置按钮
	 * 
	 * @param v
	 */
	public void onSetting(View v) {
		toolLayout.setVisibility(LinearLayout.GONE);
		try {
			startActivity(new Intent(TedActivity.this, SettingsActivity.class));
		} catch (Exception e) {
			Crouton.showText(this, e.getMessage(), Style.ALERT);
		}
	}

	/**
	 * 颜色插入按钮
	 * 
	 * @param v
	 */
	public void onColor(View v) {
		toolLayout.setVisibility(LinearLayout.GONE);
		ColorPicker cp = new ColorPicker(this, new ColorListener(), "edittext",
				getString(R.string.insert_color), Color.GREEN);
		cp.show();
	}

	/**
	 * 模版按钮
	 * 
	 * @param v
	 */
	@SuppressLint("InflateParams")
	public void onTemplate(View v) {
		// 调用模版Activity
		toolLayout.setVisibility(LinearLayout.GONE);
		try {
			Intent intent = new Intent(TedActivity.this, TemplateActivity.class);
			startActivityForResult(intent, RequestCode.TEMPLATE.ordinal());
		} catch (Exception e) {
			Crouton.showText(this, e.getMessage(), Style.ALERT);
		}
	}

	/**
	 * 右缩进按钮
	 * 
	 * @param v
	 */
	// public void onRight(View v) {
	// int startSelection = mEditor.getSelectionStart();
	// int endSelection = mEditor.getSelectionEnd();
	// String selectedText = mEditor.getText().toString()
	// .substring(startSelection, endSelection);
	// Editable editable = mEditor.getText();
	// if (selectedText.length() != 0) {
	// String startData = mEditor.getText().toString();
	// String textData = startData.substring(0, startSelection);
	// if (textData.contains("\n")) {
	// int newLineIndex = textData.lastIndexOf("\n");
	// editable.replace(newLineIndex, newLineIndex, "\n    ");
	// } else {
	// editable.insert(0, "    ");
	// }
	// editable.replace(startSelection, endSelection,
	// selectedText.replace("\n", "\n    "));
	// } else {
	// editable.insert(mEditor.getSelectionStart(), "    ");
	// }
	// }

	/**
	 * api查看按钮
	 * 
	 * @param v
	 */
	public void onApi(View v) {
		toolLayout.setVisibility(LinearLayout.GONE);
		try {
			Intent intent = new Intent(TedActivity.this, ApiBrowser.class);
			intent.putExtra(Constants.EXTRA_SCRIPT_PATH, mCurrentFileName);
			intent.putExtra(Constants.EXTRA_INTERPRETER_NAME, mConfiguration
					.getInterpreterForScript(mCurrentFileName).getName());
			intent.putExtra(Constants.EXTRA_SCRIPT_TEXT, mEditor.getText()
					.toString());
			startActivityForResult(intent, RequestCode.RPC_HELP.ordinal());
		} catch (Exception e) {
			Crouton.showText(this, e.getMessage(), Style.ALERT);
		}
	}

	/**
	 * 保存按钮
	 * 
	 * @param v
	 */
	public void onSave(View v) {
		toolLayout.setVisibility(LinearLayout.GONE);
		saveContent();// 保存文件
	}

	/**
	 * 运行按钮
	 * 
	 * @param v
	 */
	public void onPlay(View v) {
		toolLayout.setVisibility(LinearLayout.GONE);
		doSaveFile(mCurrentFilePath, false);
		Interpreter interpreter = mConfiguration
				.getInterpreterForScript(mCurrentFileName);
		if (interpreter != null) { // We may be editing an unknown type.
			Intent intent = new Intent(TedActivity.this,
					ScriptingLayerService.class);
			intent.setAction(Constants.ACTION_LAUNCH_FOREGROUND_SCRIPT);
			intent.putExtra(Constants.EXTRA_SCRIPT_PATH, mCurrentFilePath);
			startService(intent);
		} else {
			Crouton.cancelAllCroutons();
			Crouton.showText(this, R.string.s_Cannotrun, Style.ALERT);
		}
		// finish();
	}

	/**
	 * 搜索按钮
	 * 
	 * @param v
	 */
	public void onSearch(View v) {
		toolLayout.setVisibility(LinearLayout.GONE);
		search();
	}

	public void onTool() {
		if (toolLayout.getVisibility() != LinearLayout.GONE) {
			toolLayout.setVisibility(LinearLayout.GONE);// 隐藏
		} else {
			toolLayout.setVisibility(LinearLayout.VISIBLE);// 显示
		}
	}

	public File getPath() {
		return new GetPath().path(getApplicationContext());
	}

	/**
	 * Intent 处理
	 * 
	 * @param intent
	 */
	protected void doIntent(Intent intent) {
		if (mCurrentFilePath == null) {
			String path = intent.getStringExtra(Constants.EXTRA_SCRIPT_PATH);
			if (path != null) {
				if (intent
						.getBooleanExtra(Constants.EXTRA_IS_NEW_SCRIPT, false)) {
					// 新文件
					doOpenFile(new File(path), false);
					// 插入开头代码
					mEditor.getText()
							.insert(0,
									intent.getStringExtra(Constants.EXTRA_SCRIPT_CONTENT));
				} else {
					doOpenFile(new File(path), false);
				}
			}
		}
		String error = intent.getStringExtra(Constants.EXTRA_SCRIPT_ERROR);
		if (error != null) {// 处理错误行数
			Matcher matcher = pattern.matcher(error);
			if (matcher.find()) {
				try {
					String line = matcher.group(1).toString();
					Crouton.showText(this, error, Style.ALERT);
					if (BuildConfig.DEBUG) {
						Log.i(line);
					}
					if (line != null) {
						goToLine(Integer.parseInt(line));
					}
				} catch (Exception e) {
					e.printStackTrace();// 数组越界
				}
			}
		}
	}

	/**
	 * 通过api浏览插入文本
	 * 
	 * @param text
	 */
	protected void insertContent(String text) {
		int selectionStart = Math.min(mEditor.getSelectionStart(),
				mEditor.getSelectionEnd());
		int selectionEnd = Math.max(mEditor.getSelectionStart(),
				mEditor.getSelectionEnd());
		mEditor.getEditableText().replace(selectionStart, selectionEnd, text);
	}

	protected void doClearContents() {
		mWatcher = null;
		mInUndo = true;
		mEditor.setText("");
		mCurrentFilePath = null;
		mCurrentFileName = null;
		Settings.END_OF_LINE = Settings.DEFAULT_END_OF_LINE;
		mDirty = false;
		mReadOnly = false;
		mWarnedShouldQuit = false;
		mWatcher = new TextChangeWatcher();
		mInUndo = false;
		TextFileUtils.clearInternal(getApplicationContext());
		updateTitle();
	}

	protected boolean doOpenBackup() {
		String text;
		try {
			text = TextFileUtils.readInternal(this);
			if (!TextUtils.isEmpty(text)) {
				mInUndo = true;
				mEditor.setText(text);
				mWatcher = new TextChangeWatcher();
				mCurrentFilePath = null;
				mCurrentFileName = null;
				mDirty = false;
				mInUndo = false;
				mReadOnly = false;
				mEditor.setEnabled(true);
				updateTitle();
				return true;
			} else {
				return false;
			}
		} catch (OutOfMemoryError e) {
			Crouton.showText(this, R.string.toast_memory_open, Style.ALERT);
		}
		return true;
	}

	/**
	 * 更新标题
	 */
	protected void updateTitle() {
		String title;
		String name;
		name = "?";
		if ((mCurrentFileName != null) && (mCurrentFileName.length() > 0)) {
			name = mCurrentFileName;
		}
		if (mReadOnly) {
			title = getString(R.string.title_editor_readonly, name);
		} else if (mDirty) {
			title = getString(R.string.title_editor_dirty, name);
		} else {
			title = getString(R.string.title_editor, name);
		}
		setTitle(title);
	}

	/**
	 * 退出
	 */
	protected void quit() {
		if (!mDirty) {
			if (dialog != null) {
				dialog.dismiss();
			}
			finish();
		} else {
			createDialog();
			dialog.show();

		}
	}

	/**
	 * 是否要退出
	 */
	protected void warnOrQuit() {
		if (mWarnedShouldQuit) {
			quit();
		} else {
			Crouton.showText(this, R.string.toast_warn_no_undo_will_quit,
					Style.INFO);
			mWarnedShouldQuit = true;
		}
	}

	/**
	 * 判断是否可以撤销
	 * 
	 * @return
	 */
	protected boolean undo() {
		boolean didUndo = false;
		mInUndo = true;
		int caret;
		caret = mWatcher.undo(mEditor.getText());
		if (caret >= 0) {
			mEditor.setSelection(caret, caret);
			didUndo = true;
		}
		mInUndo = false;
		return didUndo;
	}

	/**
	 * 搜索框控制
	 */
	protected void search() {
		switch (mSearchLayout.getVisibility()) {
		case LinearLayout.GONE:
			mSearchLayout.setVisibility(LinearLayout.VISIBLE);
			break;
		case LinearLayout.VISIBLE:
		default:
			mSearchLayout.setVisibility(LinearLayout.GONE);
			break;
		}
	}

	/**
	 * false 往下搜索 true 往上搜索
	 * 
	 * @param pre
	 */
	@SuppressLint("DefaultLocale")
	protected void searchNextOrPre(boolean pre) {
		String search, text;
		int selection, next;
		search = mSearchInput.getText().toString();
		text = mEditor.getText().toString();
		if (pre) {// 往上搜索
			selection = mEditor.getSelectionStart() - 1;
		} else {// 往下搜索
			selection = mEditor.getSelectionEnd();
		}
		if (search.length() == 0) {
			Crouton.cancelAllCroutons();
			Crouton.showText(this, R.string.toast_search_no_input, Style.INFO);
			return;
		}
		if (!Settings.SEARCHMATCHCASE) {
			search = search.toLowerCase();
			text = text.toLowerCase();
		}
		if (pre) {
			next = text.lastIndexOf(search, selection);
		} else {
			next = text.indexOf(search, selection);
		}
		if (next > -1) {
			mEditor.setSelection(next, next + search.length());
			if (!mEditor.isFocused())
				mEditor.requestFocus();
		} else {
			if (Settings.SEARCHWRAP) {
				if (pre) {
					next = text.lastIndexOf(search);
				} else {
					next = text.indexOf(search);
				}
				if (next > -1) {
					mEditor.setSelection(next, next + search.length());
					if (!mEditor.isFocused())
						mEditor.requestFocus();
				} else {
					Crouton.cancelAllCroutons();
					Crouton.showText(this, R.string.toast_search_not_found,
							Style.INFO);
				}
			} else {
				Crouton.cancelAllCroutons();
				Crouton.showText(this, R.string.toast_search_eof, Style.INFO);
			}
		}
	}

	protected int NewLineIndex(int indexNewLine) {
		String data = mEditor.getText().toString();
		final StringBuffer sb = new StringBuffer(data);
		List<Integer> myList = new ArrayList<Integer>();
		myList.add(0);
		for (int i = 0; i < sb.length(); i++) {
			if (sb.charAt(i) == '\n')
				myList.add(i);
		}
		return myList.get(indexNewLine);
	}

	/**
	 * 跳转到指定行
	 */
	protected void goToLine(int line) {
		int lineCount = mEditor.getLineCount();
		if (line < lineCount) {
			mEditor.setSelection(NewLineIndex(line));
		}
	}

	/**
	 * 保存文件
	 */
	protected void saveContent() {
		if ((mCurrentFilePath == null) || (mCurrentFilePath.length() == 0)) {
			// 直接保存文件
		} else {
			doSaveFile(mCurrentFilePath, true);
		}
	}

	/**
	 * 打开文件
	 * 
	 * @param file
	 * @param forceReadOnly
	 * @return
	 */
	protected boolean doOpenFile(File file, boolean forceReadOnly) {
		String text;
		if (file == null)
			return false;
		try {
			text = readTextFile(file);
			if (text != null) {
				mInUndo = true;
				mEditor.setText(text);
				mWatcher = new TextChangeWatcher();
				mCurrentFilePath = getCanonizePath(file);
				mCurrentFileName = file.getName();
				mDirty = false;
				mInUndo = false;
				if (file.canWrite() && (!forceReadOnly)) {
					mReadOnly = false;
					mEditor.setEnabled(true);
				} else {
					mReadOnly = true;
					mEditor.setEnabled(false);
				}
				if (mCurrentFilePath != null
						&& mCurrentFilePath.endsWith(".py")) {
					mEditor.updateFromSettings("py");
				} else {
					mEditor.updateFromSettings("");
				}
				updateTitle();
				return true;
			} else {
				Crouton.showText(this, R.string.toast_open_error, Style.ALERT);
			}
		} catch (OutOfMemoryError e) {
			Crouton.showText(this, R.string.toast_memory_open, Style.ALERT);
		}
		return false;
	}

	/**
	 * 保存文件
	 * 
	 * @param path
	 * @param show
	 */
	protected void doSaveFile(String path, boolean show) {
		String content;
		if (path == null) {
			Crouton.showText(this, R.string.toast_save_null, Style.ALERT);
			return;
		}
		content = mEditor.getText().toString();
		if (!TextFileUtils.writeTextFile(path + ".tmp", content)) {
			Crouton.showText(this, R.string.toast_save_temp, Style.ALERT);
			return;
		}
		if (!deleteItem(path)) {
			Crouton.showText(this, R.string.toast_save_delete, Style.ALERT);
			return;
		}
		if (!renameItem(path + ".tmp", path)) {
			Crouton.showText(this, R.string.toast_save_rename, Style.ALERT);
			return;
		}
		mCurrentFilePath = getCanonizePath(new File(path));
		mCurrentFileName = (new File(path)).getName();
		mReadOnly = false;
		mDirty = false;
		updateTitle();
		if (mCurrentFilePath != null && mCurrentFilePath.endsWith(".py")) {
			mEditor.updateFromSettings("py");
		} else {
			mEditor.updateFromSettings("");
		}
		if (show) {
			Crouton.showText(this, R.string.toast_save_success, Style.CONFIRM);
		}
		// runAfterSave();
	}

	protected void doAutoSaveFile(boolean show) {
		String text = mEditor.getText().toString();
		if (text.length() == 0)
			return;
		if (TextFileUtils.writeInternal(this, text)) {
			if (show) {
				Crouton.showText(this, R.string.toast_file_saved_auto,
						Style.ALERT);
			}
		}
	}

	/**
	 * 初始化编辑器
	 */
	protected void initEdit() {
		// 编辑器
		mEditor = (AdvancedEditText) findViewById(R.id.editor);
		mEditor.addTextChangedListener(this);// 添加文本变化监听器
	}

	/**
	 * 初始化控件
	 */
	@SuppressLint("NewApi")
	protected void initLayout() {
		// 工具
		toolBtn = (ImageButton) findViewById(R.id.toolBtn);
		toolLayout = (CircleLayout) findViewById(R.id.toolLayout);
		// 搜索框
		mSearchLayout = (LinearLayout) findViewById(R.id.searchLayout);
		mSearchInput = (EditText) findViewById(R.id.textSearch);
		findViewById(R.id.buttonSearchClose).setOnClickListener(this);// 绑定搜索框关闭按钮事件
		findViewById(R.id.buttonSearchNext).setOnClickListener(this);// 绑定搜索框下一个按钮事件
		findViewById(R.id.buttonSearchPrev).setOnClickListener(this);// 绑定搜索框上一个按钮事件
		toolBtn.setOnTouchListener(new ToolOnTouch(this,
				(ToolRelativeLayout) findViewById(R.id.editor_layout)));
	}

	/**
	 * 创建对话框
	 */
	protected void createDialog() {
		dialog = new AlertDialog.Builder(TedActivity.this)
				.setTitle(R.string.notice)
				.setMessage(R.string.ui_save_text)
				.setPositiveButton(R.string.ui_save,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								saveContent();
								finish();
							}
						})
				.setNegativeButton(R.string.ui_cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						})
				.setNegativeButton(R.string.ui_no_save,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (dialog != null) {
									dialog.dismiss();
								}
								finish();
							}
						}).create();
	}

	class ColorListener implements ColorPicker.OnColorChangedListener {
		@Override
		public void onColorChanged(String key, String color) {
			int start = mEditor.getSelectionStart();
			int end = mEditor.getSelectionEnd();
			if (start < 0 || end < 0 || !mEditor.isFocused()) {
				return;
			}
			mEditor.getEditableText().replace(Math.min(start, end),
					Math.max(start, end), color, 0, color.length());
			// mEditor.getText().insert(mEditor.getSelectionStart(), color);//
			// 插入
		}

	}
}
