/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *//*


package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

*/
/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 *//*

@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class IteratingLinearOptimizerTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private IteratingLinearOptimizerParameters parameters;
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private LinearOptimizer linearOptimizer;
    private ObjectiveFunctionEvaluator costEvaluator;
    private Crac crac;
    private RaoData raoData;
    private List<String> workingVariants;

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Before
    public void setUp() {
        mockNativeLibraryLoader();

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        raoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters());
        parameters = new IteratingLinearOptimizerParameters(10, 0);

        workingVariants = new ArrayList<>();
        systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        linearOptimizer = Mockito.mock(LinearOptimizer.class);
        // TODO: PowerMockito.whenNew(LinearOptimizer.class).withAnyArguments().

        SystematicSensitivityResult systematicSensitivityResult1 = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityResult systematicSensitivityResult2 = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityResult systematicSensitivityResult3 = Mockito.mock(SystematicSensitivityResult.class);
        SystematicSensitivityResult systematicSensitivityResult4 = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult1.getReferenceFlow(Mockito.any())).thenReturn(100.);
        Mockito.when(systematicSensitivityResult2.getReferenceFlow(Mockito.any())).thenReturn(50.);
        Mockito.when(systematicSensitivityResult3.getReferenceFlow(Mockito.any())).thenReturn(20.);
        Mockito.when(systematicSensitivityResult3.getReferenceFlow(Mockito.any())).thenReturn(0.);
        Mockito.when(systematicSensitivityInterface.run(Mockito.any()))
            .thenReturn(systematicSensitivityResult1, systematicSensitivityResult2, systematicSensitivityResult3, systematicSensitivityResult4);

        // mock linear optimisation engine
        // linear optimisation returns always the same value -> optimal solution is 1.0 for all RAs
        doAnswer(new Answer() {
            private int count = 1;
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                RaoData raoData = (RaoData) args[0];
                double setPoint;
                double cost;
                switch (count) {
                    case 1:
                        setPoint = 1.0;
                        cost = 50.;
                        break;
                    case 2:
                        setPoint = 3.0;
                        cost = 20;
                        break;
                    case 3:
                        setPoint = 3.0;
                        cost = 0;
                        break;
                    default:
                        setPoint = 0;
                        cost = 0;
                        break;
                }
                workingVariants.add(raoData.getWorkingVariantId());
                crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
                    .getVariant(raoData.getWorkingVariantId())
                    .setSetPoint(crac.getPreventiveState().getId(), setPoint);
                crac.getExtension(CracResultExtension.class).getVariant(raoData.getWorkingVariantId())
                    .setFunctionalCost(cost);
                count += 1;

                return raoData;
            }
        }).when(linearOptimizer).optimize(any());

        costEvaluator = Mockito.mock(ObjectiveFunctionEvaluator.class);
        Mockito.when(costEvaluator.computeFunctionalCost(raoData)).thenReturn(0.);
        Mockito.when(costEvaluator.computeVirtualCost(raoData)).thenReturn(0.);
    }

    @Test
    public void optimize() {
        String preOptimVariant = raoData.getPreOptimVariantId();

        Mockito.when(linearOptimizer.getSolverResultStatusString()).thenReturn("OPTIMAL");
        Mockito.when(costEvaluator.computeFunctionalCost(Mockito.any())).thenReturn(100., 50., 20., 0.);

        // run an iterating optimization
        String bestVariantId = new IteratingLinearOptimizer(
            systematicSensitivityInterface,
            costEvaluator,
            linearOptimizer,
            parameters).optimize(raoData);

        // check results
        assertNotNull(bestVariantId);
        assertEquals(100, crac.getExtension(CracResultExtension.class).getVariant(preOptimVariant).getCost(), DOUBLE_TOLERANCE);
        assertEquals(20, crac.getExtension(CracResultExtension.class).getVariant(bestVariantId).getCost(), DOUBLE_TOLERANCE);

        // In the end CRAC should contain results only for pre-optim variant and post-optim variant
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains(preOptimVariant));
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains(workingVariants.get(1)));
        assertFalse(crac.getExtension(ResultVariantManager.class).getVariants().contains(workingVariants.get(0)));

        assertEquals(0, crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
            .getVariant(preOptimVariant)
            .getSetPoint(crac.getPreventiveState().getId()), DOUBLE_TOLERANCE);
        assertEquals(0, crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
            .getVariant(preOptimVariant)
            .getSetPoint(crac.getState("N-1 NL1-NL3", Instant.OUTAGE).getId()), DOUBLE_TOLERANCE);

        assertEquals(3, crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
            .getVariant(bestVariantId)
            .getSetPoint(crac.getPreventiveState().getId()), DOUBLE_TOLERANCE);
    }

    @Test
    public void optimizeWithInfeasibility() {
        String preOptimVariant = raoData.getWorkingVariantId();

        Mockito.when(linearOptimizer.getSolverResultStatusString()).thenReturn("INFEASIBLE");
        Mockito.when(costEvaluator.computeFunctionalCost(Mockito.any())).thenReturn(100., 50., 20., 0.);

        // run an iterating optimization
        String bestVariantId = new IteratingLinearOptimizer(
            systematicSensitivityInterface,
            costEvaluator,
            linearOptimizer,
            parameters).optimize(raoData);

        // check results
        assertNotNull(bestVariantId);
        assertEquals(100, crac.getExtension(CracResultExtension.class).getVariant(preOptimVariant).getCost(), DOUBLE_TOLERANCE);
        assertEquals(100, crac.getExtension(CracResultExtension.class).getVariant(bestVariantId).getCost(), DOUBLE_TOLERANCE);

        // In the end CRAC should contain results only for pre-optim variant and post-optim variant
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains(preOptimVariant));
        assertFalse(crac.getExtension(ResultVariantManager.class).getVariants().contains(workingVariants.get(0)));

        assertEquals(0, crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
            .getVariant(preOptimVariant)
            .getSetPoint(crac.getPreventiveState().getId()), DOUBLE_TOLERANCE);
        assertEquals(0, crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
            .getVariant(preOptimVariant)
            .getSetPoint(crac.getState("N-1 NL1-NL3", Instant.OUTAGE).getId()), DOUBLE_TOLERANCE);
    }

    /**
     * Create a situation with 2 PSTs of the same operator
     */
    private void setUpWithTwoPsts() {
        network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        crac = CommonCracCreation.create();

        rangeAction1 = new PstRangeActionImpl("PST_FR_1", "PST_FR_1", "FR", new NetworkElement("FFR1AA1  FFR2AA1  2"));
        rangeAction1.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        ((SimpleCrac) crac).addRangeAction(rangeAction1);

        rangeAction2 = new PstRangeActionImpl("PST_FR_2", "PST_FR_2", "FR", new NetworkElement("BBE1AA1  BBE3AA1  2"));
        rangeAction2.addUsageRule(new OnStateImpl(UsageMethod.AVAILABLE, crac.getPreventiveState()));
        ((SimpleCrac) crac).addRangeAction(rangeAction2);

        crac.synchronize(network);

        cnec1 = crac.getBranchCnec("cnec1basecase");
        cnec2 = crac.getBranchCnec("cnec2basecase");

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
*/
