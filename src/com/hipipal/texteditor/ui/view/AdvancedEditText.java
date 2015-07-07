package com.hipipal.texteditor.ui.view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Scroller;

import com.googlecode.android_scripting.R;
import com.hipipal.texteditor.common.Constants;
import com.hipipal.texteditor.common.Settings;

/**
 * TODO create a syntax highlighter
 */
public class AdvancedEditText extends EditText implements Constants,
		OnKeyListener, OnGestureListener {

	String fileType = null;

	Boolean isWatch = true;

	/**
	 * @param context
	 *            the current context
	 * @param attrs
	 *            some attributes
	 * @category ObjectLifecycle
	 */
	public AdvancedEditText(Context context, AttributeSet attrs) {
		super(context, attrs);

		mPaintNumbers = new Paint();
		mPaintNumbers.setTypeface(Typeface.MONOSPACE);
		mPaintNumbers.setAntiAlias(true);

		mPaintHighlight = new Paint();

		mScale = context.getResources().getDisplayMetrics().density;
		mPadding = (int) (mPaddingDP * mScale);

		mHighlightedLine = mHighlightStart = -1;

		mDrawingRect = new Rect();
		mLineBounds = new Rect();

		mGestureDetector = new GestureDetector(getContext(), this);

		updateFromSettings("");

	}

	public void setFileType(String ft) {
		fileType = ft;
	}

	/**
	 * @see android.widget.TextView#computeScroll()
	 * @category View
	 */
	@Override
	public void computeScroll() {

		if (mTedScroller != null) {
			if (mTedScroller.computeScrollOffset()) {
				scrollTo(mTedScroller.getCurrX(), mTedScroller.getCurrY());
			}
		} else {
			super.computeScroll();
		}
	}

	/**
	 * @see EditText#onDraw(Canvas)
	 * @category View
	 */
	@Override
	public void onDraw(Canvas canvas) {
		int count, padding, lineX, baseline;

		// padding
		padding = mPadding;
		count = getLineCount();
		if (Settings.SHOW_LINE_NUMBERS) {
			padding = (int) (Math.floor(Math.log10(count)) + 1);
			padding = (int) ((padding * mPaintNumbers.getTextSize()) + mPadding + (Settings.TEXT_SIZE
					* mScale * 0.5));
			setPadding(padding, mPadding, mPadding, mPadding);
		} else {
			setPadding(mPadding, mPadding, mPadding, mPadding);
		}

		// get the drawing boundaries
		getDrawingRect(mDrawingRect);

		// display current line
		computeLineHighlight();

		// draw line numbers
		count = getLineCount();
		lineX = (int) (mDrawingRect.left + padding - (Settings.TEXT_SIZE
				* mScale * 0.5));

		for (int i = 0; i < count; i++) {
			baseline = getLineBounds(i, mLineBounds);
			if ((mMaxSize != null) && (mMaxSize.x < mLineBounds.right)) {
				mMaxSize.x = mLineBounds.right;
			}

			if ((mLineBounds.bottom < mDrawingRect.top)
					|| (mLineBounds.top > mDrawingRect.bottom)) {
				continue;
			}

			if ((i == mHighlightedLine) && (!Settings.WORDWRAP)) {
				canvas.drawRect(mLineBounds, mPaintHighlight);
			}

			if (Settings.SHOW_LINE_NUMBERS) {
				canvas.drawText("" + (i + 1), mDrawingRect.left + mPadding,
						baseline, mPaintNumbers);
			}
			if (Settings.SHOW_LINE_NUMBERS) {
				canvas.drawLine(lineX, mDrawingRect.top, lineX,
						mDrawingRect.bottom, mPaintNumbers);
			}
		}

		if (mMaxSize != null) {
			mMaxSize.y = mLineBounds.bottom;
			mMaxSize.x = Math.max(mMaxSize.x + mPadding - mDrawingRect.width(),
					0);
			mMaxSize.y = Math.max(
					mMaxSize.y + mPadding - mDrawingRect.height(), 0);
		}
		super.onDraw(canvas);
	}

	/**
	 * @see android.view.View.OnKeyListener#onKey(android.view.View, int,
	 *      android.view.KeyEvent)
	 */
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		return false;
	}

	/**
	 * @see android.widget.TextView#onTouchEvent(android.view.MotionEvent)
	 * @category GestureDetection
	 */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		if (mGestureDetector != null) {
			return mGestureDetector.onTouchEvent(event);
		}

		return true;
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.MotionEvent)
	 * @category GestureDetection
	 */
	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onSingleTapUp(android.view.MotionEvent)
	 * @category GestureDetection
	 */
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (isEnabled()) {
			((InputMethodManager) getContext().getSystemService(
					Context.INPUT_METHOD_SERVICE)).showSoftInput(this,
					InputMethodManager.SHOW_IMPLICIT);
		}
		return true;
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onShowPress(android.view.MotionEvent)
	 * @category GestureDetection
	 */
	@Override
	public void onShowPress(MotionEvent e) {
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onLongPress(android.view.MotionEvent)
	 */
	@Override
	public void onLongPress(MotionEvent e) {

	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onScroll(android.view.MotionEvent,
	 *      android.view.MotionEvent, float, float)
	 */
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// mTedScroller.setFriction(0);
		return true;
	}

	/**
	 * @see android.view.GestureDetector.OnGestureListener#onFling(android.view.MotionEvent,
	 *      android.view.MotionEvent, float, float)
	 */
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (!Settings.FLING_TO_SCROLL) {
			return true;
		}

		if (mTedScroller != null) {
			mTedScroller.fling(getScrollX(), getScrollY(), -(int) velocityX,
					-(int) velocityY, 0, mMaxSize.x, 0, mMaxSize.y);
		}
		return true;
	}

	/**
	 * Update view settings from the app preferences
	 * 
	 * @category Custom
	 */
	public void updateFromSettings(String fileType) {
		Log.d(TAG, "updateFromSettings:" + fileType);

		if (isInEditMode()) {
			return;
		}

		setTypeface(Settings.getTypeface(getContext()));

		// wordwrap
		setHorizontallyScrolling(!Settings.WORDWRAP);

		// color Theme
		switch (Settings.COLOR) {
		case COLOR_NEGATIVE:
			setBackgroundResource(R.drawable.textfield_black);
			setTextColor(Color.WHITE);
			mPaintHighlight.setColor(Color.WHITE);
			mPaintNumbers.setColor(Color.GRAY);
			break;
		case COLOR_MATRIX:
			setBackgroundResource(R.drawable.textfield_matrix);
			setTextColor(Color.GREEN);
			mPaintHighlight.setColor(Color.GREEN);
			mPaintNumbers.setColor(Color.rgb(0, 128, 0));
			break;
		case COLOR_SKY:
			setBackgroundResource(R.drawable.textfield_sky);
			setTextColor(Color.rgb(0, 0, 64));
			mPaintHighlight.setColor(Color.rgb(0, 0, 64));
			mPaintNumbers.setColor(Color.rgb(0, 128, 255));
			break;
		case COLOR_DRACULA:
			setBackgroundResource(R.drawable.textfield_dracula);
			setTextColor(Color.RED);
			mPaintHighlight.setColor(Color.RED);
			mPaintNumbers.setColor(Color.rgb(192, 0, 0));
			break;
		case COLOR_CLASSIC:
		default:
			setBackgroundResource(R.drawable.textfield_white);
			setTextColor(Color.BLACK);
			mPaintHighlight.setColor(Color.BLACK);
			mPaintNumbers.setColor(Color.GRAY);
			break;
		}
		mPaintHighlight.setAlpha(48);

		// text size
		setTextSize(Settings.TEXT_SIZE);
		mPaintNumbers.setTextSize(Settings.TEXT_SIZE * mScale * 0.85f);

		// refresh view
		postInvalidate();
		refreshDrawableState();

		// use Fling when scrolling settings ?
		if (Settings.FLING_TO_SCROLL) {
			mTedScroller = new Scroller(getContext());
			mMaxSize = new Point();
		} else {
			mTedScroller = null;
			mMaxSize = null;
		}

		/*
		 * if (this.fileType!=null) { if (this.fileType.equals("py")) { init();
		 * } } else if (fileType!=null && !fileType.equals("")) {
		 */
		// this.fileType = fileType;
		// Log.d(TAG, "fileType:"+fileType);
		Log.d(TAG, "init");
		// TODO Auto-generated method stub
		if (fileType.equals("py")) {
			// Log.d(TAG, "init OK");
			isWatch = true;
			init();
			refresh();
		} else {
			isWatch = false;
			cancelUpdate();
			unHightlight();
		}
		// }
	}

	/**
	 * Compute the line to highlight based on selection
	 */
	protected void computeLineHighlight() {
		int i, line, selStart;
		String text;

		if (!isEnabled()) {
			mHighlightedLine = -1;
			return;
		}

		selStart = getSelectionStart();
		if (mHighlightStart != selStart) {
			text = getText().toString();

			line = i = 0;
			while (i < selStart) {
				i = text.indexOf("\n", i);
				if (i < 0) {
					break;
				}
				if (i < selStart) {
					++line;
				}
				++i;
			}

			mHighlightedLine = line;
		}
	}

	/** The line numbers paint */
	protected Paint mPaintNumbers;
	/** The line numbers paint */
	protected Paint mPaintHighlight;
	/** the offset value in dp */
	protected int mPaddingDP = 6;
	/** the padding scaled */
	protected int mPadding;
	/** the scale for desnity pixels */
	protected float mScale;

	/** the scroller instance */
	protected Scroller mTedScroller;
	/** the velocity tracker */
	protected GestureDetector mGestureDetector;
	/** the Max size of the view */
	protected Point mMaxSize;

	/** the highlighted line index */
	protected int mHighlightedLine;
	protected int mHighlightStart;

	protected Rect mDrawingRect, mLineBounds;

	// ///////

	public interface OnTextChangedListener {
		public void onTextChanged(String text);
	}

	public OnTextChangedListener onTextChangedListener = null;
	public int updateDelay = 1000;
	public int errorLine = 0;
	public boolean dirty = false;

	private static final int COLOR_ERROR = 0x80ff0000;
	// private static final int COLOR_NUMBER = 0xff7ba212;
	private static final int COLOR_KEYWORD = 0xff7ba212;
	private static final int COLOR_BUILTIN = 0xffd79e39;
	private static final int COLOR_COMMENT = 0xff808080;
	private static final int COLOR_QUOTE = 0xff399ed7;

	private static final Pattern line = Pattern.compile(".*\\n");
	private static final Pattern numbers = Pattern
			.compile("\\b(\\d*[.]?\\d+)\\b");
	private static final Pattern keywords = Pattern
			.compile("\\b(break|continue|del|" + "except|exec|finally|"
					+ "pass|print|raise|" + "return|try|with|"
					+ "global|assert|" + "lambda|yield|" + "def|class|self|"
					+ "for|while|" + "if|elif|else|" + "and|in|is|not|or|"
					+ "import|from|as)\\b");
	private static final Pattern builtins = Pattern
			.compile("\\b(True|False|bool|enumerate|set|frozenset|help|"
					+ "reversed|sorted|sum|"
					+ "Ellipsis|None|NotImplemented|__import__|abs|"
					+ "apply|buffer|callable|chr|classmethod|cmp|"
					+ "coerce|compile|complex|delattr|dict|dir|divmod|"
					+ "eval|execfile|file|filter|float|getattr|globals|"
					+ "hasattr|hash|hex|id|input|int|intern|isinstance|"
					+ "issubclass|iter|len|list|locals|long|map|max|"
					+ "min|object|oct|open|ord|pow|property|range|"
					+ "raw_input|reduce|reload|repr|round|setattr|"
					+ "slice|staticmethod|str|super|tuple|type|unichr|"
					+ "unicode|vars|xrange|zip|"
					+ "ArithmeticError|AssertionError|AttributeError|"
					+ "DeprecationWarning|EOFError|EnvironmentError|"
					+ "Exception|FloatingPointError|IOError|"
					+ "ImportError|IndentationError|IndexError|"
					+ "KeyError|KeyboardInterrupt|LookupError|"
					+ "MemoryError|NameError|NotImplementedError|"
					+ "OSError|OverflowError|OverflowWarning|"
					+ "ReferenceError|RuntimeError|RuntimeWarning|"
					+ "StandardError|StopIteration|SyntaxError|"
					+ "SyntaxWarning|SystemError|SystemExit|TabError|"
					+ "TypeError|UnboundLocalError|UnicodeError|"
					+ "UnicodeEncodeError|UnicodeDecodeError|"
					+ "UnicodeTranslateError|"
					+ "UserWarning|ValueError|Warning|WindowsError|"
					+ "ZeroDivisionError)\\b");
	private static final Pattern comments = Pattern
			.compile("/\\*(?:.|[\\n\\r])*?\\*/|" + "#.*\n|"
					+ "\"\"\"(?:.|[\\n\\r])*?\"\"\"|"
					+ "\'\'\'(?:.|[\\n\\r])*?\'\'\'");
	private static final Pattern trailingWhiteSpace = Pattern.compile(
			"[\\t ]+$", Pattern.MULTILINE);
	private static final Pattern quotes = Pattern.compile("\"([^[\"\\n]])+\"|"
			+ "\'([^[\'\\n]])+\'");

	private final Handler updateHandler = new Handler();
	private final Runnable updateRunnable = new Runnable() {
		@Override
		public void run() {
			Editable e = getText();

			if (onTextChangedListener != null)
				onTextChangedListener.onTextChanged(e.toString());

			highlightWithoutChange(e);
		}
	};
	private boolean modified = true;

	public void setTextHighlighted(CharSequence text) {
		cancelUpdate();

		errorLine = 0;
		dirty = false;

		modified = false;
		setText(highlight(new SpannableStringBuilder(text)));
		modified = true;

		if (onTextChangedListener != null)
			onTextChangedListener.onTextChanged(text.toString());
	}

	public String getCleanText() {
		return trailingWhiteSpace.matcher(getText()).replaceAll("");
	}

	public void refresh() {
		highlightWithoutChange(getText());
	}

	public void unHightlight() {
		unHightWithoutChange(getText());
	}

	private void init() {
		//setHorizontallyScrolling(true);

		setFilters(new InputFilter[] { new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				if (modified && end - start == 1 && start < source.length()
						&& dstart < dest.length()) {
					char c = source.charAt(start);

					if (c == '\n')
						return autoIndent(source, start, end, dest, dstart,
								dend);
				}

				return source;
			}
		} });

		addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable e) {
				if (isWatch) {
					cancelUpdate();

					if (!modified)
						return;

					dirty = true;
					updateHandler.postDelayed(updateRunnable, updateDelay);
				}
			}
		});
	}

	private void cancelUpdate() {
		updateHandler.removeCallbacks(updateRunnable);
	}

	private void highlightWithoutChange(Editable e) {
		modified = false;
		highlight(e);
		modified = true;
	}

	public void unHightWithoutChange(Editable e) {
		Log.d(TAG, "unHightWithoutChange");
		modified = false;
		clearSpans(e);
		modified = true;

	}

	private Editable highlight(Editable e) {
		try {
			// don't use e.clearSpans() because it will remove
			// too much
			clearSpans(e);

			if (e.length() == 0)
				return e;

			if (errorLine > 0) {
				Matcher m = line.matcher(e);

				for (int n = errorLine; n-- > 0 && m.find();)
					;

				e.setSpan(new BackgroundColorSpan(COLOR_ERROR), m.start(),
						m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}

			/*
			 * for( Matcher m = numbers.matcher( e ); m.find(); ) e.setSpan( new
			 * ForegroundColorSpan( COLOR_NUMBER ), m.start(), m.end(),
			 * Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			 */

			for (Matcher m = keywords.matcher(e); m.find();)
				e.setSpan(new ForegroundColorSpan(COLOR_KEYWORD), m.start(),
						m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			for (Matcher m = builtins.matcher(e); m.find();)
				e.setSpan(new ForegroundColorSpan(COLOR_BUILTIN), m.start(),
						m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			for (Matcher m = comments.matcher(e); m.find();)
				e.setSpan(new ForegroundColorSpan(COLOR_COMMENT), m.start(),
						m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			for (Matcher m = quotes.matcher(e); m.find();)
				e.setSpan(new ForegroundColorSpan(COLOR_QUOTE), m.start(),
						m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} catch (Exception ex) {
		}

		return e;
	}

	private void clearSpans(Editable e) {
		// remove foreground color spans
		{
			ForegroundColorSpan spans[] = e.getSpans(0, e.length(),
					ForegroundColorSpan.class);

			for (int n = spans.length; n-- > 0;)
				e.removeSpan(spans[n]);
		}

		// remove background color spans
		{
			BackgroundColorSpan spans[] = e.getSpans(0, e.length(),
					BackgroundColorSpan.class);

			for (int n = spans.length; n-- > 0;)
				e.removeSpan(spans[n]);
		}
	}

	private CharSequence autoIndent(CharSequence source, int start, int end,
			Spanned dest, int dstart, int dend) {
		String indent = "";
		int istart = dstart - 1;
		int iend = -1;

		// find start of this line
		boolean dataBefore = false;
		int pt = 0;

		for (; istart > -1; --istart) {
			char c = dest.charAt(istart);

			if (c == '\n')
				break;

			if (c != ' ' && c != '\t') {
				if (!dataBefore) {
					// indent always after those characters
					if (c == '{' || c == '+' || c == '-' || c == '*'
							|| c == '/' || c == '%' || c == '^' || c == '=')
						--pt;

					dataBefore = true;
				}

				// parenthesis counter
				if (c == '(')
					--pt;
				else if (c == ')')
					++pt;
			}
		}

		// copy indent of this line into the next
		if (istart > -1) {
			char charAtCursor = dest.charAt(dstart);

			for (iend = ++istart; iend < dend; ++iend) {
				char c = dest.charAt(iend);

				// auto expand comments
				if (charAtCursor != '\n' && c == '/' && iend + 1 < dend
						&& dest.charAt(iend) == c) {
					iend += 2;
					break;
				}

				if (c != ' ' && c != '\t')
					break;
			}

			indent += dest.subSequence(istart, iend);
		}

		// add new indent
		if (pt < 0)
			indent += "\t";

		// append white space of previous line and new indent
		return source + indent;
	}

	public static Pattern getNumbers() {
		return numbers;
	}
}
