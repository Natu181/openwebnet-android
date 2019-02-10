package com.github.openwebnet.service.impl;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import com.github.niqdev.openwebnet.OpenWebNet;
import com.github.openwebnet.R;
import com.github.openwebnet.component.Injector;
import com.github.openwebnet.service.CommonService;
import com.github.openwebnet.service.EnvironmentService;
import com.github.openwebnet.service.GatewayService;
import com.github.openwebnet.service.PreferenceService;
import com.github.openwebnet.service.UtilityService;
import com.github.openwebnet.view.ChangeLogDialogFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import javax.inject.Inject;

import static com.github.niqdev.openwebnet.OpenWebNet.gateway;
import static com.github.niqdev.openwebnet.OpenWebNet.newClient;

public class CommonServiceImpl implements CommonService {

    private static final Logger log = LoggerFactory.getLogger(CommonService.class);

    private HashMap<String, OpenWebNet> CLIENT_CACHE;

    @Inject
    PreferenceService preferenceService;

    @Inject
    UtilityService utilityService;

    @Inject
    EnvironmentService environmentService;

    @Inject
    GatewayService gatewayService;

    @Inject
    Context mContext;

    public CommonServiceImpl() {
        Injector.getApplicationComponent().inject(this);
    }

    @Override
    public void initApplication(AppCompatActivity activity) {
        if (preferenceService.isFirstRun()) {
            environmentService.add(utilityService.getString(R.string.drawer_menu_example))
                .subscribe(
                    id -> log.debug("initApplication with success"),
                    throwable -> log.error("initApplication", throwable));
            preferenceService.initFirstRun();
        }
        if (preferenceService.isNewVersion()) {
            ChangeLogDialogFragment.show(activity);
            preferenceService.initVersion();
        }
        CLIENT_CACHE = new HashMap<>();
    }

    // TODO clean cache always when add/update/delete gateway
    @Override
    public OpenWebNet findClient(String gatewayUuid) {
        if (!CLIENT_CACHE.containsKey(gatewayUuid)) {
            // blocking - same thread
            gatewayService.findById(gatewayUuid).subscribe(gatewayModel -> {
                OpenWebNet.OpenGateway gateway = gateway(
                    gatewayModel.getHost(),
                    gatewayModel.getPort(),
                    gatewayModel.getPasswordNullable());
                OpenWebNet client = newClient(gateway);
                CLIENT_CACHE.put(gatewayUuid, client);
                log.info("new client cached: {}", gatewayUuid);
            });
        }
        return CLIENT_CACHE.get(gatewayUuid);
    }

    @Override
    public String getDefaultGateway() {
        return preferenceService.getDefaultGateway();
    }

}
