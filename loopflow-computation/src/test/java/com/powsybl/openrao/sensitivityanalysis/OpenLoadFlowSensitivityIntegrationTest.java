/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.sensi.OpenSensitivityAnalysisProvider;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityAnalysisRunParameters;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFactorModelReader;
import com.powsybl.sensitivity.SensitivityResultModelWriter;
import com.powsybl.sensitivity.SensitivityState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class OpenLoadFlowSensitivityIntegrationTest {
    @Test
    void convergedBaseCaseDoesNotEmitPreContingencyStatus() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(TwoSides.ONE));
        List<SensitivityFactor> factors = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT)).getBasecaseFactors(network);
        SensitivityResultModelWriter writer = new SensitivityResultModelWriter(List.of(), List.of());

        new OpenSensitivityAnalysisProvider().run(network, network.getVariantManager().getWorkingVariantId(),
            new SensitivityFactorModelReader(factors, network), writer, new SensitivityAnalysisRunParameters().setParameters(new SensitivityAnalysisParameters())).join();

        assertTrue(writer.isComputationComplete());
        assertTrue(writer.getStateStatuses().stream().noneMatch(status -> status.getState().equals(SensitivityState.PRE_CONTINGENCY)));
        assertFalse(writer.getValues().isEmpty());
        SensitivityAnalysisResult input = new SensitivityAnalysisResult(factors, writer.getStateStatuses(), List.of(), List.of(), writer.getValues(), writer.isComputationComplete());
        SystematicSensitivityResult result = new SystematicSensitivityResult().completeData(input, crac.getOutageInstant().getOrder());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus());
        assertEquals(SystematicSensitivityResult.SensitivityComputationStatus.SUCCESS, result.getStatus(crac.getPreventiveState()));
        String elementId = crac.getFlowCnec("cnec1basecase").getNetworkElementId();
        double providerReference = writer.getValues().stream()
            .filter(value -> factors.get(value.getFactorIndex()).getFunctionId().equals(elementId))
            .findFirst().orElseThrow().getFunctionReference();
        assertTrue(Double.isFinite(providerReference));
        assertEquals(providerReference, result.getReferenceFlow(crac.getFlowCnec("cnec1basecase"), TwoSides.ONE), 1e-6);
    }

    @Test
    void failedBaseCaseThrowsWithoutEmittingPreContingencyStatus() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(TwoSides.ONE));
        List<SensitivityFactor> factors = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT)).getBasecaseFactors(network);
        SensitivityResultModelWriter writer = new SensitivityResultModelWriter(List.of(), List.of());
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        OpenLoadFlowParameters.create(parameters.getLoadFlowParameters()).setMaxNewtonRaphsonIterations(1);

        CompletionException exception = assertThrows(CompletionException.class, () -> new OpenSensitivityAnalysisProvider().run(
            network, network.getVariantManager().getWorkingVariantId(), new SensitivityFactorModelReader(factors, network), writer,
            new SensitivityAnalysisRunParameters().setParameters(parameters)).join());
        assertTrue(exception.getCause().getMessage().contains("Initial load flow of base situation ended with solver status MAX_ITERATION_REACHED"));
        assertFalse(writer.isComputationComplete());
        assertTrue(writer.getStateStatuses().stream().noneMatch(status -> status.getState().equals(SensitivityState.PRE_CONTINGENCY)));
        assertTrue(writer.getValues().isEmpty());
    }
}
