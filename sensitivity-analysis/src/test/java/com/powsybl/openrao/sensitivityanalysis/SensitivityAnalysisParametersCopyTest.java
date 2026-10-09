/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityOperatorStrategiesCalculationMode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link SensitivityAnalysisParameters} instance is shared between sensitivity analyses (e.g. MARMOT timestamps).
 * Each analysis sets its own operator strategies calculation mode, so it must work on a private copy and never write to
 * the shared instance.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisParametersCopyTest {

    @Test
    void testCopyIsDeepAndIndependent() {
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.getLoadFlowParameters().setDc(true).setDistributedSlack(false);

        SensitivityAnalysisParameters copy = SystematicSensitivityAdapter.copy(parameters);

        assertNotSame(parameters, copy);
        assertNotSame(parameters.getLoadFlowParameters(), copy.getLoadFlowParameters());
        assertTrue(copy.getLoadFlowParameters().isDc());
        assertFalse(copy.getLoadFlowParameters().isDistributedSlack());

        copy.setOperatorStrategiesCalculationMode(SensitivityOperatorStrategiesCalculationMode.ONLY_OPERATOR_STRATEGIES);
        copy.getLoadFlowParameters().setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_LOAD);
        assertEquals(SensitivityOperatorStrategiesCalculationMode.NONE, parameters.getOperatorStrategiesCalculationMode());
        assertEquals(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX, parameters.getLoadFlowParameters().getBalanceType());
    }

    @Test
    void testAnalysisWithAppliedRaDoesNotMutateTheSharedParameters() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(ONE, TWO));
        RangeActionSensitivityProvider factorProvider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", crac.getInstant("curative")), crac.getPstRangeAction("pst"), -3.1);
        SensitivityAnalysisParameters sharedParameters = new SensitivityAnalysisParameters();

        // the with-RA analysis runs with ONLY_OPERATOR_STRATEGIES, but only on its private copy
        SystematicSensitivityAdapter.runSensitivity(network, factorProvider, appliedRemedialActions, sharedParameters, "MockSensi", crac.getOutageInstant());

        assertEquals(SensitivityOperatorStrategiesCalculationMode.NONE, sharedParameters.getOperatorStrategiesCalculationMode());
    }
}
