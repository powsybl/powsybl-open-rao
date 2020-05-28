/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.SimpleLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.parameters.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class})
public class IteratingLinearOptimizerTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private RaoParameters raoParameters;
    private SystematicSensitivityComputation systematicSensitivityComputation;
    private SimpleLinearOptimizer simpleLinearOptimizer;
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
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/IteratingLinearOptimizerParameters.json"));

        systematicSensitivityComputation = Mockito.mock(SystematicSensitivityComputation.class);
        simpleLinearOptimizer = Mockito.mock(SimpleLinearOptimizer.class);
    }

    @Test
    public void optimize() {
        List<String> workingVariants = new ArrayList<>();
        String preOptimVariant = raoData.getWorkingVariantId();

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
                raoData.getCracResult().setCost(cost);
                crac.getExtension(CracResultExtension.class).getVariant(raoData.getWorkingVariantId()).setCost(cost);
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
                crac.getStates().forEach(st ->
                    crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class).getVariant(raoData.getWorkingVariantId()).setSetPoint(st.getId(), setPoint)
                );
                count += 1;

                return raoData;
            }
        }).when(simpleLinearOptimizer).optimize(any());

        systematicSensitivityComputation.run(raoData);
        // run an iterating optimization
        String bestVariantId = IteratingLinearOptimizer.optimize(
            raoData,
            systematicSensitivityComputation,
            simpleLinearOptimizer,
            raoParameters.getExtension(IteratingLinearOptimizerParameters.class));

        // check results
        assertNotNull(bestVariantId);
        assertEquals(100, crac.getExtension(CracResultExtension.class).getVariant(preOptimVariant).getCost(), DOUBLE_TOLERANCE);
        assertEquals(20, crac.getExtension(CracResultExtension.class).getVariant(bestVariantId).getCost(), DOUBLE_TOLERANCE);

        // In the end CRAC should contain results only for pre-optim variant and post-optim variant
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains(preOptimVariant));
        assertTrue(crac.getExtension(ResultVariantManager.class).getVariants().contains(workingVariants.get(2)));
        assertFalse(crac.getExtension(ResultVariantManager.class).getVariants().contains(workingVariants.get(0)));
        //assertFalse(crac.getExtension(ResultVariantManager.class).getVariants().contains(workingVariants.get(1)));
        crac.getStates().forEach(st -> {
            assertEquals(Double.NaN, crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class).getVariant(preOptimVariant).getSetPoint(st.getId()), DOUBLE_TOLERANCE);
            assertEquals(3, crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class).getVariant(bestVariantId).getSetPoint(st.getId()), DOUBLE_TOLERANCE);
        });
    }
}
