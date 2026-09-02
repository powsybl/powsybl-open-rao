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
import com.powsybl.sensitivity.SensitivityOperatorStrategiesCalculationMode;
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

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * The {@link SensitivityAnalysisParameters} instance is shared between sensitivity analyses that may run concurrently
 * (e.g. MARMOT timestamps, parallel leaves). Each analysis sets its own operator strategies calculation mode, which the
 * provider may read asynchronously during the run: every analysis must therefore run on its own private copy, and the
 * shared instance must never be written to.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisParametersCopyTest {

    @Test
    void testConcurrentAnalysesEachRunOnTheirOwnCopy() throws Exception {
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
                futures.add(executorService.submit(() -> {
                    for (int run = 0; run < runsPerThread; run++) {
                        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(
                            crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
                        SystematicSensitivityAdapter.runSensitivity(network, factorProvider, sharedParameters, "MockSensi", outageInstant);
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

        // every analysis must have run on its own copy, never on the shared instance
        assertEquals(threadCount * runsPerThread, captured.size());
        for (SensitivityAnalysisParameters parameters : captured) {
            assertNotSame(sharedParameters, parameters);
        }
    }

    @Test
    void testAnalysisWithAppliedRaDoesNotMutateTheSharedParameters() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        Instant curativeInstant = crac.getInstant("curative");
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(
            crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", curativeInstant), crac.getPstRangeAction("pst"), -3.1);

        SensitivityAnalysisParameters sharedParameters = new SensitivityAnalysisParameters();

        Collection<SensitivityAnalysisParameters> captured = new ConcurrentLinkedQueue<>();
        MockSensiProvider.captureParametersInto(captured);
        try {
            SystematicSensitivityAdapter.runSensitivity(network, factorProvider, appliedRemedialActions,
                sharedParameters, "MockSensi", crac.getOutageInstant());
        } finally {
            MockSensiProvider.stopCapturingParameters();
        }

        // the with-RA analysis sets ONLY_OPERATOR_STRATEGIES, but only on its private copy
        assertEquals(SensitivityOperatorStrategiesCalculationMode.NONE, sharedParameters.getOperatorStrategiesCalculationMode());
        assertEquals(2, captured.size());
        assertEquals(1, captured.stream()
            .filter(parameters -> parameters.getOperatorStrategiesCalculationMode() == SensitivityOperatorStrategiesCalculationMode.ONLY_OPERATOR_STRATEGIES)
            .count());
        for (SensitivityAnalysisParameters parameters : captured) {
            assertNotSame(sharedParameters, parameters);
        }
    }
}
