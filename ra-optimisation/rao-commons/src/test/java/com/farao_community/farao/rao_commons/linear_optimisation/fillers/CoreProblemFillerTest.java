/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.TapConvention;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.data.crac_impl.NetworkElementImpl;
import com.farao_community.farao.data.crac_impl.PstRangeActionImpl;
import com.farao_community.farao.data.crac_impl.OnStateImpl;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.import_.Importers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RaoUtil.class})
public class CoreProblemFillerTest extends AbstractFillerTest {

    private RangeAction rangeAction1;
    private RangeAction rangeAction2;

    @Before
    public void setUp() {
        init();
        // arrange some additional data
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        coreProblemFiller = new CoreProblemFiller(2.5, null);
    }

    @Test
    public void fillTestOnPreventive() {
        coreProblemFiller = new CoreProblemFiller(0, null);
        initRaoData(crac.getPreventiveState());
        coreProblemFiller.fill(raoData, linearProblem);

        // some additional data
        final double minAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMinValue(0);
        final double maxAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMaxValue(0);
        final double currentAlpha = raoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        // check range action setpoint variable
        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - currentAlpha * SENSI_CNEC1_IT1, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT1 - currentAlpha * SENSI_CNEC1_IT1, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT1, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNull(flowVariable2);

        // check flow constraint for cnec2 does not exist
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNull(flowConstraint2);

        // check absolute variation constraints
        MPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE);
        MPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-currentAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(currentAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(3, linearProblem.getSolver().numVariables());
        assertEquals(3, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void fillTestOnPreventiveFiltered() {
        coreProblemFiller = new CoreProblemFiller(2.5, null);
        initRaoData(crac.getPreventiveState());
        coreProblemFiller.fill(raoData, linearProblem);

        // some additional data
        final double minAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMinValue(0);
        final double maxAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMaxValue(0);
        final double currentAlpha = raoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        // check range action setpoint variable
        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT1 - currentAlpha * 0, flowConstraint.lb(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(REF_FLOW_CNEC1_IT1 - currentAlpha * 0, flowConstraint.ub(), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(0, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE); // sensitivity filtered (= 0)

        // check flow variable for cnec2 does not exist
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNull(flowVariable2);

        // check flow constraint for cnec2 does not exist
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNull(flowConstraint2);

        // check absolute variation constraints
        MPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE);
        MPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-currentAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(currentAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(3, linearProblem.getSolver().numVariables());
        assertEquals(3, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void fillTestOnCurative() {
        coreProblemFiller = new CoreProblemFiller(0, null);
        initRaoData(crac.getState("N-1 NL1-NL3", Instant.OUTAGE));
        coreProblemFiller.fill(raoData, linearProblem);

        // some additional data
        final double minAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMinValue(0);
        final double maxAlpha = crac.getRangeAction(RANGE_ACTION_ID).getMaxValue(0);
        final double currentAlpha = raoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        // check range action setpoint variable
        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        assertEquals(minAlpha, setPointVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1 does not exist
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNull(flowVariable);

        // check flow constraint for cnec1 does not exist
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNull(flowConstraint);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT1 - currentAlpha * SENSI_CNEC2_IT1, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT1 - currentAlpha * SENSI_CNEC2_IT1, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT1, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check absolute variation constraints
        MPConstraint absoluteVariationConstraint1 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE);
        MPConstraint absoluteVariationConstraint2 = linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(absoluteVariationConstraint1);
        assertNotNull(absoluteVariationConstraint2);
        assertEquals(-currentAlpha, absoluteVariationConstraint1.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint1.ub(), DOUBLE_TOLERANCE);
        assertEquals(currentAlpha, absoluteVariationConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationConstraint2.ub(), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(3, linearProblem.getSolver().numVariables());
        assertEquals(3, linearProblem.getSolver().numConstraints());
    }

    private void updateProblemWithCoreFiller() {
        // arrange some additional data
        raoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_IT2);

        when(systematicSensitivityResult.getReferenceFlow(cnec1)).thenReturn(REF_FLOW_CNEC1_IT2);
        when(systematicSensitivityResult.getReferenceFlow(cnec2)).thenReturn(REF_FLOW_CNEC2_IT2);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction, cnec1)).thenReturn(SENSI_CNEC1_IT2);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction, cnec2)).thenReturn(SENSI_CNEC2_IT2);

        // fill the problem
        coreProblemFiller.update(raoData, linearProblem);
    }

    @Test
    public void updateTestOnPreventive() {
        initRaoData(crac.getPreventiveState());

        // fill a first time the linearRaoProblem with some data
        coreProblemFiller.fill(raoData, linearProblem);

        // update the problem with new data
        updateProblemWithCoreFiller();

        // some additional data
        final double currentAlpha = raoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);

        // check flow variable for cnec1
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC1_IT2 - currentAlpha * SENSI_CNEC1_IT2, flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-SENSI_CNEC1_IT2, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2 does not exist
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNull(flowVariable2);

        // check flow constraint for cnec2 does not exist
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNull(flowConstraint2);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(3, linearProblem.getSolver().numVariables());
        assertEquals(3, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void updateTestOnCurative() {
        initRaoData(crac.getState("N-1 NL1-NL3", Instant.OUTAGE));
        // fill a first time the linearRaoProblem with some data
        coreProblemFiller.fill(raoData, linearProblem);

        // update the problem with new data
        updateProblemWithCoreFiller();

        // some additional data
        final double currentAlpha = raoData.getNetwork().getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();

        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);

        // check flow variable for cnec1 does not exist
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1);
        assertNull(flowVariable);

        // check flow constraint for cnec1 does not exist
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec1);
        assertNull(flowConstraint);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(REF_FLOW_CNEC2_IT2 - currentAlpha * SENSI_CNEC2_IT2, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-SENSI_CNEC2_IT2, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 4 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation)
        // total number of constraints 4 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints)
        assertEquals(3, linearProblem.getSolver().numVariables());
        assertEquals(3, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void testFillerWithRangeActionGroup() {



        crac.newPstRangeAction()
                .withId("pst1-group1")
                .withGroupId("group1")
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .newTapRange()
                    .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                    .withRangeType(RangeType.ABSOLUTE)
                    .withMinTap(-2)
                    .withMaxTap(5)
                    .add()
                .withOperator("RTE")
                .add();
        crac.newPstRangeAction()
                .withId("pst2-group1")
                .withGroupId("group1")
                .withNetworkElement("BBE1AA1  BBE3AA1  1")
                .newTapRange()
                    .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                    .withRangeType(RangeType.ABSOLUTE)
                    .withMinTap(-5)
                    .withMaxTap(10)
                    .add()
                .withOperator("RTE")
                .add();

        network = NetworkImportsUtil.import12NodesWith2PstsNetwork();
        crac.desynchronize();
        crac.synchronize(network);
        initRaoData(crac.getPreventiveState());

        // fill a first time the linearRaoProblem with some data
        coreProblemFiller.fill(raoData, linearProblem);

        // check the number of variables and constraints
        // total number of variables 8 :
        //      - 1 per CNEC (flow)
        //      - 2 per range action (set-point and variation) x 3
        //      - 1 per group
        // total number of constraints 9 :
        //      - 1 per CNEC (flow constraint)
        //      - 2 per range action (absolute variation constraints) x 3
        //      - 1 per range action in a group (group constraint) x 2
        assertEquals(8, linearProblem.getSolver().numVariables());
        assertEquals(9, linearProblem.getSolver().numConstraints());
    }

    @Test
    public void updateWithoutFillingTest() {
        initRaoData(crac.getPreventiveState());
        try {
            updateProblemWithCoreFiller();
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    /**
     * Create a situation with 2 PSTs of the same operator
     */
    private void setUpWithTwoPsts() {
        network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        crac = CommonCracCreation.create();

        rangeAction1 = crac.newPstRangeAction()
            .withId("PST_FR_1")
            .withOperator("FR")
            .withNetworkElement("FFR1AA1  FFR2AA1  2")
            .newFreeToUseUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.PREVENTIVE)
                .add()
            .add();

        rangeAction1 = crac.newPstRangeAction()
            .withId("PST_FR_2")
            .withOperator("FR")
            .withNetworkElement("BBE1AA1  BBE3AA1  2")
            .newFreeToUseUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .withInstant(Instant.PREVENTIVE)
                .add()
            .add();

        crac.synchronize(network);

        cnec1 = crac.getFlowCnec("cnec1basecase");
        cnec2 = crac.getFlowCnec("cnec2basecase");

        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction1, cnec1)).thenReturn(-30.0);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction2, cnec1)).thenReturn(-25.0);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction1, cnec2)).thenReturn(10.0);
        when(systematicSensitivityResult.getSensitivityOnFlow(rangeAction2, cnec2)).thenReturn(-40.0);

        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());
        raoData.getCracResultManager().fillRangeActionResultsWithNetworkValues();
        raoData.setSystematicSensitivityResult(systematicSensitivityResult);
        raoData.getCrac().getExtension(ResultVariantManager.class).setInitialVariantId(raoData.getWorkingVariantId());
        raoData.getCrac().getExtension(ResultVariantManager.class).setPrePerimeterVariantId(raoData.getWorkingVariantId());

        PowerMockito.mockStatic(RaoUtil.class);
        PowerMockito.when(RaoUtil.getMostLimitingElement(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyBoolean())).thenAnswer(invocationOnMock -> cnec2);
    }

    @Test
    public void testCompareAbsoluteSensitivities() {
        setUpWithTwoPsts();

        assertEquals(1, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction1, rangeAction2, cnec1, raoData));
        assertEquals(-1, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction2, rangeAction1, cnec1, raoData));
        assertEquals(0, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction1, rangeAction1, cnec1, raoData));
        assertEquals(0, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction2, rangeAction2, cnec1, raoData));

        assertEquals(-1, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction1, rangeAction2, cnec2, raoData));
        assertEquals(1, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction2, rangeAction1, cnec2, raoData));
        assertEquals(0, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction1, rangeAction1, cnec2, raoData));
        assertEquals(0, CoreProblemFiller.compareAbsoluteSensitivities(rangeAction2, rangeAction2, cnec2, raoData));
    }

    @Test
    public void testFilterTwoPsts() {
        setUpWithTwoPsts();
        // Both PSTs should be filtered out
        coreProblemFiller = new CoreProblemFiller(0, Map.of("FR", 0));
        coreProblemFiller.fill(raoData, linearProblem);
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }

    @Test
    public void testFilterPst1() {
        setUpWithTwoPsts();
        // One PST can be used, cnec2 is most limiting, rangeAction2 has a larger sensitivity on cnec2
        // Thus rangeAction2 can be used, rangeAction1 should be filtered out
        coreProblemFiller = new CoreProblemFiller(0, Map.of("FR", 1));
        coreProblemFiller.fill(raoData, linearProblem);
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }

    @Test
    public void testFilterPst2() {
        setUpWithTwoPsts();
        PowerMockito.when(RaoUtil.getMostLimitingElement(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyBoolean())).thenAnswer(invocationOnMock -> cnec1);
        // One PST can be used, cnec1 is most limiting, rangeAction1 has a larger sensitivity on cnec1
        // Thus rangeAction1 can be used, rangeAction2 should be filtered out
        coreProblemFiller = new CoreProblemFiller(0, Map.of("FR", 1));
        coreProblemFiller.fill(raoData, linearProblem);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }

    @Test
    public void testDontFilterPst1() {
        setUpWithTwoPsts();
        // no need to filter out PSTs
        coreProblemFiller = new CoreProblemFiller(0, Map.of("FR", 2));
        coreProblemFiller.fill(raoData, linearProblem);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }

    @Test
    public void testDontFilterPst2() {
        setUpWithTwoPsts();
        // no need to filter out PSTs
        coreProblemFiller = new CoreProblemFiller(0, null);
        coreProblemFiller.fill(raoData, linearProblem);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }

    @Test
    public void testDontFilterPst3() {
        setUpWithTwoPsts();
        // no need to filter out PSTs
        coreProblemFiller = new CoreProblemFiller(0, new HashMap<>());
        coreProblemFiller.fill(raoData, linearProblem);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction1));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction2));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction1));
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction2));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction1, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.POSITIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction2, LinearProblem.AbsExtension.NEGATIVE));
    }

    private void testFilterWrongRangeActions(int initialTapPosition, boolean shouldBeFiltered) {
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(initialTapPosition);
        initRaoData(crac.getPreventiveState());
        coreProblemFiller.fill(raoData, linearProblem);
        MPVariable variable = linearProblem.getAbsoluteRangeActionVariationVariable(crac.getRangeAction(RANGE_ACTION_ID));
        if (shouldBeFiltered) {
            assertNull(variable);
        } else {
            assertNotNull(variable);
        }
    }

    @Test
    public void testFilterWrongRangeActions1() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(-15, false);
    }

    @Test
    public void testFilterWrongRangeActions2() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(15, false);
    }

    @Test
    public void testFilterWrongRangeActions3() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(-1, false);
    }

    @Test
    public void testFilterWrongRangeActions4() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(-16, true);
    }

    @Test
    public void testFilterWrongRangeActions5() {
        // PST has tap limits of -15 / +15
        testFilterWrongRangeActions(16, true);
    }
}
