package com.hipipal.texteditor.undo;

import android.text.Editable;
import android.util.Log;

import com.googlecode.android_scripting.BuildConfig;

public class TextChangeDelete implements TextChange {

	protected StringBuffer mSequence;
	protected int mStart;

	/**
	 * @param seq
	 *            the sequence being deleted
	 * @param start
	 *            the start index
	 */
	public TextChangeDelete(CharSequence seq, int start) {
		mSequence = new StringBuffer();
		mSequence.append(seq);
		mStart = start;
	}

	/**
	 * @see com.hipipal.texteditor.undo.TextChange#undo(android.text.Editable)
	 */
	@Override
	public int undo(Editable s) {
		s.insert(mStart, mSequence);
		return mStart + mSequence.length();
	}

	/**
	 * @see com.hipipal.texteditor.undo.TextChange#getCaret()
	 */
	@Override
	public int getCaret() {
		if (mSequence.toString().contains(" "))
			return -1;
		if (mSequence.toString().contains("\n"))
			return -1;
		return mStart;
	}

	/**
	 * @see com.hipipal.texteditor.undo.TextChange#append(java.lang.CharSequence)
	 */
	@Override
	public void append(CharSequence seq) {
		mSequence.insert(0, seq);
		if (BuildConfig.DEBUG)
			Log.d(TAG, mSequence.toString());
		mStart -= seq.length();
	}

	/**
	 * @see com.hipipal.texteditor.undo.TextChange#canMergeChangeAfter(java.lang.CharSequence,
	 *      int, int, int)
	 */
	@Override
	public boolean canMergeChangeBefore(CharSequence s, int start, int count,
			int after) {
		CharSequence sub;
		if (mSequence.toString().contains(" "))
			return false;
		if (mSequence.toString().contains("\n"))
			return false;
		if ((count != 1) || (start + count != mStart))
			return false;

		sub = s.subSequence(start, start + count);
		append(sub);
		return true;
	}

	/**
	 * @see com.hipipal.texteditor.undo.TextChange#canMergeChangeBefore(java.lang.CharSequence,
	 *      int, int, int)
	 */
	@Override
	public boolean canMergeChangeAfter(CharSequence s, int start, int before,
			int count) {
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "-\"" + mSequence.toString().replaceAll("\n", "~") + "\" @"
				+ mStart;
	}

}
