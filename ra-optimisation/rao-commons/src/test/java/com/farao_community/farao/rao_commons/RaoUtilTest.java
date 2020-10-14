/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.glsk.import_.glsk_provider.UcteGlskProvider;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import com.farao_community.farao.rao_commons.objective_function_evaluator.CostEvaluator;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginObjectiveFunction;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoUtilTest {
    private static final double DOUBLE_TOLERANCE = 0.1;
    private RaoParameters raoParameters;
    private RaoData raoData;

    @Before
    public void setUp() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();
        String variantId = network.getVariantManager().getWorkingVariantId();
        RaoInput raoInput = RaoInput.builder()
            .withNetwork(network)
            .withCrac(crac)
            .withVariantId(variantId)
            .build();
        raoParameters = new RaoParameters();

        raoData = RaoUtil.initRaoData(raoInput, raoParameters);
    }

    @Test
    public void createLinearOptimizerFromRaoParameters() {
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        SystematicSensitivityInterface systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        IteratingLinearOptimizer optimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityInterface);

        assertTrue(optimizer.getObjectiveFunctionEvaluator() instanceof MinMarginObjectiveFunction);
        assertEquals(AMPERE, optimizer.getObjectiveFunctionEvaluator().getUnit());
        assertEquals(0, optimizer.getParameters().getFallbackOverCost(), DOUBLE_TOLERANCE);
        assertEquals(10, optimizer.getParameters().getMaxIterations());
    }

    @Test
    public void createLinearOptimizerFromRaoParametersWithLoopFlows() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        SystematicSensitivityInterface systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        IteratingLinearOptimizer optimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityInterface);

        assertTrue(optimizer instanceof IteratingLinearOptimizerWithLoopFlows);
        assertTrue(optimizer.getObjectiveFunctionEvaluator() instanceof MinMarginObjectiveFunction);
        assertEquals(MEGAWATT, optimizer.getObjectiveFunctionEvaluator().getUnit());
        assertEquals(0, optimizer.getParameters().getFallbackOverCost(), DOUBLE_TOLERANCE);
        assertEquals(10, optimizer.getParameters().getMaxIterations());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersMegawatt() {
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoParameters);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(MEGAWATT, costEvaluator.getUnit());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersAmps() {
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoParameters);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(AMPERE, costEvaluator.getUnit());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersRelativeMW() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoParameters);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(MEGAWATT, costEvaluator.getUnit());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersRelativeAmps() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoParameters);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(AMPERE, costEvaluator.getUnit());
    }

    @Test
    public void testThatRaoDataCreationSynchronizesCrac() {
        assertTrue(raoData.getCrac().isSynchronized());
    }

    @Test
    public void testCreationOfSystematicSensitivityInterface() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData);
        assertNotNull(systematicSensitivityInterface);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForBoundariesOnRelativeMargin() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        UcteGlskProvider ucteGlskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/GlskCountry.xml"), network);
        Crac crac = CommonCracCreation.create();
        String variantId = network.getVariantManager().getWorkingVariantId();
        RaoInput raoInput = RaoInput.builder()
                .withNetwork(network)
                .withCrac(crac)
                .withGlskProvider(ucteGlskProvider)
                .withVariantId(variantId)
                .build();
        RaoParameters parameters = new RaoParameters();
        parameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.initRaoData(raoInput, parameters);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForEmptyBoundariesOnRelativeMargin() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        UcteGlskProvider ucteGlskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/GlskCountry.xml"), network);
        List<Pair<Country, Country>> boundaries = new ArrayList<>();
        Crac crac = CommonCracCreation.create();
        String variantId = network.getVariantManager().getWorkingVariantId();
        RaoInput raoInput = RaoInput.builder()
                .withNetwork(network)
                .withCrac(crac)
                .withGlskProvider(ucteGlskProvider)
                .withBoundaries(boundaries)
                .withVariantId(variantId)
                .build();
        RaoParameters parameters = new RaoParameters();
        parameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.initRaoData(raoInput, parameters);
    }

    @Test (expected = FaraoException.class)
    public void testExceptionForGlskOnRelativeMargin() {
        List<Pair<Country, Country>> boundaries = new ArrayList<>(Collections.singleton(new ImmutablePair<>(Country.FR, Country.BE)));
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();
        String variantId = network.getVariantManager().getWorkingVariantId();
        RaoInput raoInput = RaoInput.builder()
                .withNetwork(network)
                .withCrac(crac)
                .withBoundaries(boundaries)
                .withVariantId(variantId)
                .build();
        RaoParameters parameters = new RaoParameters();
        parameters.setObjectiveFunction(RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.initRaoData(raoInput, parameters);
    }
}

