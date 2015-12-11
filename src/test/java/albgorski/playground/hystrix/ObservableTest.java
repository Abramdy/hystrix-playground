package albgorski.playground.hystrix;

import org.junit.Test;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("Duplicates")
public class ObservableTest {

    @Test
    public void just() throws InterruptedException {
        int count = 5;
        final CountDownLatch latch = new CountDownLatch(count);
        Observable<String> just1 = createOne(count, 1);
        just1.subscribeOn(Schedulers.newThread()).subscribe(counter -> {
            latch.countDown();
            System.out.println("just1 " + Thread.currentThread().getName() + " : " + counter);
        });
        System.out.println("after observables");
        latch.await(1, TimeUnit.MINUTES);
    }


    private Observable<String> createOne(int amount, final int pos) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try {
                    if (!subscriber.isUnsubscribed()) {
                        for (int i = 1; i <= amount; i++) {
                            Thread.sleep(50);
                            subscriber.onNext(pos + " has value " + i);
                        }
                        subscriber.onCompleted();
                    }
                } catch (Exception ex) {
                    subscriber.onError(ex);
                }
            }
        });
    }
}
