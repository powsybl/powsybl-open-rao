/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoDataManager;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizer;
import com.farao_community.farao.rao_commons.SystematicSensitivityComputation;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class})
public class IteratingLinearOptimizerTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private IteratingLinearOptimizerParameters parameters;
    private SystematicSensitivityComputation systematicSensitivityComputation;
    private LinearOptimizer linearOptimizer;
    private Crac crac;
    private RaoData raoData;

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        PowerMockito.doNothing().when(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Before
    public void setUp() {
        mockNativeLibraryLoader();

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        raoData = new RaoData(network, crac);
        parameters = new IteratingLinearOptimizerParameters(Unit.MEGAWATT, 10, 0);

        systematicSensitivityComputation = Mockito.mock(SystematicSensitivityComputation.class);

        linearOptimizer = Mockito.mock(LinearOptimizer.class);
    }

    @Test
    public void optimize() {
        List<String> workingVariants = new ArrayList<>();
        String preOptimVariant = raoData.getWorkingVariantId();

        RaoData spiedRaoData = Mockito.spy(raoData);
        RaoDataManager spiedRaoDataManager = Mockito.spy(raoData.getRaoDataManager());
        Mockito.when(spiedRaoData.getRaoDataManager()).thenReturn(spiedRaoDataManager);
        Mockito.doNothing().when(spiedRaoDataManager).fillCracResultsWithSensis(any(), anyDouble());

        // mock sensitivity engine
        // sensitivity computation returns a cost of 100 before optim, and 50 after optim
        doAnswer(new Answer() {

            private int count = 1;

            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                RaoData raoData = (RaoData) args[0];
                double cost;
                switch (count) {
                    case 1:
                        cost = 100;
                        break;
                    case 2:
                        cost = 50;
                        break;
                    case 3:
                        cost = 20;
                        break;
                    default:
                        cost = 0;
                        break;
                }
                raoData.getCracResult().setFunctionalCost(cost);
                crac.getExtension(CracResultExtension.class).getVariant(raoData.getWorkingVariantId()).setFunctionalCost(cost);
                count += 1;
                return null;
            }
        }).when(systematicSensitivityComputation).run(any(), any());

        // mock linear optimisation engine
        // linear optimisation returns always the same value -> optimal solution is 1.0 for all RAs
        doAnswer(new Answer() {
            private int count = 1;
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                RaoData raoData = (RaoData) args[0];
                double setPoint;
                switch (count) {
                    case 1:
                        setPoint = 1.0;
                        break;
                    case 2:
                    case 3:
                        setPoint = 3.0;
                        break;
                    default:
                        setPoint = 0;
                        break;
                }
                workingVariants.add(raoData.getWorkingVariantId());
                crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
                    .getVariant(raoData.getWorkingVariantId())
                    .setSetPoint(crac.getPreventiveState().getId(), setPoint);
                count += 1;

                return raoData;
            }
        }).when(linearOptimizer).optimize(any());

        Mockito.when(linearOptimizer.getSolverResultStatusString()).thenReturn("OPTIMAL");

        systematicSensitivityComputation.run(spiedRaoData, Unit.MEGAWATT);
        // run an iterating optimization
        String bestVariantId = new IteratingLinearOptimizer(
            systematicSensitivityComputation,
            linearOptimizer,
            parameters).optimize(spiedRaoData);

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
        assertEquals(3, crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
            .getVariant(bestVariantId)
            .getSetPoint(crac.getPreventiveState().getId()), DOUBLE_TOLERANCE);

    }

    @Test
    public void optimizeWithInfeasibility() {
        List<String> workingVariants = new ArrayList<>();
        String preOptimVariant = raoData.getWorkingVariantId();

        RaoData spiedRaoData = Mockito.spy(raoData);
        RaoDataManager spiedRaoDataManager = Mockito.spy(raoData.getRaoDataManager());
        Mockito.when(spiedRaoData.getRaoDataManager()).thenReturn(spiedRaoDataManager);
        Mockito.doNothing().when(spiedRaoDataManager).fillCracResultsWithSensis(any(), anyDouble());

        // mock sensitivity engine
        // sensitivity computation returns a cost of 100 before optim, and 50 after optim
        doAnswer(new Answer() {

            private int count = 1;

            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                RaoData raoData = (RaoData) args[0];
                double cost;
                switch (count) {
                    case 1:
                        cost = 100;
                        break;
                    case 2:
                        cost = 50;
                        break;
                    case 3:
                        cost = 20;
                        break;
                    default:
                        cost = 0;
                        break;
                }
                raoData.getCracResult().setFunctionalCost(cost);
                crac.getExtension(CracResultExtension.class).getVariant(raoData.getWorkingVariantId()).setFunctionalCost(cost);
                count += 1;
                return null;
            }
        }).when(systematicSensitivityComputation).run(any());

        // mock linear optimisation engine
        // linear optimisation returns always the same value -> optimal solution is 1.0 for all RAs
        doAnswer(new Answer() {
            private int count = 1;
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                RaoData raoData = (RaoData) args[0];
                double setPoint;
                switch (count) {
                    case 1:
                        setPoint = 1.0;
                        break;
                    case 2:
                    case 3:
                        setPoint = 3.0;
                        break;
                    default:
                        setPoint = 0;
                        break;
                }
                workingVariants.add(raoData.getWorkingVariantId());
                crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class)
                    .getVariant(raoData.getWorkingVariantId())
                    .setSetPoint(crac.getPreventiveState().getId(), setPoint);
                count += 1;

                return raoData;
            }
        }).when(linearOptimizer).optimize(any());

        Mockito.when(linearOptimizer.getSolverResultStatusString()).thenReturn("INFEASIBLE");

        systematicSensitivityComputation.run(spiedRaoData);
        // run an iterating optimization
        String bestVariantId = new IteratingLinearOptimizer(
            systematicSensitivityComputation,
            linearOptimizer,
            parameters).optimize(spiedRaoData);

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
    }
}
