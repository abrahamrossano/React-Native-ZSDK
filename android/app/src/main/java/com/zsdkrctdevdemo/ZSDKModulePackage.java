package com.zsdkrctdevdemo;

import com.facebook.react.*;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZSDKModulePackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext rc) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new ZSDKModule(rc));
        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext rc) {
        return Collections.emptyList();
    }
}