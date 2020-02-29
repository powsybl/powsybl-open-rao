/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.balance_computation;

import com.farao_community.farao.balances_adjustment.util.CountryArea;
import com.farao_community.farao.balances_adjustment.util.CountryAreaTest;
import com.farao_community.farao.balances_adjustment.util.NetworkArea;
import com.powsybl.action.util.Scalable;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class BalanceComputationImplDcTest {
    private Network testNetwork1;
    private Map<NetworkArea, Double> networkAreaNetPositionTargetMap;
    private Map<NetworkArea, Scalable> networkAreasScalableMap;
    private ComputationManager computationManager;
    private CountryArea countryAreaFR;
    private CountryArea countryAreaBE;

    private BalanceComputationParameters parameters;
    private BalanceComputationFactory balanceComputationFactory;
    private LoadFlow.Runner loadFlowRunner;
    private String newStateId = "NewStateId";

    @Before
    public void setUp() {
        testNetwork1 = Importers.loadNetwork("testCase.xiidm", CountryAreaTest.class.getResourceAsStream("/testCase.xiidm"));

        countryAreaFR = new CountryArea(Country.FR);
        countryAreaBE = new CountryArea(Country.BE);

        computationManager = LocalComputationManager.getDefault();

        parameters = new BalanceComputationParameters();
        OpenLoadFlowParameters openLoadFlowParameters = new OpenLoadFlowParameters();
        openLoadFlowParameters.setDc(true);
        parameters.getLoadFlowParameters().addExtension(OpenLoadFlowParameters.class, openLoadFlowParameters);

        balanceComputationFactory = new BalanceComputationFactoryImpl();

        loadFlowRunner = new LoadFlow.Runner(new OpenLoadFlowProvider(new DenseMatrixFactory()));

        networkAreasScalableMap = new HashMap<>();
        Scalable scalableFR = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("FFR1AA1 _generator"), Scalable.onGenerator("FFR2AA1 _generator"), Scalable.onGenerator("FFR3AA1 _generator")));
        networkAreasScalableMap.put(countryAreaFR, scalableFR);

        Scalable scalableBE = Scalable.proportional(Arrays.asList(60f, 30f, 10f),
                Arrays.asList(Scalable.onGenerator("BBE1AA1 _generator"), Scalable.onGenerator("BBE3AA1 _generator"), Scalable.onGenerator("BBE2AA1 _generator")));
        networkAreasScalableMap.put(countryAreaBE, scalableBE);

    }

    @Test
    public void testBalancedNetwork() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1000.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 1500.);

        BalanceComputation balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
    }

    @Test
    public void testBalancedNetworkAfter1Scaling() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 1300.);

        BalanceComputation balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());

    }

    @Test
    public void testBalancesAdjustmentWithDifferentStateId() {
        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1100.);

        testNetwork1.getVariantManager().cloneVariant(testNetwork1.getVariantManager().getWorkingVariantId(), newStateId);

        Map<NetworkArea, Scalable> networkAreasScalableMap1 = new HashMap<>();
        Scalable scalable1 = Scalable.onGenerator("FFR1AA1 _generator");
        Scalable scalable2 = Scalable.onGenerator("FFR2AA1 _generator");
        Scalable scalable3 = Scalable.onGenerator("FFR3AA1 _generator");

        networkAreasScalableMap1.put(countryAreaFR, Scalable.proportional(28.f, scalable1, 28f, scalable2, 44.f, scalable3));

        BalanceComputation balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap1, loadFlowRunner, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(newStateId, parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
        // Check net position does not change with the initial state id after balances
        assertEquals(1000., countryAreaFR.getNetPosition(testNetwork1), 1.);
        // Check target net position after balances with the new state id
        testNetwork1.getVariantManager().setWorkingVariant(newStateId);
        assertEquals(1100., countryAreaFR.getNetPosition(testNetwork1), 1.);

    }

    @Test
    public void testUnBalancedNetwork() {

        networkAreaNetPositionTargetMap = new HashMap<>();
        networkAreaNetPositionTargetMap.put(countryAreaFR, 1200.);
        networkAreaNetPositionTargetMap.put(countryAreaBE, 1500.);

        BalanceComputation balanceComputation = balanceComputationFactory.create(testNetwork1, networkAreaNetPositionTargetMap, networkAreasScalableMap, loadFlowRunner, computationManager, 1);

        BalanceComputationResult result = balanceComputation.run(testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals(1000, countryAreaFR.getNetPosition(testNetwork1), 1e-3);
        assertEquals(1500, countryAreaBE.getNetPosition(testNetwork1), 1e-3);

    }
}
