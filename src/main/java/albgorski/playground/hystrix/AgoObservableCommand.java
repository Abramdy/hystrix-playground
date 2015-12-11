package albgorski.playground.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class AgoObservableCommand extends HystrixObservableCommand<Optional<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(AgoObservableCommand.class);
    private static final int CIRCUIT_BREAKER_SLEEP_WINDOW = 1000;
    private final CountDownLatch latch;
    private final long workingTimeInMs;

    protected AgoObservableCommand(CountDownLatch latch, long workingTimeInMs, int hystrixTimeOut) {
        super(Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("albgorski.playground.hystrix"))
                .andCommandKey(HystrixCommandKey.Factory.asKey(AgoObservableCommand.class.getSimpleName()))
                .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter()
                                .withFallbackEnabled(true)
                                .withRequestCacheEnabled(false)
                                .withRequestLogEnabled(false)
                                .withCircuitBreakerEnabled(true)
                                .withExecutionIsolationSemaphoreMaxConcurrentRequests(10)
                                .withExecutionTimeoutInMilliseconds(hystrixTimeOut)
                                .withExecutionTimeoutEnabled(true)
                                .withExecutionIsolationThreadInterruptOnTimeout(true)
                                .withMetricsRollingPercentileEnabled(true)
                                .withMetricsRollingStatisticalWindowInMilliseconds(10_000)
                                .withCircuitBreakerErrorThresholdPercentage(50)
                                .withCircuitBreakerRequestVolumeThreshold(10)
                                .withCircuitBreakerSleepWindowInMilliseconds(CIRCUIT_BREAKER_SLEEP_WINDOW)
                                .withCircuitBreakerEnabled(true)
                ));
        this.latch = latch;
        this.workingTimeInMs = workingTimeInMs;
    }

    @Override
    protected Observable<Optional<String>> construct() {
        return createOne(workingTimeInMs);
    }

    private Observable<Optional<String>> createOne(long workingTime) {
        return Observable.create(new Observable.OnSubscribe<Optional<String>>() {
            @Override
            public void call(Subscriber<? super Optional<String>> subscriber) {
                try {
                    if (!subscriber.isUnsubscribed()) {
                        Thread thread = Thread.currentThread();
                        LOG.debug("start work thread " + thread.getId() + " " + thread.getName() + " for " + workingTime + "ms");
                        Thread.sleep(workingTime);
                        Optional<String> result = of("thread " + thread.getId() + " " + thread.getName() + " worked=" + workingTime + "ms");
                        latch.countDown();
                        subscriber.onNext(result);
                        subscriber.onCompleted();
                    }
                } catch (Exception ex) {
                    subscriber.onError(ex);
                }

            }
        });
    }

    @Override
    protected Observable<Optional<String>> resumeWithFallback() {
        return Observable.just(empty());
    }
}
