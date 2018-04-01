package com.github.openwebnet.repository;

import com.github.openwebnet.BuildConfig;
import com.github.openwebnet.OpenWebNetApplicationTest;
import com.github.openwebnet.component.ApplicationComponentTest;
import com.github.openwebnet.component.DaggerApplicationComponentTest;
import com.github.openwebnet.component.Injector;
import com.github.openwebnet.component.module.ApplicationContextModuleTest;
import com.github.openwebnet.component.module.DatabaseModuleTest;
import com.github.openwebnet.component.module.DomoticModuleTest;
import com.github.openwebnet.component.module.RepositoryModuleTest;
import com.github.openwebnet.database.DatabaseRealm;
import com.github.openwebnet.model.AutomationModel;
import com.github.openwebnet.model.DeviceModel;
import com.github.openwebnet.model.DomoticModel;
import com.github.openwebnet.model.EnvironmentModel;
import com.github.openwebnet.model.IpcamModel;
import com.github.openwebnet.model.LightModel;
import com.github.openwebnet.model.TemperatureModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import rx.observers.TestSubscriber;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(application = OpenWebNetApplicationTest.class, constants = BuildConfig.class, sdk = 21)
@PowerMockIgnore({"android.*"})
@PrepareForTest({Injector.class})
public class EnvironmentRepositoryTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Inject
    EnvironmentRepository environmentRepository;

    @Inject
    DatabaseRealm databaseRealm;

    @Before
    public void setupDagger() {
        ApplicationComponentTest applicationComponentTest = DaggerApplicationComponentTest.builder()
            .applicationContextModuleTest(new ApplicationContextModuleTest())
            .databaseModuleTest(new DatabaseModuleTest())
            .repositoryModuleTest(new RepositoryModuleTest(false))
            .domoticModuleTest(new DomoticModuleTest())
            .build();

        PowerMockito.mockStatic(Injector.class);
        PowerMockito.when(Injector.getApplicationComponent()).thenReturn(applicationComponentTest);

        ((ApplicationComponentTest) Injector.getApplicationComponent()).inject(this);
    }

    @Test
    public void environmentRepository_getNextIdDefault() {
        environmentRepository_testGetNextId(null, 100);
    }

    @Test
    public void environmentRepository_getNextId() {
        environmentRepository_testGetNextId(108, 109);
    }

    private void environmentRepository_testGetNextId(Integer currentMaxId, Integer nextId) {
        when(databaseRealm.findMax(EnvironmentModel.class, EnvironmentModel.FIELD_ID)).thenReturn(currentMaxId);

        TestSubscriber<Integer> tester = new TestSubscriber<>();
        environmentRepository.getNextId().subscribe(tester);

        verify(databaseRealm).findMax(EnvironmentModel.class, EnvironmentModel.FIELD_ID);

        tester.assertValue(nextId);
        tester.assertCompleted();
        tester.assertNoErrors();
    }

    @Test
    public void environmentRepository_add() {
        Integer ENVIRONMENT_ID = 100;
        String ENVIRONMENT_NAME = "myName";
        EnvironmentModel environment = newEnvironmentModel(ENVIRONMENT_ID, ENVIRONMENT_NAME);

        when(databaseRealm.add(environment)).thenReturn(environment);

        TestSubscriber<Integer> tester = new TestSubscriber<>();
        environmentRepository.add(environment).subscribe(tester);

        verify(databaseRealm).add(environment);

        tester.assertValue(ENVIRONMENT_ID);
        tester.assertCompleted();
        tester.assertNoErrors();
    }

    @Test
    public void environmentRepository_update() {
        EnvironmentModel environment = newEnvironmentModel(108, "env");

        when(databaseRealm.update(environment)).thenReturn(null);

        TestSubscriber<Void> tester = new TestSubscriber<>();
        environmentRepository.update(environment).subscribe(tester);

        verify(databaseRealm).update(environment);

        tester.assertCompleted();
        tester.assertNoErrors();
    }

    @Test
    public void environmentRepository_findById() {
        Integer ENVIRONMENT_ID = 100;
        EnvironmentModel environment = newEnvironmentModel(ENVIRONMENT_ID, "myName");
        List<EnvironmentModel> environments = Arrays.asList(environment);

        when(databaseRealm.findCopyWhere(EnvironmentModel.class, EnvironmentModel.FIELD_ID, ENVIRONMENT_ID, null))
            .thenReturn(environments);

        TestSubscriber<EnvironmentModel> tester = new TestSubscriber<>();
        environmentRepository.findById(ENVIRONMENT_ID).subscribe(tester);

        verify(databaseRealm).findCopyWhere(EnvironmentModel.class, EnvironmentModel.FIELD_ID, ENVIRONMENT_ID, null);

        tester.assertValue(environment);
        tester.assertCompleted();
        tester.assertNoErrors();
    }

    @Test
    public void environmentRepository_findAll() {
        List<EnvironmentModel> environments = Arrays
            .asList(newEnvironmentModel(100, "environment1"), newEnvironmentModel(101, "environment2"));

        when(databaseRealm.findSortedAscending(EnvironmentModel.class, EnvironmentModel.FIELD_NAME))
            .thenReturn(environments);

        TestSubscriber<List<EnvironmentModel>> tester = new TestSubscriber<>();
        environmentRepository.findAll().subscribe(tester);

        verify(databaseRealm).findSortedAscending(EnvironmentModel.class, EnvironmentModel.FIELD_NAME);

        tester.assertValue(environments);
        tester.assertCompleted();
        tester.assertNoErrors();
    }

    @Test
    public void environmentRepository_delete() {
        final int ENVIRONMENT_ID = 42;

        doNothing().when(databaseRealm).delete(EnvironmentModel.class, EnvironmentModel.FIELD_ID, ENVIRONMENT_ID);
        doNothing().when(databaseRealm).delete(LightModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        doNothing().when(databaseRealm).delete(AutomationModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        doNothing().when(databaseRealm).delete(DeviceModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        doNothing().when(databaseRealm).delete(IpcamModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        doNothing().when(databaseRealm).delete(TemperatureModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);

        TestSubscriber<Void> tester = new TestSubscriber<>();
        environmentRepository.delete(ENVIRONMENT_ID).subscribe(tester);

        verify(databaseRealm).delete(LightModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        verify(databaseRealm).delete(AutomationModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        verify(databaseRealm).delete(DeviceModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        verify(databaseRealm).delete(IpcamModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        verify(databaseRealm).delete(TemperatureModel.class, DomoticModel.FIELD_ENVIRONMENT_ID, ENVIRONMENT_ID);
        verify(databaseRealm).delete(EnvironmentModel.class, EnvironmentModel.FIELD_ID, ENVIRONMENT_ID);

        tester.assertCompleted();
        tester.assertNoErrors();
    }

    private EnvironmentModel newEnvironmentModel(Integer id, String name) {
        EnvironmentModel environment = new EnvironmentModel();
        environment.setId(id);
        environment.setName(name);
        return environment;
    }

}
