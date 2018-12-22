package com.github.openwebnet.service.impl;

import android.content.Context;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.widget.TextView;

import com.github.openwebnet.component.Injector;
import com.github.openwebnet.service.UtilityService;
import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

public class UtilityServiceImpl implements UtilityService {

    private static final Logger log = LoggerFactory.getLogger(UtilityService.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Inject
    Context mContext;

    private SimpleDateFormat dateFormat;

    public UtilityServiceImpl() {
        Injector.getApplicationComponent().inject(this);
        this.dateFormat = new SimpleDateFormat(DATE_FORMAT);
    }

    @Override
    public String getString(int id) {
        return mContext.getResources().getString(id);
    }

    @Override
    public boolean hasNetworkAccess() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    @Override
    public boolean hasInternetAccess() {
        // http://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-timeouts
        Runtime runtime = Runtime.getRuntime();
        try {
            // ping google DNS
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException | InterruptedException e) {
            log.error("hasInternetAccess", e);
        }
        return false;
    }

    @Override
    public <T extends TextView> boolean isBlankText(T view) {
        return view == null || TextUtils.isEmpty(view.getText().toString().trim());
    }

    @Override
    public <T extends TextView> String sanitizedText(T view) {
        Preconditions.checkNotNull(view, "text is null");
        return view.getText().toString().trim().replaceAll("\\s+", " ");
    }

    @Override
    public String formatDate(Date date) {
        return dateFormat.format(date);
    }

}
