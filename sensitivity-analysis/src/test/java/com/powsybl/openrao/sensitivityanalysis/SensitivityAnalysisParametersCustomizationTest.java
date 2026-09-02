/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisParametersCustomizationTest {

    @AfterEach
    void tearDown() {
        SensitivityAnalysisParametersCustomization.remove();
    }

    @Test
    void testNoCustomizationLeavesParametersUntouched() {
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        double flowFlowSensitivityValueThreshold = parameters.getFlowFlowSensitivityValueThreshold();
        SensitivityAnalysisParametersCustomization.apply(parameters);
        assertEquals(flowFlowSensitivityValueThreshold, parameters.getFlowFlowSensitivityValueThreshold());
    }

    @Test
    void testCustomizationIsApplied() {
        SensitivityAnalysisParametersCustomization.set(p -> p.setFlowFlowSensitivityValueThreshold(0.5));
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        SensitivityAnalysisParametersCustomization.apply(parameters);
        assertEquals(0.5, parameters.getFlowFlowSensitivityValueThreshold());
    }

    @Test
    void testCustomizationIsRemoved() {
        SensitivityAnalysisParametersCustomization.set(p -> p.setFlowFlowSensitivityValueThreshold(0.5));
        SensitivityAnalysisParametersCustomization.remove();
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        SensitivityAnalysisParametersCustomization.apply(parameters);
        assertEquals(0d, parameters.getFlowFlowSensitivityValueThreshold());
    }

    @Test
    void testCustomizationIsNotNull() {
        assertThrows(NullPointerException.class, () -> SensitivityAnalysisParametersCustomization.set(null));
    }

    /**
     * The search tree installs one customization per leaf chunk, on threads of a shared pool: a customization must
     * never leak from a thread to another, otherwise concurrent leaves would end up sharing a cache scope.
     */
    @Test
    void testCustomizationIsThreadScoped() throws Exception {
        SensitivityAnalysisParametersCustomization.set(p -> p.setFlowFlowSensitivityValueThreshold(0.5));

        AtomicReference<Double> thresholdSeenByOtherThread = new AtomicReference<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.submit(() -> {
                SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
                SensitivityAnalysisParametersCustomization.apply(parameters);
                thresholdSeenByOtherThread.set(parameters.getFlowFlowSensitivityValueThreshold());
            }).get();
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        }

        // the other thread has no customization of its own and must not see this one
        assertEquals(0d, thresholdSeenByOtherThread.get());

        // while this thread still has its own
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        SensitivityAnalysisParametersCustomization.apply(parameters);
        assertEquals(0.5, parameters.getFlowFlowSensitivityValueThreshold());
    }

    /**
     * The RAO parameters instance is shared by all the threads of the search tree: each of them attaches its own cache
     * scope to it. This goes through the whole {@link SystematicSensitivityAdapter} path to check that what a thread
     * attaches ends up only in the parameters of its own analyses, and never in the shared instance.
     */
    @Test
    void testConcurrentCustomizationsDoNotLeakIntoTheSharedParameters() throws Exception {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        Instant outageInstant = crac.getInstant("outage");

        // the one and only parameters instance, shared by all threads, exactly as the RAO parameters are
        SensitivityAnalysisParameters sharedParameters = new SensitivityAnalysisParameters();

        Collection<SensitivityAnalysisParameters> captured = new ConcurrentLinkedQueue<>();
        MockSensiProvider.captureParametersInto(captured);

        int threadCount = 4;
        int runsPerThread = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                // each thread attaches its own marker, standing for its own cache scope
                double marker = i + 1d;
                futures.add(executorService.submit(() -> {
                    SensitivityAnalysisParametersCustomization.set(p -> p.setFlowFlowSensitivityValueThreshold(marker));
                    try {
                        for (int run = 0; run < runsPerThread; run++) {
                            RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(
                                crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
                            SystematicSensitivityAdapter.runSensitivity(network, factorProvider, sharedParameters, "MockSensi", outageInstant);
                        }
                    } finally {
                        SensitivityAnalysisParametersCustomization.remove();
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
            MockSensiProvider.stopCapturingParameters();
        }

        // the shared instance must never have been written to
        assertEquals(0d, sharedParameters.getFlowFlowSensitivityValueThreshold());

        // every analysis must have run on its own copy, carrying its own thread's marker and no other
        assertEquals(threadCount * runsPerThread, captured.size());
        for (SensitivityAnalysisParameters parameters : captured) {
            assertNotSame(sharedParameters, parameters);
            double marker = parameters.getFlowFlowSensitivityValueThreshold();
            assertTrue(marker >= 1d && marker <= threadCount, "unexpected marker " + marker + ", a customization leaked between threads");
        }
        // and all threads must be represented, otherwise the test would pass without ever running them concurrently
        assertEquals(threadCount, captured.stream().map(SensitivityAnalysisParameters::getFlowFlowSensitivityValueThreshold).distinct().count());
    }

    @Test
    void testRemoveIsIdempotent() {
        SensitivityAnalysisParametersCustomization.remove();
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        SensitivityAnalysisParametersCustomization.apply(parameters);
        assertNull(parameters.getLoadFlowParameters().getExtension(Object.class));
    }
}
