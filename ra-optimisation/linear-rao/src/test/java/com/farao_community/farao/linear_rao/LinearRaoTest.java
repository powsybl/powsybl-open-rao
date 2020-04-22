/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.linear_rao.optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.farao_community.farao.util.SensitivityComputationException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class})
public class LinearRaoTest {

    private static final double DOUBLE_TOLERANCE = 0.1;

    private LinearRao linearRao;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;
    private LinearRaoParameters linearRaoParameters;
    private SystematicAnalysisEngine systematicAnalysisEngine;
    private LinearOptimisationEngine linearOptimisationEngine;
    private Network network;
    private Crac crac;
    private String variantId;
    private LinearRaoData linearRaoData;

    @Before
    public void setUp() {
        mockNativeLibraryLoader();

        linearRao = Mockito.spy(LinearRao.class);

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        variantId = network.getVariantManager().getWorkingVariantId();
        linearRaoData = new LinearRaoData(network, crac);
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParameters.json"));
        linearRaoParameters = raoParameters.getExtension(LinearRaoParameters.class);
        linearRaoParameters.setSecurityAnalysisWithoutRao(false);
        computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        systematicAnalysisEngine = Mockito.mock(SystematicAnalysisEngine.class);
        linearOptimisationEngine = Mockito.mock(LinearOptimisationEngine.class);
    }

    @Test
    public void getName() {
        assertEquals("LinearRao", linearRao.getName());
    }

    @Test
    public void getVersion() {
        assertEquals("1.0.0", linearRao.getVersion());
    }

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        PowerMockito.doNothing().when(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Test
    public void runLinearRaoWithSensitivityComputationError() {
        Mockito.doThrow(new SensitivityComputationException("error with sensi")).when(systematicAnalysisEngine).run(any());
        try {
            linearRao.runLinearRao(linearRaoData, systematicAnalysisEngine, linearOptimisationEngine, linearRaoParameters).join();
            fail();
        } catch (SensitivityComputationException e) {
            // should throw
        }
    }

    @Test
    public void runWithSensitivityComputationException() {
        Mockito.doThrow(new SensitivityComputationException("error with sensi")).when(linearRao).runLinearRao(any(), any(), any(), any());
        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
        assertEquals(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE, results.getExtension(LinearRaoResult.class).getSystematicSensitivityAnalysisStatus());
    }

    @Test
    public void runLinearRaoWithLinearOptimisationError() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearOptimisationEngine).run(any());
        try {
            linearRao.runLinearRao(linearRaoData, systematicAnalysisEngine, linearOptimisationEngine, linearRaoParameters).join();
            fail();
        } catch (LinearOptimisationException e) {
            // should throw
        }
    }

    @Test
    public void runWithLinearOptimisationException() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearRao).runLinearRao(any(), any(), any(), any());
        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
        assertEquals(LinearRaoResult.LpStatus.FAILURE, results.getExtension(LinearRaoResult.class).getLpStatus());
    }

    @Test
    public void runWithFaraoException() {
        Mockito.doThrow(new FaraoException("farao exception")).when(linearRao).runLinearRao(any(), any(), any(), any());
        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
    }

    @Test
    public void runWithRaoParametersError() {
        raoParameters.removeExtension(LinearRaoParameters.class);

        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
    }

    @Test
    public void runLinearRaoSecurityAnalysisWithoutRao() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearOptimisationEngine).run(any());
        raoParameters.getExtension(LinearRaoParameters.class).setSecurityAnalysisWithoutRao(true);

        RaoResult results = linearRao.runLinearRao(linearRaoData, systematicAnalysisEngine, linearOptimisationEngine, linearRaoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.SUCCESS, results.getStatus());
    }

    @Test
    public void runLinearRaoSecurityAnalysisWithZeroIteration() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearOptimisationEngine).run(any());
        raoParameters.getExtension(LinearRaoParameters.class).setMaxIterations(0);

        RaoResult results = linearRao.runLinearRao(linearRaoData, systematicAnalysisEngine, linearOptimisationEngine, linearRaoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.SUCCESS, results.getStatus());
    }

    @Test
    public void runLinearRaoTestConvergeInTwoIterations() {

        // mock sensitivity engine
        // sensitivity computation returns a cost of 100 before optim, and 50 after optim
        doAnswer(new Answer() {

            private int count = 1;

            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                LinearRaoData linearRaoData = (LinearRaoData) args[0];
                linearRaoData.getCracResult().setCost(count == 1 ? 100.0 : 50.0);
                crac.getExtension(CracResultExtension.class).getVariant(linearRaoData.getWorkingVariantId()).setCost(count == 1 ? 100.0 : 50.0);
                count += 1;
                return null;
            }
        }).when(systematicAnalysisEngine).run(any());

        // mock linear optimisation engine
        // linear optimisation returns always the same value -> optimal solution is 1.0 for all RAs
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                crac.getStates().forEach(st ->
                    crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class).getVariant(linearRaoData.getWorkingVariantId()).setSetPoint(st.getId(), 1.0)
                );

                return linearRaoData;
            }
        }).when(linearOptimisationEngine).run(any());

        // run Rao
        RaoResult results = linearRao.runLinearRao(linearRaoData, systematicAnalysisEngine, linearOptimisationEngine, linearRaoParameters).join();

        // check results
        assertNotNull(results);
        assertEquals(RaoResult.Status.SUCCESS, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
        assertEquals(LinearRaoResult.SystematicSensitivityAnalysisStatus.DEFAULT, results.getExtension(LinearRaoResult.class).getSystematicSensitivityAnalysisStatus());
        assertEquals(LinearRaoResult.LpStatus.RUN_OK, results.getExtension(LinearRaoResult.class).getLpStatus());

        String preOptimVariant = results.getPreOptimVariantId();
        String postOptimVariant = results.getPostOptimVariantId();

        assertEquals(100.0, crac.getExtension(CracResultExtension.class).getVariant(preOptimVariant).getCost(), DOUBLE_TOLERANCE);
        assertEquals(50, crac.getExtension(CracResultExtension.class).getVariant(postOptimVariant).getCost(), DOUBLE_TOLERANCE);

        crac.getStates().forEach(st ->
            assertEquals(1.0,  crac.getRangeAction("PRA_PST_BE").getExtension(RangeActionResultExtension.class).getVariant(postOptimVariant).getSetPoint(st.getId()), DOUBLE_TOLERANCE)
        );
    }
}
