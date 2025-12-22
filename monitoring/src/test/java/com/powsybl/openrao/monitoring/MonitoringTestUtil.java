package com.powsybl.openrao.monitoring;

import com.powsybl.computation.local.LocalComputationManager;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public final class MonitoringTestUtil {
    private MonitoringTestUtil() {
        // Util class
    }

    public static LocalComputationManager getComputationManager(final AtomicInteger referenceValue, final CountDownLatch latch) throws IOException {
        return new LocalComputationManager() {
            /**
             * The getExecutor method is called by OpenLoadFlow to run the loadflow with the executor.
             * Here the referenceValue will be incremented before each loadflow execution and
             * the latch count will be decremented after each loadflow execution.
             */
            @Override
            public Executor getExecutor() {
                final Executor delegate = super.getExecutor();
                return command ->
                    delegate.execute(() -> {
                        referenceValue.incrementAndGet();
                        command.run(); // Loadflow execution goes here
                        latch.countDown();
                    });
            }
        };
    }
}
