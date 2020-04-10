/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.farao_community.farao.linear_rao.optimisation.LinearOptimisationException;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LinearRaoProblem.class)
public class LinearOptimisationEngineTest {
    private LinearOptimisationEngine linearOptimisationEngine;
    private LinearRaoProblem linearRaoProblemMock;

    @Before
    public void setUp() {

        try {
            PowerMockito.whenNew(MPSolver.class).withAnyArguments().thenReturn(new MPSolverMock());
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }

        // RaoParameters
        RaoParameters raoParameters = Mockito.mock(RaoParameters.class);
        linearOptimisationEngine = new LinearOptimisationEngine(raoParameters);

        /*linearOptimisationEngine = Mockito.spy(new LinearOptimisationEngine(raoParameters));

        Mockito.when(linearOptimisationEngine.createLinearRaoProblem()).thenReturn(linearRaoProblemMock);
        Mockito.when(linearOptimisationEngine.getFillerL()).thenReturn(linearRaoProblemMock);
        Mockito.when(linearOptimisationEngine.createLinearRaoProblem()).thenReturn(linearRaoProblemMock);


        linearRaoProblemMock = Mockito.mock(LinearRaoProblem.class);


        Crac crac = new SimpleCrac("id");
        crac.addState(new SimpleState(Optional.empty(), new Instant("preventive", 0)));
        Network networkMock = Mockito.mock(Network.class);
        SystematicSensitivityAnalysisResult sensitivityResultMock = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        RaoParameters raoParametersMock = Mockito.mock(RaoParameters.class);

        //linearOptimisationEngine = new LinearOptimisationEngine(raoParametersMock);

         */
    }

    @Test
    public void test() {

    }


}
