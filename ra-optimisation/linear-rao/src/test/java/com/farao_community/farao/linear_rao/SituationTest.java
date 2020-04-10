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
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
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
        InitialSituation initialSituation = new InitialSituation(network, crac);

        assertNotNull(crac.getExtension(CracResultExtension.class));

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(1, resultVariantManager.getVariants().size());

        initialSituation.setResults(systematicSensitivityAnalysisResult);
        assertEquals(-488, initialSituation.getCost(), PRECISION_FLOW);

        String variant = initialSituation.getResultVariant();
        String preventive = crac.getPreventiveState().getId();
        assertEquals(499, crac.getCnecs().iterator().next().getExtension(CnecResultExtension.class).getVariant(variant).getFlowInMW(), PRECISION_FLOW);
        assertEquals(0, crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class).getVariant(variant).getSetPoint(preventive), PRECISION_SET_POINT);

        initialSituation.deleteResultVariant();
        // We don't want to delete the initial variant
        assertEquals(1, resultVariantManager.getVariants().size());
    }

    @Test
    public void optimizedSituationTest() {
        //Needed to create the variant manager
        new InitialSituation(network, crac);

        OptimizedSituation optimizedSituation = new OptimizedSituation(network, crac);

        assertNotNull(crac.getExtension(CracResultExtension.class));

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        assertEquals(2, resultVariantManager.getVariants().size());

        LinearOptimisationEngine linearOptimisationEngine = Mockito.mock(LinearOptimisationEngine.class);

        optimizedSituation.setResults(systematicSensitivityAnalysisResult);
        assertEquals(-488, optimizedSituation.getCost(), PRECISION_FLOW);

        String variant = optimizedSituation.getResultVariant();
        String preventive = crac.getPreventiveState().getId();
        assertEquals(499, crac.getCnecs().iterator().next().getExtension(CnecResultExtension.class).getVariant(variant).getFlowInMW(), PRECISION_FLOW);
        assertTrue(Double.isNaN(crac.getRangeActions().iterator().next().getExtension(RangeActionResultExtension.class).getVariant(variant).getSetPoint(preventive)));

        optimizedSituation.deleteResultVariant();
        assertEquals(1, resultVariantManager.getVariants().size());
    }
}
