package com.github.openwebnet.view.device;

import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.github.openwebnet.R;
import com.github.openwebnet.model.EnvironmentModel;
import com.github.openwebnet.model.GatewayModel;
import com.github.openwebnet.service.EnvironmentService;
import com.github.openwebnet.service.GatewayService;
import com.github.openwebnet.service.UtilityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;

import static com.github.openwebnet.view.NavigationViewItemSelectedListener.MENU_ENVIRONMENT_RANGE_MAX;
import static com.github.openwebnet.view.NavigationViewItemSelectedListener.MENU_ENVIRONMENT_RANGE_MIN;

public abstract class AbstractDeviceActivity extends AppCompatActivity {

    private static final Logger log = LoggerFactory.getLogger(AbstractDeviceActivity.class);

    public static final String EXTRA_DEFAULT_ENVIRONMENT = "com.github.openwebnet.view.device.AbstractDeviceActivity.EXTRA_DEFAULT_ENVIRONMENT";
    public static final String EXTRA_DEFAULT_GATEWAY = "com.github.openwebnet.view.device.AbstractDeviceActivity.EXTRA_DEFAULT_GATEWAY";

    @BindView(R.id.spinnerDeviceEnvironment)
    Spinner spinnerDeviceEnvironment;

    @BindView(R.id.spinnerDeviceGateway)
    Spinner spinnerDeviceGateway;

    @BindView(R.id.checkBoxDeviceFavourite)
    CheckBox checkBoxDeviceFavourite;

    @BindString(R.string.validation_required)
    String validationRequired;

    @BindString(R.string.label_none)
    String labelNone;

    @BindString(R.string.label_missing_gateway)
    String labelMissingGateway;

    @Inject
    UtilityService utilityService;

    @Inject
    EnvironmentService environmentService;

    @Inject
    GatewayService gatewayService;

    private SparseArray<EnvironmentModel> environmentArray;
    private SparseArray<GatewayModel> gatewayArray;

    /**
     *
     */
    protected abstract void onMenuSave();

    protected void initSpinnerEnvironment() {
        environmentService.findAll().subscribe(environments -> {
            environmentArray = initSparseArray(environments);

            List<String> environmentValues = Stream.of(environments)
                .map(environment -> environment.getName()).collect(Collectors.toList());

            initEmptyList(environmentValues, labelNone);
            initSpinnerAdapter(spinnerDeviceEnvironment, environmentValues);

            int defaultEnvironment = getIntent().getIntExtra(EXTRA_DEFAULT_ENVIRONMENT, -1);
            log.debug("defaultEnvironment: {}", defaultEnvironment);
            if (defaultEnvironment >= MENU_ENVIRONMENT_RANGE_MIN && defaultEnvironment <= MENU_ENVIRONMENT_RANGE_MAX) {
                selectEnvironment(defaultEnvironment);
            }
        });
    }

    protected void initSpinnerGateway() {
        gatewayService.findAll().subscribe(gateways -> {
            gatewayArray = initSparseArray(gateways);

            List<String> gatewayValues = Stream.of(gateways)
                .map(gateway -> {
                    String gatewayStr = String.format("%s:%d", gateway.getHost(), gateway.getPort());
                    if (gateway.getPasswordNullable() != null) {
                        return gatewayStr.concat(" (*)");
                    }
                    return gatewayStr;
                }).collect(Collectors.toList());

            initEmptyList(gatewayValues, labelMissingGateway);
            initSpinnerAdapter(spinnerDeviceGateway, gatewayValues);

            String defaultGateway = getIntent().getStringExtra(EXTRA_DEFAULT_GATEWAY);
            log.debug("defaultGateway: {}", defaultGateway);
            if (defaultGateway != null) {
                selectGateway(defaultGateway);
            }
        });
    }

    protected <T> SparseArray<T> initSparseArray(List<T> items) {
        SparseArray<T> array = new SparseArray<>();
        for (int index = 0; index < items.size(); index++) {
            array.put(index, items.get(index));
        }
        return array;
    }

    private void initEmptyList(List<String> values, String label) {
        if (values.isEmpty()) {
            values.add(label);
        }
    }

    protected void initSpinnerAdapter(Spinner spinner, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, values);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    protected EnvironmentModel getSelectedEnvironment() {
        return environmentArray.get(spinnerDeviceEnvironment.getSelectedItemPosition());
    }

    protected void selectEnvironment(Integer environmentId) {
        Function <Integer, Integer> findSelectedEnvironment = id -> {
            for (int i=0; i<environmentArray.size(); i++) {
                if (environmentArray.valueAt(i).getId() == id) {
                    return i;
                }
            }
            throw new IllegalStateException("unable to find a valid environment");
        };
        spinnerDeviceEnvironment.setSelection(findSelectedEnvironment.apply(environmentId));
    }

    protected GatewayModel getSelectedGateway() {
        return gatewayArray.get(spinnerDeviceGateway.getSelectedItemPosition());
    }

    protected void selectGateway(String gatewayUuid) {
        Function <String, Integer> findSelectedGateway = uuid -> {
            for (int i=0; i<gatewayArray.size(); i++) {
                if (gatewayArray.valueAt(i).getUuid().equals(uuid)) {
                    return i;
                }
            }
            throw new IllegalStateException("unable to find a valid gateway");
        };
        spinnerDeviceGateway.setSelection(findSelectedGateway.apply(gatewayUuid));
    }

    protected boolean isValidDeviceEnvironment() {
        return isValidRequired((TextView) spinnerDeviceEnvironment.getSelectedView());
    }

    protected boolean isValidDeviceGateway() {
        return isValidRequired((TextView) spinnerDeviceGateway.getSelectedView());
    }

    protected boolean isValidRequired(TextView view) {
        if (utilityService.isBlankText(view)
                || view.getText().equals(labelNone)
                || view.getText().equals(labelMissingGateway)) {
            view.setError(validationRequired);
            view.requestFocus();
            return false;
        }
        return true;
    }

    protected boolean isFavourite() {
        return checkBoxDeviceFavourite.isChecked();
    }

    protected void setFavourite(boolean checked) {
        checkBoxDeviceFavourite.setChecked(checked);
    }

    protected <S> Function<S, Integer> findSelectedItem(SparseArray<S> sparseArray) {
        return type -> {
            for (int i = 0; i < sparseArray.size(); i++) {
                if (sparseArray.valueAt(i).equals(type)) {
                    return i;
                }
            }
            throw new IllegalStateException("unable to find an item");
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_device_save:
                onMenuSave();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_save, menu);
        return true;
    }

}
