package albgorski.playground.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixObservableCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static java.util.Optional.of;

public class AgoObservableCommand extends HystrixObservableCommand<Optional<String>> {
    private static final Logger LOG = LoggerFactory.getLogger(AgoObservableCommand.class);
    private static final int EXECUTION_TIMEOUT = 1000;
    private static final int CIRCUIT_BREAKER_SLEEP_WINDOW = 1000;
    private static final Setter SETTER =
            Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey("albgorski.playground.hystrix"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(AgoObservableCommand.class.getSimpleName()))
                    .andCommandPropertiesDefaults(
                            HystrixCommandProperties.Setter()
                                    .withFallbackEnabled(true)
                                    .withRequestCacheEnabled(false)
                                    .withRequestLogEnabled(false)
                                    .withCircuitBreakerEnabled(true)
                                    .withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE)
                                    .withExecutionIsolationSemaphoreMaxConcurrentRequests(10)
                                    .withExecutionTimeoutInMilliseconds(EXECUTION_TIMEOUT)
                                    .withExecutionTimeoutEnabled(true)
                                    .withExecutionIsolationThreadInterruptOnTimeout(true)
                                    .withMetricsRollingPercentileEnabled(true)
                                    .withMetricsRollingStatisticalWindowInMilliseconds(10_000)
                                    .withCircuitBreakerErrorThresholdPercentage(50)
                                    .withCircuitBreakerRequestVolumeThreshold(10)
                                    .withCircuitBreakerSleepWindowInMilliseconds(CIRCUIT_BREAKER_SLEEP_WINDOW)
                                    .withCircuitBreakerEnabled(true)
                    );
    private final CountDownLatch latch;
    private final long workingTimeInMs;

    protected AgoObservableCommand(CountDownLatch latch, long workingTimeInMs) {
        super(SETTER);
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
                        Thread.sleep(workingTime);
                        Thread thread = Thread.currentThread();
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

}
