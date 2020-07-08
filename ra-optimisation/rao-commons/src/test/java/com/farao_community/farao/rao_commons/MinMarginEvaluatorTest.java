/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinMarginEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 1;

    private Crac crac;
    private RaoData raoData;
    private SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult;
    Network network;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        raoData = new RaoData(network, crac);

        systematicSensitivityAnalysisResult = Mockito.mock(SystematicSensitivityAnalysisResult.class);

        Mockito.when(systematicSensitivityAnalysisResult.getReferenceFlow(crac.getCnec("cnec1basecase")))
            .thenReturn(100.);
        Mockito.when(systematicSensitivityAnalysisResult.getReferenceFlow(crac.getCnec("cnec2basecase")))
            .thenReturn(200.);

        Mockito.when(systematicSensitivityAnalysisResult.getReferenceIntensity(any())).thenReturn(Double.NaN);
        Mockito.when(systematicSensitivityAnalysisResult.getReferenceIntensity(crac.getCnec("cnec1basecase")))
            .thenReturn(30.);
        Mockito.when(systematicSensitivityAnalysisResult.getReferenceIntensity(crac.getCnec("cnec2basecase")))
            .thenReturn(60.);

        raoData.setSystematicSensitivityAnalysisResult(systematicSensitivityAnalysisResult);
    }

    @Test
    public void getCostInMegawatt() {
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT);
        assertEquals(787, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereWithMissingValues() {
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE);
        assertEquals(1196, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereWithNoMissingValues() {
        Mockito.when(systematicSensitivityAnalysisResult.getReferenceIntensity(crac.getCnec("cnec1stateCurativeContingency1")))
            .thenReturn(10.);
        Mockito.when(systematicSensitivityAnalysisResult.getReferenceIntensity(crac.getCnec("cnec1stateCurativeContingency2")))
            .thenReturn(10.);
        Mockito.when(systematicSensitivityAnalysisResult.getReferenceIntensity(crac.getCnec("cnec2stateCurativeContingency1")))
            .thenReturn(10.);
        Mockito.when(systematicSensitivityAnalysisResult.getReferenceIntensity(crac.getCnec("cnec2stateCurativeContingency2")))
            .thenReturn(10.);
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE);
        assertEquals(1440, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testIgnoreMnecs() {
        crac.newCnec().setId("mnec1basecase")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setDirection(Direction.BOTH).setSide(Side.LEFT).setMaxValue(300.).setUnit(Unit.MEGAWATT).add()
                .setOptimized(false).setMonitored(true)
                .setInstant(crac.getInstant("initial"))
                .add();

        crac.desynchronize();
        RaoInput.synchronize(crac, network);

        Mockito.when(systematicSensitivityAnalysisResult.getReferenceFlow(crac.getCnec("mnec1basecase")))
                .thenReturn(200.);
        Mockito.when(systematicSensitivityAnalysisResult.getReferenceIntensity(crac.getCnec("mnec1basecase")))
                .thenReturn(60.);

        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT);
        assertEquals(787, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE);
        assertEquals(1196, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }
}
