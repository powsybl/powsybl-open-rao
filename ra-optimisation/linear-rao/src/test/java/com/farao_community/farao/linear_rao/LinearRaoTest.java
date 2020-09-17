/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.farao_community.farao.sensitivity_computation.SensitivityComputationException;
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

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class, RaoUtil.class})
public class LinearRaoTest {

    private LinearRao linearRao;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private Network network;
    private Crac crac;
    private String variantId;
    private RaoData raoData;
    private RaoInput raoInput;

    @Before
    public void setUp() {
        mockNativeLibraryLoader();

        linearRao = Mockito.spy(LinearRao.class);

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        variantId = network.getVariantManager().getWorkingVariantId();
        raoData = Mockito.spy(new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState())));
        RaoDataManager spiedRaoDataManager = Mockito.spy(raoData.getRaoDataManager());
        Mockito.when(raoData.getRaoDataManager()).thenReturn(spiedRaoDataManager);
        Mockito.doNothing().when(spiedRaoDataManager).fillCracResultsWithSensis(anyDouble(), anyDouble());
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParameters.json"));
        LinearRaoParameters linearRaoParameters = raoParameters.getExtension(LinearRaoParameters.class);
        linearRaoParameters.setSecurityAnalysisWithoutRao(false);
        computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);

        raoInput = RaoInput.builder()
            .withNetwork(network)
            .withCrac(crac)
            .withVariantId(variantId)
            .build();
        mockRaoUtil();
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

    private void mockRaoUtil() {
        PowerMockito.mockStatic(RaoUtil.class);
        ObjectiveFunctionEvaluator costEvaluator = Mockito.mock(ObjectiveFunctionEvaluator.class);
        Mockito.when(costEvaluator.getCost(raoData)).thenReturn(0.);
        BDDMockito.when(RaoUtil.createObjectiveFunction(raoParameters)).thenReturn(costEvaluator);
        BDDMockito.when(RaoUtil.initRaoData(raoInput, raoParameters)).thenCallRealMethod();
    }

    @Test
    public void runLinearRaoWithSensitivityComputationError() {
        Mockito.doThrow(new SensitivityComputationException("error with sensi")).when(systematicSensitivityInterface).run(any(), any(), any());
        RaoResult raoResult = linearRao.run(raoData, systematicSensitivityInterface, iteratingLinearOptimizer, raoParameters).join();
        assertEquals(RaoResult.Status.FAILURE, raoResult.getStatus());
        assertEquals(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE,
            raoResult.getExtension(LinearRaoResult.class).getSystematicSensitivityAnalysisStatus());
    }

    @Test(expected = LinearOptimisationException.class)
    public void runLinearRaoWithLinearOptimisationError() {
        Mockito.when(iteratingLinearOptimizer.optimize(any())).thenThrow(new LinearOptimisationException("error with optim"));
        linearRao.run(raoData, systematicSensitivityInterface, iteratingLinearOptimizer, raoParameters).join();
    }

    @Test
    public void runWithRaoParametersError() {
        raoParameters.removeExtension(LinearRaoParameters.class);

        RaoResult results = linearRao.run(raoInput, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
    }
}
