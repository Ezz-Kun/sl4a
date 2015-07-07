package com.hipipal.texteditor;

import android.annotation.SuppressLint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.hipipal.texteditor.ui.view.ToolRelativeLayout;

@SuppressLint("ClickableViewAccessibility")
public class ToolOnTouch implements OnTouchListener {

	private float lastX, startX;
	private float lastY, startY;
	private float x;
	private float y;
	private int left, top, right, bottom, screenWidth, screenHeight;
	private ToolRelativeLayout layout;
	private TedActivity context;

	public ToolOnTouch(TedActivity context, ToolRelativeLayout layout) {
		this.context = context;
		this.layout = layout;
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		screenWidth = dm.widthPixels;
		screenHeight = dm.heightPixels - 50;
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		// 触摸点相对于屏幕左上角坐标
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:// 按下
			lastX = startX = event.getRawX();
			lastY = startY = event.getRawY();
			break;
		case MotionEvent.ACTION_MOVE:// 移动
			x = event.getRawX() - lastX;
			y = event.getRawY() - lastY;
			updatePosition(view, (int) x, (int) y);// 更新位置
			lastX = event.getRawX();
			lastY = event.getRawY();
			break;
		case MotionEvent.ACTION_UP:
			if (startX == event.getRawX() && startY == event.getRawY()) {
				context.onTool();
				// System.out.println("点击");
			}
			break;
		}
		return false;
	}

	/**
	 * 更新浮动窗口位置参数
	 */
	private void updatePosition(View view, int x, int y) {
		left = view.getLeft() + x;
		top = view.getTop() + y;
		right = view.getRight() + x;
		bottom = view.getBottom() + y;
		if (left < 0) {
			left = 0;
			right = left + view.getWidth();
		}
		if (right > screenWidth) {
			right = screenWidth;
			left = right - view.getWidth();
		}
		if (top < 0) {
			top = 0;
			bottom = top + view.getHeight();
		}
		if (bottom > screenHeight) {
			bottom = screenHeight;
			top = bottom - view.getHeight();
		}
		// View的当前位置
		layout.setBtnLayout(left, top, right, bottom);
		view.layout(left, top, right, bottom);
		view.postInvalidate();
	}

}
