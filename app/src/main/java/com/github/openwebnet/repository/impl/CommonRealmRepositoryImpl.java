package com.github.openwebnet.repository.impl;

import com.annimon.stream.Stream;
import com.github.openwebnet.database.DatabaseRealm;
import com.github.openwebnet.model.RealmModel;
import com.github.openwebnet.repository.CommonRealmRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import io.realm.RealmObject;
import rx.Observable;

import static com.google.common.base.Preconditions.checkState;

public abstract class CommonRealmRepositoryImpl<M extends RealmObject & RealmModel>
        implements CommonRealmRepository<M> {

    private static final Logger log = LoggerFactory.getLogger(CommonRealmRepository.class);

    @Inject
    DatabaseRealm databaseRealm;

    protected abstract Class<M> getRealmModelClass();

    @Override
    public Observable<String> add(M model) {
        return Observable.create(subscriber -> {
            try {
                subscriber.onNext(databaseRealm.add(model).getUuid());
                subscriber.onCompleted();
            } catch (Exception e) {
                log.error("common-ADD", e);
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<List<String>> addAll(List<M> models) {
        return Observable.create(subscriber -> {
            try {
                List<String> results = Stream
                    .of(databaseRealm.addAll(models))
                    .map(RealmModel::getUuid)
                    .toList();

                subscriber.onNext(results);
                subscriber.onCompleted();
            } catch (Exception e) {
                log.error("common-ADD_ALL", e);
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<Void> update(M model) {
        return Observable.create(subscriber -> {
            try {
                databaseRealm.update(model);
                subscriber.onNext(null);
                subscriber.onCompleted();
            } catch (Exception e) {
                log.error("common-UPDATE", e);
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<Void> delete(String uuid) {
        return Observable.create(subscriber -> {
            try {
                databaseRealm.delete(getRealmModelClass(), RealmModel.FIELD_UUID, uuid);
                subscriber.onNext(null);
                subscriber.onCompleted();
            } catch (Exception e) {
                log.error("common-DELETE", e);
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<Void> deleteAll() {
        return Observable.create(subscriber -> {
            try {
                databaseRealm.deleteAll(getRealmModelClass());
                subscriber.onNext(null);
                subscriber.onCompleted();
            } catch (Exception e) {
                log.error("common-DELETE_ALL", e);
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<M> findById(String uuid) {
        return Observable.create(subscriber -> {
            try {
                List<M> models = databaseRealm.findWhere(getRealmModelClass(), RealmModel.FIELD_UUID, uuid);
                checkState(models.size() == 1, "primary key violation: invalid uuid");
                subscriber.onNext(models.get(0));
                subscriber.onCompleted();
            } catch (Exception e) {
                log.error("common-FIND_BY_ID", e);
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<List<M>> findAll() {
        return Observable.create(subscriber -> {
            try {
                subscriber.onNext(databaseRealm.find(getRealmModelClass()));
                subscriber.onCompleted();
            } catch (Exception e) {
                log.error("common-FIND_ALL", e);
                subscriber.onError(e);
            }
        });
    }

}
