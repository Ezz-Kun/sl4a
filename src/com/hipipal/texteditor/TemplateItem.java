package com.hipipal.texteditor;

public class TemplateItem {

	private String name;
	private String context;

	public TemplateItem(String name, String context) {
		setName(name);
		setContext(context);
	}

	/**
	 * 返回模版名字
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * 设置模版名字
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回模版内容
	 * 
	 * @return
	 */
	public String getContext() {
		return context;
	}

	/**
	 * 设置模版内容
	 * 
	 * @param context
	 */
	public void setContext(String context) {
		this.context = context;
	}

	/**
	 * 重写toString方法直接返回模版内容
	 */
	@Override
	public String toString() {
		return getContext();
	}

}
