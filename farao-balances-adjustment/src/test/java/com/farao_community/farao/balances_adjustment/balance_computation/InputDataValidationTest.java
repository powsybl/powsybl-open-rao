/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.balance_computation;

import com.farao_community.farao.balances_adjustment.util.*;
import com.powsybl.action.util.Scalable;
import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class InputDataValidationTest {
    private BalanceComputation balanceComputation;
    private Network testNetwork1;
    private Network testNetwork2;
    private Network networkNull;
    private Map<NetworkArea, Double> networkAreaNetPositionTargetMap;
    private Map<NetworkArea, Scalable> networkAreasScalableMap;
    private ComputationManager computationManager;

    private CountryArea countryAreaFR;
    private CountryArea countryAreaBE;
    private CountryArea countryAreaNotFound;
    private BalanceComputationParameters parameters;
    private BalanceComputationFactory balanceComputationFactory;
    private String workingVariantId;
    private LoadFlow.Runner loadFlowRunner;
    private String error = "The input data for balance computation is not valid";

    @Before
    public void setUp() {
        testNetwork1 = Importers.loadNetwork("testCase.xiidm", CountryAreaTest.class.getResourceAsStream("/testCase.xiidm"));
        testNetwork2 = NetworkTestFactory.createNetwork();

        countryAreaFR = new CountryArea(Country.FR);
        countryAreaBE = new CountryArea(Country.BE);
        countryAreaNotFound = new CountryArea(Country.ES);

        computationManager = LocalComputationManager.getDefault();
        parameters = new BalanceComputationParameters();
        balanceComputationFactory = new BalanceComputationFactoryImpl();
        workingVariantId = testNetwork1.getVariantManager().getWorkingVariantId();
        loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);

    }

    @Test(expected = NullPointerException.class)
    public void testNullNetwork() {
        balanceComputation = balanceComputationFactory.create(networkNull, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNetPositionTargetMap() {
        networkAreasScalableMap = new HashMap<>();
        balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testNullNetworkAreasScalableMap() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
    }

    @Test
    public void testNotFoundNetworkArea() {
        networkAreasScalableMap = new HashMap<>();
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 0.);
        networkAreaNetPositionTargetMap.put(countryAreaNotFound, 0.);

        balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
        List<String> listOfViolations = ((BalanceComputationImpl) balanceComputation).listInputDataViolations();
        assertTrue(listOfViolations.contains("The " + countryAreaNotFound + " is not found in the network " + testNetwork1));

        try {
            balanceComputation.run(workingVariantId, parameters).join();
            fail("My method didn't throw when I expected it to");
        } catch (PowsyblException e) {
            assertEquals(error, e.getMessage());
        }
    }

    @Test
    public void testNotFoundVoltageLevel() {
        networkAreasScalableMap = new HashMap<>();
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaNotFound, 0.);

        NetworkArea networkAreaTest = new VoltageLevelsArea("Area", Arrays.asList(testNetwork2.getVoltageLevel("vlFr1A"), testNetwork1.getVoltageLevel("FFR3AA1")));
        networkAreaNetPositionTargetMap.put(networkAreaTest, 0.);

        balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
        List<String> listOfViolations = ((BalanceComputationImpl) balanceComputation).listInputDataViolations();

        assertTrue(listOfViolations.contains("The " + countryAreaNotFound + " is not found in the network " + testNetwork1));
        assertTrue(listOfViolations.contains("The " + testNetwork1 + " doesn't contain all voltage levels of " + networkAreaTest));

        try {
            balanceComputation.run(workingVariantId, parameters).join();
            fail("My method didn't throw when I expected it to");
        } catch (PowsyblException e) {
            assertEquals(error, e.getMessage());
        }
    }

    @Test
    public void testNotFoundAreaScalables() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 0.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 0.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalable = Mockito.mock(Scalable.class);
        networkAreasScalableMap.put(countryAreaFR, scalable);

        balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
        List<String> listOfViolations = ((BalanceComputationImpl) balanceComputation).listInputDataViolations();
        assertTrue(listOfViolations.contains("The " + countryAreaBE.getName() + " is not defined in the scalable network areas map"));
        assertTrue(listOfViolations.contains("The scalable of " + countryAreaFR + " doesn't contain injections in network"));

        try {
            balanceComputation.run(workingVariantId, parameters).join();
            fail("My method didn't throw when I expected it to");
        } catch (PowsyblException e) {
            assertEquals(error, e.getMessage());
        }
    }

    @Test
    public void testNullElements() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, null);
        networkAreaNetPositionTargetMap.put(null, 0.);

        networkAreasScalableMap = new HashMap<>();
        networkAreasScalableMap.put(countryAreaFR, null);
        Scalable scalableMock = Mockito.mock(Scalable.class);
        networkAreasScalableMap.put(null, scalableMock);

        balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
        List<String> listOfViolations = ((BalanceComputationImpl) balanceComputation).listInputDataViolations();

        assertTrue(listOfViolations.contains("The net position target map contains null network areas"));
        assertTrue(listOfViolations.contains("The net position target map contains null values"));
        assertTrue(listOfViolations.contains("The scalable network areas map contains null network areas"));
        assertTrue(listOfViolations.contains("The scalable network areas map contains null values"));

        try {
            balanceComputation.run(workingVariantId, parameters).join();
            fail("My method didn't throw when I expected it to");
        } catch (PowsyblException e) {
            assertEquals(error, e.getMessage());
        }
    }

    @Test
    public void testNotFoundInjection() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 0.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 0.);

        networkAreasScalableMap = new HashMap<>();
        Scalable scalable = Mockito.mock(Scalable.class);
        networkAreasScalableMap.put(countryAreaFR, scalable);
        Scalable generatorScalableFR = Scalable.onGenerator("FFR1AA1 _generator");
        Scalable generatorScalableWithBranchId = Scalable.onGenerator("BBE1AA1  BBE2AA1  1");
        Scalable proportionalScalable = Scalable.proportional(Arrays.asList(30.f, 70.f), Arrays.asList(generatorScalableFR, generatorScalableWithBranchId));
        networkAreasScalableMap.put(countryAreaBE, proportionalScalable);

        balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
        List<String> listOfViolations = ((BalanceComputationImpl) balanceComputation).listInputDataViolations();

        assertTrue(listOfViolations.contains("The scalable of " + countryAreaFR + " doesn't contain injections in network"));
        assertTrue(listOfViolations.contains("The scalable of " + countryAreaBE + " contains injections " + Arrays.asList("FFR1AA1 _generator") + " not found in the network area"));
        assertTrue(listOfViolations.contains("The scalable of " + countryAreaBE + " contains injections " + Arrays.asList("BBE1AA1  BBE2AA1  1") + " not found in the network"));

        try {
            balanceComputation.run(workingVariantId, parameters).join();
            fail("My method didn't throw when I expected it to");
        } catch (PowsyblException e) {
            assertEquals(error, e.getMessage());
        }
    }

    @Test
    public void testValidInputData() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 0.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 0.);

        networkAreasScalableMap = new HashMap<>();

        Scalable generatorScalableFR = Scalable.onGenerator("FFR1AA1 _generator");
        networkAreasScalableMap.put(countryAreaFR, generatorScalableFR);

        Scalable loadScalableBE = Scalable.onLoad("BBE1AA1 _load");
        networkAreasScalableMap.put(countryAreaBE, loadScalableBE);

        balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);
        List<String> listOfViolations = ((BalanceComputationImpl) balanceComputation).listInputDataViolations();

        assertTrue(listOfViolations.isEmpty());
    }
}
