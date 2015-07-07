package com.googlecode.android_scripting.activity;

import java.io.File;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.googlecode.android_scripting.FeaturedInterpreters;
import com.googlecode.android_scripting.R;

@SuppressLint("InflateParams")
public abstract class ScriptManagerAdapter extends BaseAdapter {

	protected final Context mContext;
	protected final LayoutInflater mInflater;

	public ScriptManagerAdapter(Context context) {
		mContext = context;
		mInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return getScriptList().size();
	}

	@Override
	public Object getItem(int position) {
		return getScriptList().get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout container;
		ViewHolder holder;
		File script = getScriptList().get(position);

		if (convertView == null) {
			container = (LinearLayout) mInflater.inflate(R.layout.list_item,
					null);
		} else {
			container = (LinearLayout) convertView;
		}

		ImageView icon = (ImageView) container
				.findViewById(R.id.list_item_icon);
		int resourceId;
		if (script.isDirectory()) {
			resourceId = R.drawable.folder;
		} else {
			resourceId = FeaturedInterpreters.getInterpreterIcon(mContext,
					script.getName());
			if (resourceId == 0) {
				resourceId = R.drawable.sl4a_logo_32;
			}
		}
		icon.setImageResource(resourceId);

		holder = new ViewHolder(container);
		container.setTag(holder);
		holder.text.setText(getScriptList().get(position).getName());
		return container;
	}

	protected abstract List<File> getScriptList();

	static class ViewHolder {
		final TextView text;

		ViewHolder(View view) {
			text = (TextView) view.findViewById(R.id.list_item_title);
		}
	}
}
