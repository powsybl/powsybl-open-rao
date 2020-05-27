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
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import com.farao_community.farao.rao_commons.linear_optimisation.SimpleLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimisationException;
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
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class, IteratingLinearOptimizer.class})
public class LinearRaoTest {

    private LinearRao linearRao;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;
    private SystematicSensitivityComputation systematicSensitivityComputation;
    private SimpleLinearOptimizer simpleLinearOptimizer;
    private Network network;
    private Crac crac;
    private String variantId;
    private RaoData raoData;

    @Before
    public void setUp() {
        mockNativeLibraryLoader();
        mockIteratingLinearOptimizer();

        linearRao = Mockito.spy(LinearRao.class);

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        variantId = network.getVariantManager().getWorkingVariantId();
        raoData = new RaoData(network, crac);
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParametersFull.json"));
        LinearRaoParameters linearRaoParameters = raoParameters.getExtension(LinearRaoParameters.class);
        linearRaoParameters.setSecurityAnalysisWithoutRao(false);
        computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        systematicSensitivityComputation = Mockito.mock(SystematicSensitivityComputation.class);
        simpleLinearOptimizer = Mockito.mock(SimpleLinearOptimizer.class);
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

    private void mockIteratingLinearOptimizer() {
        PowerMockito.mockStatic(IteratingLinearOptimizer.class);
    }

    @Test
    public void runLinearRaoWithSensitivityComputationError() {
        Mockito.doThrow(new SensitivityComputationException("error with sensi")).when(systematicSensitivityComputation).run(any());
        try {
            linearRao.runLinearRao(raoData, systematicSensitivityComputation, raoParameters).join();
            fail();
        } catch (SensitivityComputationException e) {
            // should throw
        }
    }

    @Test
    public void runWithSensitivityComputationException() {
        Mockito.doThrow(new SensitivityComputationException("error with sensi")).when(linearRao).runLinearRao(any(), any(), any());
        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
        assertEquals(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE, results.getExtension(LinearRaoResult.class).getSystematicSensitivityAnalysisStatus());
    }

    @Test
    public void runLinearRaoWithLinearOptimisationError() {
        BDDMockito.given(IteratingLinearOptimizer.optimize(any(), (SystematicSensitivityComputation) any(), any())).willThrow(new LinearOptimisationException("error with optim"));
        try {
            linearRao.runLinearRao(raoData, systematicSensitivityComputation, raoParameters).join();
            fail();
        } catch (LinearOptimisationException e) {
            // should throw
        }
    }

    @Test
    public void runWithLinearOptimisationException() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearRao).runLinearRao(any(), any(), any());
        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
        assertEquals(LinearRaoResult.LpStatus.FAILURE, results.getExtension(LinearRaoResult.class).getLpStatus());
    }

    @Test
    public void runWithFaraoException() {
        Mockito.doThrow(new FaraoException("farao exception")).when(linearRao).runLinearRao(any(), any(), any());
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
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(simpleLinearOptimizer).run(any(), any());
        raoParameters.getExtension(LinearRaoParameters.class).setSecurityAnalysisWithoutRao(true);

        RaoResult results = linearRao.runLinearRao(raoData, systematicSensitivityComputation, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.SUCCESS, results.getStatus());
    }

    @Test
    public void runLinearRaoSecurityAnalysisWithZeroIteration() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(simpleLinearOptimizer).run(any(), any());
        raoParameters.getExtension(IteratingLinearOptimizerParameters.class).setMaxIterations(0);

        RaoResult results = linearRao.runLinearRao(raoData, systematicSensitivityComputation, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.SUCCESS, results.getStatus());
    }
}
