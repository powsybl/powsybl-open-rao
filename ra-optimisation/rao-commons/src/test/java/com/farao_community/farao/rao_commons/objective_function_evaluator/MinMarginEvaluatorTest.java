/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoInputHelper;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinMarginEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 1;

    private Crac crac;
    private RaoData raoData;
    private SystematicSensitivityResult systematicSensitivityResult;
    private Network network;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        raoData = RaoData.createOnPreventiveState(network, crac);

        setPtdfSum("cnec1basecase", 0.5);
        setPtdfSum("cnec1stateCurativeContingency1", 0.95);
        setPtdfSum("cnec1stateCurativeContingency2", 0.95);
        setPtdfSum("cnec2basecase", 0.4);
        setPtdfSum("cnec2stateCurativeContingency1", 0.6);
        setPtdfSum("cnec2stateCurativeContingency2", 0.6);

        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);

        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("cnec1basecase")))
                .thenReturn(100.);
        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("cnec2basecase")))
                .thenReturn(200.);

        Mockito.when(systematicSensitivityResult.getReferenceIntensity(any())).thenReturn(Double.NaN);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1basecase")))
                .thenReturn(30.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec2basecase")))
                .thenReturn(60.);

        raoData.setSystematicSensitivityResult(systematicSensitivityResult);
    }

    private void setPtdfSum(String cnecId, double ptdfSum) {
        crac.getBranchCnec(cnecId).getExtension(CnecResultExtension.class).getVariant(raoData.getInitialVariantId()).setAbsolutePtdfSum(ptdfSum);
    }

    @Test
    public void getCostInMegawatt() {
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, false);
        assertEquals(-787, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, true, 0.01);
        assertEquals(-1968, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereWithMissingValues() {
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, false);
        assertEquals(-1440, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, true, 0.01);
        assertEquals(-3600, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereWithNoMissingValues() {
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1stateCurativeContingency1")))
                .thenReturn(10.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1stateCurativeContingency2")))
                .thenReturn(10.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec2stateCurativeContingency1")))
                .thenReturn(10.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec2stateCurativeContingency2")))
                .thenReturn(10.);
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, false);
        assertEquals(-1440, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, true, 0.01);
        assertEquals(-3600, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testIgnoreMnecs() {
        crac.newBranchCnec().setId("mnec1basecase")
                .newNetworkElement().setId("DDE2AA1  NNL3AA1  1").add()
                .newThreshold().setRule(BranchThresholdRule.ON_LEFT_SIDE).setMax(300.).setMin(-300.).setUnit(Unit.MEGAWATT).add()
                .optimized().monitored()
                .setInstant(crac.getInstant("initial"))
                .add();

        crac.desynchronize();
        RaoInputHelper.synchronize(crac, network);

        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("mnec1basecase")))
                .thenReturn(200.);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("mnec1basecase")))
                .thenReturn(60.);

        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, false);
        assertEquals(-787, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, false);
        assertEquals(-1440, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void testMinimumPtdfSum() {
        setPtdfSum("cnec1basecase", 0.005);
        setPtdfSum("cnec1stateCurativeContingency1", 0.0095);
        setPtdfSum("cnec1stateCurativeContingency2", 0.0095);
        setPtdfSum("cnec2basecase", 0.004);
        setPtdfSum("cnec2stateCurativeContingency1", 0.006);
        setPtdfSum("cnec2stateCurativeContingency2", 0.006);

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, true, 0.02);
        assertEquals(-39363, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void testRequirePtdfSumLb() {
        new MinMarginEvaluator(Unit.MEGAWATT, true);
    }
}
