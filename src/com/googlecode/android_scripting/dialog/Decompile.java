package com.googlecode.android_scripting.dialog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.googlecode.android_scripting.Constants;
import com.googlecode.android_scripting.FileUtils;
import com.googlecode.android_scripting.R;
import com.googlecode.android_scripting.activity.ScriptingLayerService;
import com.hipipal.texteditor.common.TextFileUtils;

import de.keyboardsurfer.mobile.app.android.widget.crouton.Crouton;
import de.keyboardsurfer.mobile.app.android.widget.crouton.Style;

public class Decompile {

	public static void show(final Activity activity, final String path) {
		// 解压文件
		final String rootPath = activity.getFilesDir().getAbsolutePath();
		try {
			byte[] buffer = new byte[1024];
			ZipInputStream zip = new ZipInputStream(activity.getAssets().open(
					"decompile.zip"));
			ZipEntry zipEntry;
			while ((zipEntry = zip.getNextEntry()) != null) {
				File file = new File(rootPath, zipEntry.getName());
				if (zipEntry.isDirectory()) {
					file.mkdirs();// 如果是文件夹并且不存在则创建
					FileUtils.chmod(file, 0755);
				} else {
					if (!file.getParentFile().exists()) {
						file.getParentFile().mkdirs();
						FileUtils.chmod(file.getParentFile(), 0755);
					}
					if (file.lastModified() < zipEntry.getTime()) {
						OutputStream output = new BufferedOutputStream(
								new FileOutputStream(file), 1024);
						int len;
						while ((len = zip.read(buffer, 0, 1024)) != -1) {
							output.write(buffer, 0, len);
						}
						output.flush();
						output.close();
						file.setLastModified(zipEntry.getTime());
						FileUtils.chmod(file, 0755);
					}
				}
			}
			zip.close();
		} catch (IOException e) {
			e.printStackTrace();
			Crouton.cancelAllCroutons();
			Crouton.showText(activity,
					activity.getString(R.string.s_CannotuseDeconpile),
					Style.ALERT);
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Crouton.cancelAllCroutons();
			Crouton.showText(activity,
					activity.getString(R.string.s_CannotuseDeconpile),
					Style.ALERT);
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		List<CharSequence> list = new ArrayList<CharSequence>();
		list.add(activity.getString(R.string.s_DecompileOne));
		list.add(activity.getString(R.string.s_DecompileTwo));
		list.add(activity.getString(R.string.s_DecompileThree));
		list.add(activity.getString(R.string.s_DecompileFour));
		CharSequence[] mylist = list.toArray(new CharSequence[list.size()]);
		builder.setItems(mylist, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String method = "";
				switch (which) {
				case 0:
					method = "1";
					break;
				case 1:
					method = "2";
					break;
				case 2:
					method = "3";
					break;
				case 3:
					method = "4";
					break;
				}
				if (!TextFileUtils.writeTextFile(rootPath + File.separator
						+ "decompile/command.dat",
						String.format("('%s','%s')", method, path))) {
					Crouton.showText(activity,
							activity.getString(R.string.s_CannotuseDeconpile),
							Style.ALERT);
					return;
				}
				Intent intent = new Intent(activity,
						ScriptingLayerService.class);
				intent.setAction(Constants.ACTION_LAUNCH_FOREGROUND_SCRIPT);
				intent.putExtra(Constants.EXTRA_SCRIPT_PATH, rootPath
						+ File.separator + "decompile/decompile.py");
				activity.startService(intent);
			}
		});
		builder.show();
	}

}
