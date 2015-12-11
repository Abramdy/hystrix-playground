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

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("Duplicates")
public class ExecuteCommandsInParallelTest {
    private static final Logger LOG = LoggerFactory.getLogger(ExecuteCommandsInParallelTest.class);
    public static final Action1<Optional<String>> ACTION = s -> LOG.debug(">>> " + s);

    @Test
    public void executeSeparate() throws InterruptedException {
        long start = System.currentTimeMillis();
        int count = 3;
        final CountDownLatch latch = new CountDownLatch(count);
        int workingTimeInMs = 200;
        int workingTimeSummarized = count * workingTimeInMs;

        for (int i = 0; i < count; i++) {
            Observable<Optional<String>> single1 = new AgoObservableCommand(latch, workingTimeInMs, 500).toObservable().subscribeOn(Schedulers.computation());
            single1.single().subscribe(ACTION);
        }
        LOG.debug("observables initialized");

        latch.await(workingTimeSummarized + 500, TimeUnit.MILLISECONDS);
        long executionTime = System.currentTimeMillis() - start;

        assertThat(executionTime).isLessThanOrEqualTo(workingTimeSummarized);
        LOG.debug("work took " + executionTime + "ms, all task summarized took " + workingTimeSummarized + "ms");

        // wait for log4j messages to come on the console before program finishes
        Thread.sleep(50);
    }

}