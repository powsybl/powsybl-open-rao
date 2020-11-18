/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import com.farao_community.farao.rao_commons.objective_function_evaluator.CostEvaluator;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginObjectiveFunction;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
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
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
            .withNetworkVariantId(variantId)
            .build();
        raoParameters = new RaoParameters();
    }

    private void addPtdfParameters(List<String> boundaries) {
        if (boundaries != null) {
            raoParameters.setPtdfBoundariesFromCountryCodes(boundaries);
        }
    }

    private void addGlskProvider() {
        ZonalData<LinearGlsk> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
            .getZonalGlsks(network);
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
                .withNetworkVariantId(variantId)
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
        addPtdfParameters(null);
        SystematicSensitivityInterface systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        IteratingLinearOptimizer optimizer = RaoUtil.createLinearOptimizer(raoParameters, systematicSensitivityInterface);

        assertTrue(optimizer.getObjectiveFunctionEvaluator() instanceof MinMarginObjectiveFunction);
        assertTrue(((MinMarginObjectiveFunction) optimizer.getObjectiveFunctionEvaluator()).isRelative());
        assertEquals(MEGAWATT, optimizer.getObjectiveFunctionEvaluator().getUnit());
    }

    @Test
    public void createRelativeOptimizerAmpere() {
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        addPtdfParameters(null);
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
        RaoUtil.initCrac(crac, network);
        assertTrue(crac.isSynchronized());
    }

    @Test
    public void testCreationOfSystematicSensitivityInterface() {
        raoParameters.setRaoWithLoopFlowLimitation(true);
        raoData =  new RaoData(
                raoInput.getNetwork(),
                raoInput.getCrac(),
                raoInput.getOptimizedState(),
                raoInput.getPerimeter(),
                raoInput.getReferenceProgram(),
                raoInput.getGlskProvider(),
                raoInput.getBaseCracVariantId(),
                raoParameters.getLoopflowCountries());
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData, false);
        assertNotNull(systematicSensitivityInterface);
    }

    @Test (expected = FaraoException.class)
    public void testExceptionForGlskOnRelativeMargin() {
        addPtdfParameters(new ArrayList<>(Arrays.asList("FR-ES", "ES-PT")));
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForNoPtdfParametersOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForNullBoundariesOnRelativeMargin() {
        addGlskProvider();
        addPtdfParameters(null);
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForEmptyBoundariesOnRelativeMargin() {
        addGlskProvider();
        addPtdfParameters(new ArrayList<>());
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test
    public void testCreateSystematicSensitivityInterfaceOnRelativeMargin() {
        addPtdfParameters(new ArrayList<>(Arrays.asList("FR-BE", "BE-NL", "FR-DE", "DE-NL")));
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoData =  new RaoData(
                raoInput.getNetwork(),
                raoInput.getCrac(),
                raoInput.getOptimizedState(),
                raoInput.getPerimeter(),
                raoInput.getReferenceProgram(),
                raoInput.getGlskProvider(),
                raoInput.getBaseCracVariantId(),
                raoParameters.getLoopflowCountries());
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData, true);
        assertNotNull(systematicSensitivityInterface);
    }
}

