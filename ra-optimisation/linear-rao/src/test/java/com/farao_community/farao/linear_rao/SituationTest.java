/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class})
public class SituationTest {
    private static final double PRECISION_FLOW = 1.0;
    private static final double PRECISION_SET_POINT = 1.0;

    Network network;
    Crac crac;
    ComputationManager computationManager;
    SensitivityComputationParameters sensitivityComputationParameters;
    SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");
        PstRange pstRange = new PstWithRange("RA PST BE", pstElement);
        ((SimpleCrac) crac).addRangeAction(pstRange);
        crac.synchronize(network);

        computationManager = Mockito.mock(ComputationManager.class);
        sensitivityComputationParameters = Mockito.mock(SensitivityComputationParameters.class);
        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);
        Map<Cnec, Double> cnecFlowMap = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecFlowMap.put(cnec, 499.));
        systematicSensitivityAnalysisResult = new SystematicSensitivityAnalysisResult(new HashMap<>(), cnecFlowMap, new HashMap<>());
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(new SystematicSensitivityAnalysisResult(new HashMap<>(), cnecFlowMap, new HashMap<>()));
    }

    @Test
    public void initialSituationTest() {
        InitialSituation initialSituation = new InitialSituation(network, network.getVariantManager().getWorkingVariantId(), crac);

        assertNotNull(crac.getExtension(CracResultExtension.class));

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());

        initialSituation.deleteResultVariant();
        // We never want the initial situation to delete its variant. Allows us to call delete variant on any situation without having to worry if it's the initial one or not.
        assertEquals(1, resultVariantManager.getVariants().size());
    }

    @Test
    public void optimizedSituationTest() {
        OptimizedSituation optimizedSituation = new OptimizedSituation(network, network.getVariantManager().getWorkingVariantId(), crac);

        assertNotNull(crac.getExtension(CracResultExtension.class));

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());

        optimizedSituation.deleteResultVariant();
        assertEquals(0, resultVariantManager.getVariants().size());
    }

    @Test
    public void sameRasTest() {
        OptimizedSituation sameSituation1 = new OptimizedSituation(network, network.getVariantManager().getWorkingVariantId(), crac);
        OptimizedSituation sameSituation2 = new OptimizedSituation(network, network.getVariantManager().getWorkingVariantId(), crac);
        OptimizedSituation differentSituation = new OptimizedSituation(network, network.getVariantManager().getWorkingVariantId(), crac);

        String variant1 = sameSituation1.getCracResultVariant();
        String variant2 = sameSituation2.getCracResultVariant();
        String variant3 = differentSituation.getCracResultVariant();

        RangeActionResultExtension rangeActionResultExtension = crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class);
        RangeActionResult pstResult1 = rangeActionResultExtension.getVariant(variant1);
        RangeActionResult pstResult2 = rangeActionResultExtension.getVariant(variant2);
        RangeActionResult pstResult3 = rangeActionResultExtension.getVariant(variant3);

        String prevStateId = crac.getPreventiveState().getId();
        pstResult1.setSetPoint(prevStateId, 3);
        pstResult2.setSetPoint(prevStateId, 3);
        pstResult3.setSetPoint(prevStateId, 2);

        assertTrue(sameSituation1.sameRaResults(sameSituation2));
        assertTrue(sameSituation2.sameRaResults(sameSituation1));
        assertFalse(sameSituation1.sameRaResults(differentSituation));
    }
}
