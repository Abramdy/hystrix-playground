package albgorski.playground.hystrix;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func3;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("Duplicates")
public class ZipCommandsTest {
    private static final Logger LOG = LoggerFactory.getLogger(ZipCommandsTest.class);
    private static final Func3<Optional<String>, Optional<String>, Optional<String>, List<Optional<String>>> ZIP_AS_LIST = (first, second, third) -> asList(first, second, third);

    @Test
    public void executeZip() throws InterruptedException {
        long start = System.currentTimeMillis();
        int count = 3;
        final CountDownLatch latch = new CountDownLatch(count);
        int hystrixTimeout = 300;

        int firstWorksLongerAsTimeout = hystrixTimeout + 10;
        int secondWorksShorterAsTimeout = hystrixTimeout - 10;
        int thirdWorksShorterAsTimeout = hystrixTimeout - 1;
        int workingTimeSummarized = firstWorksLongerAsTimeout + secondWorksShorterAsTimeout + thirdWorksShorterAsTimeout;

        Observable<Optional<String>> single1 = new AgoObservableCommand(latch, firstWorksLongerAsTimeout, hystrixTimeout).toObservable().subscribeOn(Schedulers.computation()).single();
        Observable<Optional<String>> single2 = new AgoObservableCommand(latch, secondWorksShorterAsTimeout, hystrixTimeout).toObservable().subscribeOn(Schedulers.computation()).single();
        Observable<Optional<String>> single3 = new AgoObservableCommand(latch, thirdWorksShorterAsTimeout, hystrixTimeout).toObservable().subscribeOn(Schedulers.computation()).single();

        LOG.debug("observables initialized");
        List<Optional<String>> single = Observable.zip(single1, single2, single3, ZIP_AS_LIST).subscribeOn(Schedulers.computation()).toBlocking().single();

        long executionTime = System.currentTimeMillis() - start;
        // 2 results with value since one is overtime with fallback Optional.empty()
        assertThat(single.stream().filter(Optional::isPresent).collect(Collectors.toList())).hasSize(2);
        // execution time must be smaller than working time summarized since tasks are executed parallel (on computation scheduler)
        assertThat(executionTime).isLessThanOrEqualTo(workingTimeSummarized);

        LOG.debug("result: " + single);
        LOG.debug("work took " + executionTime + "ms, all task summarized took " + workingTimeSummarized + "ms");

        latch.await(count * hystrixTimeout + 500, TimeUnit.MILLISECONDS);
        // wait for log4j messages to come on the console before program finishes
        Thread.sleep(200);
    }
}