package albgorski.playground.hystrix;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AgoObservableCommandTest {
    private static final Logger LOG = LoggerFactory.getLogger(AgoObservableCommandTest.class);
    public static final Action1<Optional<String>> ACTION = s -> LOG.debug(">>> " + s);

    @Test
    public void executeSeparate() throws InterruptedException {
        long start = System.currentTimeMillis();
        int count = 3;
        final CountDownLatch latch = new CountDownLatch(count);
        int workingTimeInMs = 200;

        for (int i = 0; i < count; i++) {
            Observable<Optional<String>> single1 = new AgoObservableCommand(latch, workingTimeInMs).toObservable().subscribeOn(Schedulers.computation());
            single1.single().subscribe(ACTION);
        }

        System.out.println("\n\n");
        LOG.debug("observables initialized");
        latch.await(count * workingTimeInMs + 500, TimeUnit.MILLISECONDS);
        LOG.debug("work took " + (System.currentTimeMillis() - start) + "ms, all task summarized took " + (count * workingTimeInMs) + "ms");

        // wait for log4j messages to come on the console before program finishes
        Thread.sleep(50);
    }
}