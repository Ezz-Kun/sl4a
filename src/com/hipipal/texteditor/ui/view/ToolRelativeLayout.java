package com.hipipal.texteditor.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.googlecode.android_scripting.R;

public class ToolRelativeLayout extends RelativeLayout {

	private int left, top, right, bottom;
	private ImageButton toolBtn;

	public ToolRelativeLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public ToolRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public ToolRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
		toolBtn = (ImageButton) findViewById(R.id.toolBtn);
		if (toolBtn != null) {
			// System.out.println("找到控件");
			if (left == 0 && top == 0 && right == 0 && bottom == 0) {
				// System.out.println("0");
				left = toolBtn.getLeft();
				top = toolBtn.getTop();
				right = toolBtn.getRight();
				bottom = toolBtn.getBottom();
			}
			toolBtn.layout(left, top, right, bottom);
			toolBtn.postInvalidate();
		}
	}

	public void setBtnLayout(int left, int top, int right, int bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}

}
