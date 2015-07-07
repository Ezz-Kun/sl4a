package com.googlecode.android_scripting.activity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import python.progressbar.CircularProgressDrawable;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.R;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

public class InstallExampleDialog {

	private ImageView ivDrawable;
	private CircularProgressDrawable drawable;
	private Animator currentAnimation;;
	private Dialog dialog;
	private Context context;
	private InstallAsyncTask installAsyncTask;
	private String destPath;
	private boolean isEnd = false;

	public InstallExampleDialog(final Context context,
			final ScriptManager sManager, Builder builder) {
		this.context = context;
		dialog = builder.create();
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
		dialog.setContentView(R.layout.example_install_dialog);
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg) {
				sManager.updateAndFilterScriptList("");
				sManager.updatemAdapter();
			}
		});
		installAsyncTask = new InstallAsyncTask();
		ivDrawable = (ImageView) this.dialog.findViewById(R.id.iv_drawable);
		drawable = new CircularProgressDrawable(context.getResources()
				.getDimensionPixelSize(R.dimen.drawable_ring_size), context
				.getResources().getColor(android.R.color.darker_gray), context
				.getResources().getColor(R.color.holo_green_light), context
				.getResources().getColor(R.color.holo_blue_dark));
		ivDrawable.setImageDrawable(drawable);
		currentAnimation = prepareStyle1Animation();
		currentAnimation.start();
		ivDrawable.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (currentAnimation != null) {
					currentAnimation.cancel();
				}
				currentAnimation = prepareStyle1Animation();
				currentAnimation.start();
				if (isEnd) {
					if (currentAnimation != null) {
						currentAnimation.cancel();
					}
					dialog.cancel();// 如果解压完成了。点击该控件则消失
				}
			}
		});
	}

	/**
	 * 得到解压路径
	 * 
	 * @return
	 */
	public String getDestPath() {
		if (!destPath.endsWith(File.separator)) {
			setDestPath(destPath + File.separator);
		}
		return destPath;
	}

	/**
	 * 设置解压路径
	 * 
	 * @param destPath
	 */
	public void setDestPath(String destPath) {
		this.destPath = destPath;
	}

	/**
	 * 开始任务
	 */
	public void start() {
		if (destPath == null) {
			if (currentAnimation != null) {
				currentAnimation.cancel();
			}
			this.dialog.cancel();
			return;
		}
		installAsyncTask = new InstallAsyncTask();
		installAsyncTask.execute();
	}

	private Animator prepareStyle1Animation() {
		AnimatorSet animation = new AnimatorSet();

		final Animator indeterminateAnimation = ObjectAnimator.ofFloat(
				drawable, CircularProgressDrawable.PROGRESS_PROPERTY, 0, 3600);
		indeterminateAnimation.setDuration(3600);

		Animator innerCircleAnimation = ObjectAnimator.ofFloat(drawable,
				CircularProgressDrawable.CIRCLE_FILL_PROPERTY, 0f, 1f);
		innerCircleAnimation.setDuration(3600);
		innerCircleAnimation.addListener(new EmptyAnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				drawable.setIndeterminate(true);
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				indeterminateAnimation.end();
				drawable.setIndeterminate(false);
				drawable.setProgress(0);
			}
		});

		animation.playTogether(innerCircleAnimation, indeterminateAnimation);
		return animation;
	}

	public class InstallAsyncTask extends AsyncTask<Void, String, Boolean> {

		private boolean running = true;
		private static final int BUFFER_SIZE = 4096;
		private BufferedOutputStream bufferedOutputStream = null;
		private BufferedInputStream bufferedInputStream = null;

		/**
		 * 解压数据
		 */
		@Override
		protected Boolean doInBackground(Void... arg) {
			try {
				// 先把文件复制到files目录
				File nfile = context.getFileStreamPath("new_api_example.zip");
				if (nfile.exists()) {
					nfile.delete();// 先删除
				}
				FileOutputStream fOutputStream = new FileOutputStream(
						context.getFileStreamPath("new_api_example.zip"));
				InputStream inputStream = context.getAssets().open(
						"new_api_example.zip");
				byte[] buff = new byte[1024];
				int len;
				while ((len = inputStream.read(buff)) > 0) {
					fOutputStream.write(buff, 0, len);
				}
				fOutputStream.flush();
				inputStream.close();
				fOutputStream.close();
				ZipFile zf = new ZipFile(context.getFileStreamPath(
						"new_api_example.zip").getAbsolutePath(), "GBK");
				@SuppressWarnings("rawtypes")
				Enumeration enumeration = zf.getEntries();
				byte buffer[] = new byte[BUFFER_SIZE];
				while (running && enumeration.hasMoreElements()) {
					ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
					String zipEntryName = zipEntry.getName();
					File file = new File(getDestPath() + zipEntryName);
					// 解压数据
					if (zipEntry.isDirectory()) {
						if (!file.exists()) {
							file.mkdirs();// 创建文件夹
						}
					} else {
						if (!file.getParentFile().exists()) {
							file.getParentFile().mkdirs();
						}
						if (file.lastModified() < zipEntry.getTime()) {
							Log.i("解压文件: " + file.getName());
							bufferedOutputStream = new BufferedOutputStream(
									new FileOutputStream(file), BUFFER_SIZE);
							bufferedInputStream = new BufferedInputStream(
									zf.getInputStream(zipEntry));
							int count;
							while ((count = bufferedInputStream.read(buffer, 0,
									BUFFER_SIZE)) != -1) {
								bufferedOutputStream.write(buffer, 0, count);
							}
							bufferedOutputStream.flush();
							bufferedInputStream.close();
							bufferedOutputStream.close();
						}
					}
				}
				zf.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}

		/**
		 * 解压完成
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			isEnd = true;
			Toast.makeText(context, context.getString(R.string.s_Unpackok),
					Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}

		@Override
		protected void onCancelled() {
			running = false;
			super.onCancelled();
		}

	}

	class EmptyAnimatorListener implements Animator.AnimatorListener {

		@Override
		public void onAnimationStart(Animator animation) {

		}

		@Override
		public void onAnimationEnd(Animator animation) {

		}

		@Override
		public void onAnimationCancel(Animator animation) {

		}

		@Override
		public void onAnimationRepeat(Animator animation) {

		}
	}

}
