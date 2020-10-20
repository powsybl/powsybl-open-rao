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
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.rao_api.RaoParameters.ObjectiveFunction.*;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoUtilTest {
    private static final double DOUBLE_TOLERANCE = 0.1;
    private RaoParameters raoParameters;
    private RaoData raoData;
    RaoInput raoInput;
    Network network;
    Crac crac;
    String variantId;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        variantId = network.getVariantManager().getWorkingVariantId();
        raoInput = RaoInput.builder()
            .withNetwork(network)
            .withCrac(crac)
            .withVariantId(variantId)
            .withOptimizedState(crac.getPreventiveState())
            .withPerimeter(crac.getStates())
            .build();
        raoParameters = new RaoParameters();

        raoData = RaoUtil.initRaoData(raoInput, raoParameters);
    }

    private void addPtdfParameters(List<String> boundaries) {
        RaoPtdfParameters raoPtdfParameters = new RaoPtdfParameters();
        if (boundaries != null) {
            raoPtdfParameters.setBoundariesFromCountryCodes(boundaries);
        }
        raoParameters.addExtension(RaoPtdfParameters.class, raoPtdfParameters);
    }

    private void addGlskProvider() {
        UcteGlskProvider glskProvider = new UcteGlskProvider(getClass().getResourceAsStream("/GlskCountry.xml"), network);
        raoInput = RaoInput.builder()
            .withNetwork(network)
            .withCrac(crac)
            .withVariantId(variantId)
            .withOptimizedState(crac.getPreventiveState())
            .withPerimeter(crac.getStates())
            .withGlskProvider(glskProvider)
            .build();
    }

    @Test
    public void createLinearOptimizerFromRaoParameters() {
        raoParameters.setObjectiveFunction(MAX_MIN_MARGIN_IN_AMPERE);
        SystematicSensitivityInterface systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        IteratingLinearOptimizer optimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityInterface);

        assertTrue(optimizer.getObjectiveFunctionEvaluator() instanceof MinMarginObjectiveFunction);
        assertFalse(((MinMarginObjectiveFunction) optimizer.getObjectiveFunctionEvaluator()).isRelative());
        assertEquals(AMPERE, optimizer.getObjectiveFunctionEvaluator().getUnit());
        assertEquals(0, optimizer.getParameters().getFallbackOverCost(), DOUBLE_TOLERANCE);
        assertEquals(10, optimizer.getParameters().getMaxIterations());
    }

    @Test
    public void createRelativeOptimizerMegawatt() {
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        SystematicSensitivityInterface systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        IteratingLinearOptimizer optimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityInterface);

        assertTrue(optimizer.getObjectiveFunctionEvaluator() instanceof MinMarginObjectiveFunction);
        assertTrue(((MinMarginObjectiveFunction) optimizer.getObjectiveFunctionEvaluator()).isRelative());
        assertEquals(MEGAWATT, optimizer.getObjectiveFunctionEvaluator().getUnit());
    }

    @Test
    public void createRelativeOptimizerAmpere() {
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        SystematicSensitivityInterface systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        IteratingLinearOptimizer optimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityInterface);

        assertTrue(optimizer.getObjectiveFunctionEvaluator() instanceof MinMarginObjectiveFunction);
        assertTrue(((MinMarginObjectiveFunction) optimizer.getObjectiveFunctionEvaluator()).isRelative());
        assertEquals(AMPERE, optimizer.getObjectiveFunctionEvaluator().getUnit());
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
        raoParameters.setObjectiveFunction(MAX_MIN_MARGIN_IN_AMPERE);
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoParameters);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(AMPERE, costEvaluator.getUnit());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersRelativeMW() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoParameters);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(MEGAWATT, costEvaluator.getUnit());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersRelativeAmps() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
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

    @Test (expected = FaraoException.class)
    public void testExceptionForGlskOnRelativeMargin() {
        addPtdfParameters(new ArrayList<>(Arrays.asList("FR-ES", "ES-PT")));
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.initRaoData(raoInput, raoParameters);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForNoPtdfParametersOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.initRaoData(raoInput, raoParameters);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForNullBoundariesOnRelativeMargin() {
        addGlskProvider();
        addPtdfParameters(null);
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.initRaoData(raoInput, raoParameters);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForEmptyBoundariesOnRelativeMargin() {
        addGlskProvider();
        addPtdfParameters(new ArrayList<>());
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        RaoUtil.initRaoData(raoInput, raoParameters);
    }

    @Test
    public void testCreateSystematicSensitivityInterfaceOnRelativeMargin() {
        addPtdfParameters(new ArrayList<>(Arrays.asList("FR-BE", "BE-NL", "FR-DE", "DE-NL")));
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoData = RaoUtil.initRaoData(raoInput, raoParameters);
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData);
        assertNotNull(systematicSensitivityInterface);
    }
}

