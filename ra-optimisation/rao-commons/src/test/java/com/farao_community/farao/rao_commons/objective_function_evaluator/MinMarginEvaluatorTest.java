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
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoInputHelper;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

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
    private String initialVariant;

    @Before
    public void setUp() {
        crac = CommonCracCreation.create();
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());
        initialVariant = raoData.getPreOptimVariantId();
        crac.getExtension(ResultVariantManager.class).setInitialVariantId(initialVariant);

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
        crac.getBranchCnec(cnecId).getExtension(CnecResultExtension.class).getVariant(initialVariant).setAbsolutePtdfSum(ptdfSum);
    }

    @Test
    public void getCostInMegawatt() {
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, null, false);
        assertEquals(-787, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, null, true, 0.01);
        assertEquals(-787 / 0.4, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInMegawattSkipOperatorsNotToOptimize() {
        // cnec1 has a margin of 1400 MW "after optim"
        // cnec2 has a margin of 787 MW "after optim"
        BranchCnec cnec1 = raoData.getCrac().getBranchCnec("cnec1basecase");
        BranchCnec cnec2 = raoData.getCrac().getBranchCnec("cnec2basecase");

        String mockPrePerimeterVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCracResultManager().copyAbsolutePtdfSumsBetweenVariants(initialVariant, mockPrePerimeterVariantId);
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(mockPrePerimeterVariantId);

        // If operator 2 doesn't share RA
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, Collections.singleton("operator2"), false);
        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, Collections.singleton("operator2"), true, 0.01);
        cnec1.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInMW(100.0);

        // case 0 : margin on cnec2 is the same => cost is equal to margin on cnec1
        // (we're setting the 'old' flow here)
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInMW(200.0);
        assertEquals(-1400, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-1400 / 0.5, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        // case 1 : margin on cnec2 is improved => cost is equal to margin on cnec1
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInMW(300.0);
        assertEquals(-1400, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-1400 / 0.5, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        // case 2 : margin on cnec2 is the slightly improved => cost is equal to margin on cnec2
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInMW(201.0);
        assertEquals(-1400, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-1400 / 0.5, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        // case 3 : margin on cnec2 is decreased and worse than on cnec1 => cost is equal to margin on cnec2
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInMW(100.0);
        assertEquals(-787, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-787 / 0.4, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        // case 4 : margin on cnec2 is decreased but better than on cnec1 => cost is equal to margin on cnec1
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInMW(100.0);
        Mockito.when(systematicSensitivityResult.getReferenceFlow(crac.getBranchCnec("cnec1basecase"))).thenReturn(1000.);
        assertEquals(-500, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-500 / 0.5, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereWithMissingValues() {
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, null, false);
        assertEquals(-1440, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, null, true, 0.01);
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
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, null, false);
        assertEquals(-1440, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, null, true, 0.01);
        assertEquals(-3600, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test
    public void getCostInAmpereSkipOperatorsNotToOptimize() {
        // cnec1 has a margin of 2249 A "after optim"
        // cnec2 has a margin of 1440 A "after optim"
        BranchCnec cnec1 = raoData.getCrac().getBranchCnec("cnec1basecase");
        BranchCnec cnec2 = raoData.getCrac().getBranchCnec("cnec2basecase");

        String mockPrePerimeterVariantId = raoData.getCracVariantManager().cloneWorkingVariant();
        raoData.getCracResultManager().copyAbsolutePtdfSumsBetweenVariants(initialVariant, mockPrePerimeterVariantId);
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(mockPrePerimeterVariantId);

        // If operator 2 doesn't share RA
        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, Collections.singleton("operator2"), false);
        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, Collections.singleton("operator2"), true, 0.01);
        cnec1.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInA(30.0);

        // case 0 : margin on cnec2 is same => cost is equal to margin on cnec1
        // (we're setting the 'old' flow here)
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInA(60.0);
        assertEquals(-2249, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-2249 / 0.5, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        // case 1 : margin on cnec2 is improved => cost is equal to margin on cnec1
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInA(70.);
        assertEquals(-2249, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-2249 / 0.5, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        // case 2 : margin on cnec2 is slightly improved => cost is equal to margin on cnec1
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInA(61.);
        assertEquals(-2249, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-2249 / 0.5, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        // case 3 : margin on cnec2 is decreased and worse than on cnec1 => cost is equal to margin on cnec2
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInA(59.0);
        assertEquals(-1440, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-1440 / 0.4, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        // case 4 : margin on cnec2 is decreased but better than on cnec1 => cost is equal to margin on cnec1
        cnec2.getExtension(CnecResultExtension.class).getVariant(mockPrePerimeterVariantId).setFlowInA(59.0);
        Mockito.when(systematicSensitivityResult.getReferenceIntensity(crac.getBranchCnec("cnec1basecase"))).thenReturn(1300.);
        assertEquals(-979, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
        assertEquals(-979 / 0.5, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
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

        MinMarginEvaluator minMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, null, false);
        assertEquals(-787, minMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);

        minMarginEvaluator = new MinMarginEvaluator(Unit.AMPERE, null, false);
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

        MinMarginEvaluator minRelativeMarginEvaluator = new MinMarginEvaluator(Unit.MEGAWATT, null, true, 0.02);
        assertEquals(-39363, minRelativeMarginEvaluator.getCost(raoData), DOUBLE_TOLERANCE);
    }

    @Test(expected = FaraoException.class)
    public void testRequirePtdfSumLb() {
        new MinMarginEvaluator(Unit.MEGAWATT, null, true);
    }

    @Test
    public void testMarginsInAmpereFromMegawattConversion() {
        List<Double> margins = new MinMarginEvaluator(Unit.MEGAWATT, null, true, 0.001).getMarginsInAmpereFromMegawattConversion(raoData);
        assertEquals(2, margins.size());
        assertEquals(2990, margins.get(0), DOUBLE_TOLERANCE);
        assertEquals(4254, margins.get(1), DOUBLE_TOLERANCE);
    }
}
