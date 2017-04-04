

package com.example.mbient.freefalldetector;


        import com.mbientlab.metawear.UnsupportedModuleException;
        import com.mbientlab.metawear.android.BtleService;
        import com.mbientlab.metawear.data.Acceleration;
        import com.mbientlab.metawear.data.AngularVelocity;
        import com.mbientlab.metawear.module.Led;
        import com.mbientlab.metawear.module.AccelerometerBmi160;
        import com.mbientlab.metawear.module.GyroBmi160;
        import com.mbientlab.metawear.module.GyroBmi160.*;

        import io.reactivex.Observable;
        import io.reactivex.disposables.Disposable;

public class MetaWearObservables {
    public Observable<Acceleration> accelObservable;
    public Observable<AngularVelocity> gyroObservable;

    MetaWearObservables(AccelerometerBmi160 accelM, GyroBmi160 gyroM){
        accelObservable.create(subscriber -> {
            accelM.acceleration().addRouteAsync(source -> source.stream((data, env) -> {
                Acceleration value = data.value(Acceleration.class);
                subscriber.onNext(value);
            }));
        });

        gyroObservable.create(subscriber -> {
            gyroM.angularVelocity().addRouteAsync(source -> source.stream((data, env) -> {
                AngularVelocity value = data.value(AngularVelocity.class);
                subscriber.onNext(value);
            }));
        });

    }



}
