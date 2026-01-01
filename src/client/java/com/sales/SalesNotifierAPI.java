package com.sales;

import com.sales.config.SalesNotifierConfig;
import com.sales.modules.SalesNotifierModule;

public class SalesNotifierAPI {
    private static SalesNotifierModule notifierModule;
    private static SalesNotifierConfig config;

    public static void setNotifierModule(SalesNotifierModule module) {
        notifierModule = module;
    }

    public static void setConfig(SalesNotifierConfig configInstance) {
        config = configInstance;
    }

    public static SalesNotifierModule getNotifierModule() {
        return notifierModule;
    }

    public static SalesNotifierConfig getConfig() {
        return config;
    }

    public static void saveConfig() {
        if (config != null) {
            config.save();
        }
    }
}
