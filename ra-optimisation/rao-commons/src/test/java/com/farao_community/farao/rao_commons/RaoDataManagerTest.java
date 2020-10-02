/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoDataManagerTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private RaoData raoData;
    private LoopFlowResult loopFlowResult;

    @Before
    public void setUp() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();

        CnecLoopFlowExtension cnecLoopFlowExtension1 = new CnecLoopFlowExtension(100., Unit.MEGAWATT);
        CnecLoopFlowExtension cnecLoopFlowExtension2 = new CnecLoopFlowExtension(100., Unit.MEGAWATT);

        crac.getCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension1);
        crac.getCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension2);

        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()));

        loopFlowResult = new LoopFlowResult();
        loopFlowResult.addCnecResult(crac.getCnec("cnec1basecase"), -252, 128., -124.);
        loopFlowResult.addCnecResult(crac.getCnec("cnec2basecase"), 24., 45., 69.);
    }

    @Test
    public void testFillCnecLoopExtensionsWithInitialResults() {
        raoData.getRaoDataManager().fillCnecLoopExtensionsWithInitialResults(loopFlowResult, raoData.getNetwork());
        assertEquals(252., raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraintInMW(), DOUBLE_TOLERANCE);
        assertEquals(100., raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraintInMW(), DOUBLE_TOLERANCE);
        assertEquals(128., raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecLoopFlowExtension.class).getLoopflowShift(), DOUBLE_TOLERANCE);
        assertEquals(45., raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecLoopFlowExtension.class).getLoopflowShift(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFillCracResultsWithLoopFlows() {
        raoData.getRaoDataManager().fillCnecLoopExtensionsWithInitialResults(loopFlowResult, raoData.getNetwork());
        raoData.getRaoDataManager().fillCracResultsWithLoopFlows(loopFlowResult);
        String var = raoData.getWorkingVariantId();

        assertEquals(-252., raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(24, raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(252., raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
        assertEquals(100., raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFillCracResultsWithLoopFlowApproximation() {

        SystematicSensitivityResult sensiResults = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResults.getReferenceFlow(raoData.getCrac().getCnec("cnec1basecase"))).thenReturn(-162.);
        Mockito.when(sensiResults.getReferenceFlow(raoData.getCrac().getCnec("cnec2basecase"))).thenReturn(47.);

        raoData.getRaoDataManager().fillCnecLoopExtensionsWithInitialResults(loopFlowResult, raoData.getNetwork());
        raoData.setSystematicSensitivityResult(sensiResults);
        raoData.getRaoDataManager().fillCracResultsWithLoopFlowApproximation();
        String var = raoData.getWorkingVariantId();

        assertEquals(-162 - 128., raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(47. - 45., raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(252., raoData.getCrac().getCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
        assertEquals(100., raoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
    }
}
