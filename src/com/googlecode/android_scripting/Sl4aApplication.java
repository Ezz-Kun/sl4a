/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.android_scripting;

import zce.app.crash.CrashHandler;

public class Sl4aApplication extends BaseApplication {

	@Override
	public void onCreate() {
		super.onCreate();
		// 捕捉异常
		CrashHandler handler = CrashHandler.getInstance();
		handler.init(this,"啊哦！程序出了点小问题!",false);
		Analytics.start(this, "UA-158835-13");
	}

	@Override
	public void onTerminate() {
		Analytics.stop();
	}

}
