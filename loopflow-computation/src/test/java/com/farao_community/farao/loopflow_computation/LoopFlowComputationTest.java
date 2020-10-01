/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.farao_community.farao.util.SensitivityComputationService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowComputationTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    @Test
    @Ignore
    public void calculateLoopFlowTest() {
        Network network = ExampleGenerator.network();
        Crac crac = ExampleGenerator.crac();
        GlskProvider glskProvider = ExampleGenerator.glskProvider();
        ReferenceProgram referenceProgram = ExampleGenerator.referenceProgram();
        SensitivityComputationParameters sensitivityComputationParameters = new SensitivityComputationParameters();

        // todo : wait for sensi Factory refactoring the update the way the sensi is been mocked
        ComputationManager computationManager = LocalComputationManager.getDefault();
        SensitivityComputationFactory sensitivityComputationFactory = ExampleGenerator.sensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);

        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, glskProvider, referenceProgram);
        LoopFlowResult loopFlowResult = loopFlowComputation.calculateLoopFlows(network, sensitivityComputationParameters);

        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(200., loopFlowResult.getLoopFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(-50., loopFlowResult.getLoopFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(50., loopFlowResult.getLoopFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);

        assertEquals(40., loopFlowResult.getCommercialFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(40., loopFlowResult.getCommercialFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(40, loopFlowResult.getCommercialFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(60., loopFlowResult.getCommercialFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(60., loopFlowResult.getCommercialFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);

        assertEquals(-10., loopFlowResult.getReferenceFlow(crac.getCnec("FR-BE1")), DOUBLE_TOLERANCE);
        assertEquals(240., loopFlowResult.getReferenceFlow(crac.getCnec("BE1-BE2")), DOUBLE_TOLERANCE);
        assertEquals(-10., loopFlowResult.getReferenceFlow(crac.getCnec("BE2-NL")), DOUBLE_TOLERANCE);
        assertEquals(110., loopFlowResult.getReferenceFlow(crac.getCnec("FR-DE")), DOUBLE_TOLERANCE);
        assertEquals(110., loopFlowResult.getReferenceFlow(crac.getCnec("DE-NL")), DOUBLE_TOLERANCE);
    }
}
