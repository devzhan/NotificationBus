/* Copyright (C) 2017 Tcl Corporation Limited */
/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.notificationsample;

import android.app.Application;

import com.sample.notificationsample.log.Logger;
import com.sample.notificationsample.log.NLog;


public class MyApplication extends Application {

    private static final String TAG ="Browser";

    @Override
    public void onCreate() {
        super.onCreate();
        initNLog();

    }


    private void initNLog() {
        NLog.setDebug(true, Logger.VERBOSE);
    }


}