/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.fastrao;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.FastRaoResultImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class FastRaoTest {

    @Test
    void testRunFilteredRaoOnPreventiveOnlyCase() throws IOException {
        // US 4.3.1 as a UT to test OneStateOnly
        Network network = Network.read("/network/TestCase12Nodes.uct", getClass().getResourceAsStream("/network/TestCase12Nodes.uct"));
        Crac crac = Crac.read("/crac/SL_ep4us3.json", getClass().getResourceAsStream("/crac/SL_ep4us3.json"), network);
        RaoInput individualRaoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_posMargin_ampere.json"));
        FastRaoParameters fastRaoParameters = new FastRaoParameters();
        fastRaoParameters.setNumberOfCnecsToAdd(1);
        fastRaoParameters.setAddUnsecureCnecs(true);
        raoParameters.addExtension(FastRaoParameters.class, fastRaoParameters);
        FastRaoResultImpl raoResult = (FastRaoResultImpl) FastRao.launchFastRaoOptimization(individualRaoInput, raoParameters, null, new HashSet<>());
        assertEquals(-143.83, raoResult.getFunctionalCost(crac.getLastInstant()), 1e-1);
        assertEquals(6, raoResult.getExtension(CriticalCnecsResult.class).getCriticalCnecIds().size());
    }

    @Test
    void testRunFilteredRaoOnComplexCase() throws IOException {
        // US 13.4.3 as a UT but with objective function SECURE_FLOW, case with prev and cur RA
        Network network = Network.read("/network/TestCase16Nodes.uct", getClass().getResourceAsStream("/network/TestCase16Nodes.uct"));
        Crac crac = Crac.read("/crac/SL_ep13us4case3.json", getClass().getResourceAsStream("/crac/SL_ep13us4case3.json"), network);
        RaoInput individualRaoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_secure_ampere.json"));
        FastRaoParameters fastRaoParameters = new FastRaoParameters();
        fastRaoParameters.setNumberOfCnecsToAdd(1);
        fastRaoParameters.setAddUnsecureCnecs(true);
        raoParameters.addExtension(FastRaoParameters.class, fastRaoParameters);
        FastRaoResultImpl raoResult = (FastRaoResultImpl) FastRao.launchFastRaoOptimization(individualRaoInput, raoParameters, null, new HashSet<>());
        assertEquals(314.7, raoResult.getFunctionalCost(crac.getLastInstant()), 1e-1);
        assertEquals(2, raoResult.getExtension(CriticalCnecsResult.class).getCriticalCnecIds().size());
    }

    @Test
    void testRunFilteredRao2() throws IOException {
        // Test with 2 preventive network actions activated
        Network network = Network.read("/network/3Nodes1LineOpen.uct", getClass().getResourceAsStream("/network/3Nodes1LineOpen.uct"));
        Crac crac = Crac.read("/crac/fast-rao-UT-2prev-network-action.json", getClass().getResourceAsStream("/crac/fast-rao-UT-2prev-network-action.json"), network);
        RaoInput individualRaoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_secure.json"));
        FastRaoParameters fastRaoParameters = new FastRaoParameters();
        raoParameters.addExtension(FastRaoParameters.class, fastRaoParameters);
        FastRaoResultImpl raoResult = (FastRaoResultImpl) FastRao.launchFastRaoOptimization(individualRaoInput, raoParameters, null, new HashSet<>());
        assertEquals(-101.15, raoResult.getFunctionalCost(crac.getLastInstant()), 1e-1);
        assertEquals(List.of(List.of("Close FR2 FR3", "Close FR1 FR2")), raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters().getPredefinedCombinations());
    }

    //TODO : add costly objec function exemple

    @Test
    void testInitialSensiFailed() throws IOException {
        // US 2.3.4
        Network network = Network.read("/network/US2-3-case4-networkDiverge.uct", getClass().getResourceAsStream("/network/US2-3-case4-networkDiverge.uct"));
        Crac crac = Crac.read("/crac/SL_ep2us3case4.json", getClass().getResourceAsStream("/crac/SL_ep2us3case4.json"), network);
        RaoInput individualRaoInput = RaoInput.build(network, crac).build();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_posMargin_ampere.json"));
        FastRaoParameters fastRaoParameters = new FastRaoParameters();
        fastRaoParameters.setNumberOfCnecsToAdd(1);
        raoParameters.addExtension(FastRaoParameters.class, fastRaoParameters);
        RaoResult raoResult = FastRao.launchFastRaoOptimization(individualRaoInput, raoParameters, null, new HashSet<>());
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
        assertEquals("Initial sensitivity analysis failed", raoResult.getExecutionDetails());
    }

    @Test
    void testError() {
        RaoInput individualRaoInput = Mockito.mock(RaoInput.class);
        RaoParameters raoParameters = Mockito.mock(RaoParameters.class);
        Mockito.when(raoParameters.hasExtension(FastRaoParameters.class)).thenReturn(true);

        State state = Mockito.mock(State.class);
        Mockito.when(individualRaoInput.getOptimizedState()).thenReturn(state);
        RaoResult raoResult = FastRao.launchFastRaoOptimization(individualRaoInput, raoParameters, null, new HashSet<>());
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
        assertEquals("Fast Rao does not support optimization on one given state only", raoResult.getExecutionDetails());

        Mockito.when(individualRaoInput.getOptimizedState()).thenReturn(null);
        Crac crac = Mockito.mock(Crac.class);
        Mockito.when(individualRaoInput.getCrac()).thenReturn(crac);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.getKind()).thenReturn(InstantKind.CURATIVE);
        Instant instant2 = Mockito.mock(Instant.class);
        Mockito.when(instant2.getKind()).thenReturn(InstantKind.CURATIVE);
        SortedSet<Instant> curativeInstants = new TreeSet<>();
        curativeInstants.add(instant);
        curativeInstants.add(instant2);
        Mockito.when(crac.getInstants(InstantKind.CURATIVE)).thenReturn(curativeInstants);
        raoResult = FastRao.launchFastRaoOptimization(individualRaoInput, raoParameters, null, new HashSet<>());
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
        assertEquals("Fast Rao does not support multi-curative optimization", raoResult.getExecutionDetails());
    }

    @Test
    void testErrorInitData() throws ExecutionException, InterruptedException {
        RaoInput raoInput = Mockito.mock(RaoInput.class);
        RaoParameters raoParameters = Mockito.mock(RaoParameters.class);
        when(raoParameters.getObjectiveFunctionParameters()).thenThrow(new OpenRaoException("This exception should be caught"));
        // Run RAO
        RaoResult raoResult = new FastRao().run(raoInput, raoParameters).get();
        assertInstanceOf(FailedRaoResultImpl.class, raoResult);
    }
}
