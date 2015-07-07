package com.googlecode.android_scripting.activity;

import com.googlecode.android_scripting.rpc.MethodDescriptor;

public class ApiItem {

	public static final int ITEM = 0;
	public static final int SECTION = 1;
	public final int type;
	public final String name;
	public final String help;
	private final MethodDescriptor method;
	public int sectionPosition;
	public int listPosition;

	public ApiItem(int type, String name, String help, MethodDescriptor method) {
		this.type = type;
		this.name = name;
		this.help = help;
		this.method = method;
	}

	public String getName() {
		return name;
	}

	public String getHelp() {
		return help;
	}

	@Override
	public String toString() {
		return name;
	}

	public MethodDescriptor getMethod() {
		return method;
	}

}
