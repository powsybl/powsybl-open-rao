/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
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
}
