/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracResultManagerTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private RaoData raoData;
    private LoopFlowResult loopFlowResult;

    @Before
    public void setUp() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();

        CnecLoopFlowExtension cnecLoopFlowExtension1 = new CnecLoopFlowExtension(100., Unit.MEGAWATT);
        CnecLoopFlowExtension cnecLoopFlowExtension2 = new CnecLoopFlowExtension(100., Unit.MEGAWATT);

        crac.getBranchCnec("cnec1basecase").addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension1);
        crac.getBranchCnec("cnec2basecase").addExtension(CnecLoopFlowExtension.class, cnecLoopFlowExtension2);
        crac.getBranchCnec("cnec2basecase").setReliabilityMargin(20);

        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());

        loopFlowResult = new LoopFlowResult();
        loopFlowResult.addCnecResult(crac.getBranchCnec("cnec1basecase"), -252, 128., -124.);
        loopFlowResult.addCnecResult(crac.getBranchCnec("cnec2basecase"), 24., 45., 69.);
    }

    @Test
    public void testFillCracResultsWithLoopFlows() {
        raoData.getCracResultManager().fillCnecResultsWithLoopFlows(loopFlowResult);
        String var = raoData.getWorkingVariantId();

        assertEquals(-252., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(24, raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(100., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
        assertEquals(100. - 20., raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
        assertEquals(128., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getCommercialFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(45., raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getCommercialFlowInMW(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFillCracResultsWithLoopFlowApproximation() {

        SystematicSensitivityResult sensiResults = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResults.getReferenceFlow(raoData.getCrac().getBranchCnec("cnec1basecase"))).thenReturn(-162.);
        Mockito.when(sensiResults.getReferenceFlow(raoData.getCrac().getBranchCnec("cnec2basecase"))).thenReturn(47.);

        raoData.getCracResultManager().fillCnecResultsWithLoopFlows(loopFlowResult);
        raoData.setSystematicSensitivityResult(sensiResults);
        raoData.getCracResultManager().fillCnecResultsWithApproximatedLoopFlows();
        String var = raoData.getWorkingVariantId();

        assertEquals(-162 - 128., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(47. - 45., raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowInMW(), DOUBLE_TOLERANCE);
        assertEquals(100., raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
        assertEquals(100. - 20., raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getLoopflowThresholdInMW(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testCopyCommercialFlowsBetweenVariants() {
        raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setCommercialFlowInMW(150.6);
        raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(raoData.getPreOptimVariantId()).setCommercialFlowInMW(653.7);
        String var = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCracResultManager().copyCommercialFlowsBetweenVariants(raoData.getPreOptimVariantId(), var);
        assertEquals(150.6, raoData.getCrac().getBranchCnec("cnec1basecase").getExtension(CnecResultExtension.class).getVariant(var).getCommercialFlowInMW(), DOUBLE_TOLERANCE);
        assertEquals(653.7, raoData.getCrac().getBranchCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(var).getCommercialFlowInMW(), DOUBLE_TOLERANCE);
    }

    private SystematicSensitivityResult getMockSensiResult(Crac crac) {
        SystematicSensitivityResult sensisResults = Mockito.mock(SystematicSensitivityResult.class);

        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1basecase"))).thenReturn(10);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1stateCurativeContingency1"))).thenReturn(20);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec1stateCurativeContingency2"))).thenReturn(30);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2basecase"))).thenReturn(40);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2stateCurativeContingency1"))).thenReturn(50);
        Mockito.when(sensisResults.getReferenceFlow(crac.getBranchCnec("cnec2stateCurativeContingency2"))).thenReturn(60);

        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1basecase"))).thenReturn(1);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1stateCurativeContingency1"))).thenReturn(2);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec1stateCurativeContingency2"))).thenReturn(3);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2basecase"))).thenReturn(4);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2stateCurativeContingency1"))).thenReturn(5);
        Mockito.when(sensisResults.getSensitivityOnFlow(crac.getRangeAction("pst"), crac.getBranchCnec("cnec2stateCurativeContingency2"))).thenReturn(6);

        return sensisResults;
    }

    @Test
    public void testComputeMinMargins() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPstRange();
        crac.synchronize(network);
        RaoData raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());
        raoData.setSystematicSensitivityResult(getMockSensiResult(crac));

        raoData.getCracResultManager().computeMinMargins((PstRangeAction) crac.getRangeAction("pst"), new ArrayList<>(crac.getBranchCnecs()), 0, 1);
        assertTrue(true);
    }
}
