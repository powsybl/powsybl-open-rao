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
import com.farao_community.farao.rao_commons.*;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
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
@PrepareForTest({NativeLibraryLoader.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class LinearRaoTest {

    private LinearRao linearRao;
    private RaoParameters raoParameters;
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private InitialSensitivityAnalysis initialSensitivityAnalysis;
    private Network network;
    private Crac crac;
    private RaoData raoData;

    @Before
    public void setUp() {
        mockNativeLibraryLoader();

        linearRao = Mockito.spy(LinearRao.class);

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        raoData = Mockito.spy(new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters()));
        CracResultManager spiedCracResultManager = Mockito.spy(raoData.getCracResultManager());
        Mockito.when(raoData.getCracResultManager()).thenReturn(spiedCracResultManager);
        Mockito.doNothing().when(spiedCracResultManager).fillCnecResultWithFlows();
        Mockito.doNothing().when(spiedCracResultManager).fillCracResultWithCosts(anyDouble(), anyDouble());
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParameters.json"));
        LinearRaoParameters linearRaoParameters = raoParameters.getExtension(LinearRaoParameters.class);
        linearRaoParameters.setSecurityAnalysisWithoutRao(false);

        systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);
        initialSensitivityAnalysis = Mockito.mock(InitialSensitivityAnalysis.class);
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
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Test
    public void runLinearRaoWithSensitivityComputationError() {
        Mockito.doThrow(new SensitivityAnalysisException("error with sensi")).when(initialSensitivityAnalysis).run();
        RaoResult raoResult = linearRao.run(raoData, systematicSensitivityInterface, iteratingLinearOptimizer, initialSensitivityAnalysis, raoParameters).join();
        assertEquals(RaoResult.Status.FAILURE, raoResult.getStatus());
        assertEquals(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE,
            raoResult.getExtension(LinearRaoResult.class).getSystematicSensitivityAnalysisStatus());
    }

    @Test(expected = LinearOptimisationException.class)
    public void runLinearRaoWithLinearOptimisationError() {
        Mockito.when(initialSensitivityAnalysis.run()).thenReturn(null);
        Mockito.when(iteratingLinearOptimizer.optimize(any())).thenThrow(new LinearOptimisationException("error with optim"));
        linearRao.run(raoData, systematicSensitivityInterface, iteratingLinearOptimizer, initialSensitivityAnalysis, raoParameters).join();
    }

    @Test
    public void runWithRaoParametersError() {
        raoParameters.removeExtension(LinearRaoParameters.class);

        RaoResult results = linearRao.run(raoData, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
    }
}
