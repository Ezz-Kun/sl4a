package com.googlecode.android_scripting.activity;

import static com.hipipal.texteditor.common.FileUtils.copyFile;
import static com.hipipal.texteditor.common.TextFileUtils.readTextFile;

import java.io.File;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ScrollView;
import android.widget.TextView;

import com.googlecode.android_scripting.R;

@SuppressLint("InflateParams")
public class AboutActivity extends ActionBarActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		getSupportActionBar().setTitle("About");
		getSupportActionBar().setHomeButtonEnabled(true);// 决定左上角的图标是否可以点击
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);// 给左上角图标的左边加上一个返回的图标

		File file = new File(getFilesDir() + File.separator + "LICENSE");
		String text = null;
		try {
			copyFile(getAssets().open("LICENSE"), file);
			text = readTextFile(file);
		} catch (IOException e) {
			text = "By: ざ凍結の→愛\nQQ群: 134840268\nEmail: 892768447@qq.com";
			e.printStackTrace();
		}
		final TextView textView = (TextView) findViewById(R.id.about_text);
		textView.setText(text);
		textView.post(new Runnable() {
			@Override
			public void run() {
				((ScrollView) findViewById(R.id.about_scroll))
						.scrollTo(0, textView.getHeight());
			}
		});
	}
}
