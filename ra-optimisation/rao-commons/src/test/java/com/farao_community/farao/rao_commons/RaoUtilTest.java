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
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerWithLoopFlows;
import com.farao_community.farao.rao_commons.objective_function_evaluator.CostEvaluator;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginObjectiveFunction;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.apache.commons.compress.utils.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

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
                raoParameters);
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData, false);
        assertNotNull(systematicSensitivityInterface);
    }

    @Test (expected = FaraoException.class)
    public void testExceptionForGlskOnRelativeMargin() {
        raoParameters.setPtdfBoundariesFromCountryCodes(new ArrayList<>(Arrays.asList("FR-ES", "ES-PT")));
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
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForEmptyBoundariesOnRelativeMargin() {
        addGlskProvider();
        raoParameters.setPtdfBoundariesFromCountryCodes(new ArrayList<>());
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test
    public void testCreateSystematicSensitivityInterfaceOnRelativeMargin() {
        raoParameters.setPtdfBoundariesFromCountryCodes(new ArrayList<>(Arrays.asList("FR-BE", "BE-NL", "FR-DE", "DE-NL")));
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
                raoParameters);
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData, true);
        assertNotNull(systematicSensitivityInterface);
    }

    @Test(expected = FaraoException.class)
    public void testAmpereWithDc() {
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().setDc(true);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test
    public void testGetCnecLocation() {
        List<Optional<Country>> countries = RaoUtil.getCnecLocation(crac.getBranchCnec("cnec1basecase"), network);
        assertEquals(2, countries.size());
        assertTrue(countries.contains(Optional.of(Country.FR)));
        assertTrue(countries.contains(Optional.of(Country.BE)));

        countries = RaoUtil.getCnecLocation(crac.getBranchCnec("cnec2basecase"), network);
        assertEquals(2, countries.size());
        assertTrue(countries.contains(Optional.of(Country.FR)));
        assertTrue(countries.contains(Optional.of(Country.DE)));
    }

    @Test
    public void testGetNetworkActionsLocation() {
        Network networkWithSwitch = NetworkImportsUtil.import12NodesNetworkWithSwitch();
        Crac cracWithSwitch = CommonCracCreation.createWithSwitch();
        List<Optional<Country>> countries = RaoUtil.getNetworkActionLocation(cracWithSwitch.getNetworkAction("switch_ra"), networkWithSwitch);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.NL)));
    }

    @Test
    public void testComputeCnecMargin() {
        CnecResult result = Mockito.mock(CnecResult.class);
        Mockito.when(result.getAbsolutePtdfSum()).thenReturn(0.5);
        CnecResultExtension resultExtension = Mockito.mock(CnecResultExtension.class);
        Mockito.when(resultExtension.getVariant(Mockito.anyString())).thenReturn(result);
        BranchCnec cnec = Mockito.mock(BranchCnec.class);
        Mockito.when(cnec.getExtension(Mockito.eq(CnecResultExtension.class))).thenReturn(resultExtension);

        Mockito.when(cnec.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(MEGAWATT))).thenReturn(1000.0);
        Mockito.when(cnec.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(AMPERE))).thenReturn(100.0);
        assertEquals(1000, RaoUtil.computeCnecMargin(cnec, variantId, MEGAWATT, false), DOUBLE_TOLERANCE);
        assertEquals(100, RaoUtil.computeCnecMargin(cnec, variantId, AMPERE, false), DOUBLE_TOLERANCE);
        assertEquals(2000, RaoUtil.computeCnecMargin(cnec, variantId, MEGAWATT, true), DOUBLE_TOLERANCE);
        assertEquals(200, RaoUtil.computeCnecMargin(cnec, variantId, AMPERE, true), DOUBLE_TOLERANCE);

        Mockito.when(cnec.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(MEGAWATT))).thenReturn(-1000.0);
        Mockito.when(cnec.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(AMPERE))).thenReturn(-100.0);
        assertEquals(-1000, RaoUtil.computeCnecMargin(cnec, variantId, MEGAWATT, false), DOUBLE_TOLERANCE);
        assertEquals(-100, RaoUtil.computeCnecMargin(cnec, variantId, AMPERE, false), DOUBLE_TOLERANCE);
        assertEquals(-1000, RaoUtil.computeCnecMargin(cnec, variantId, MEGAWATT, true), DOUBLE_TOLERANCE);
        assertEquals(-100, RaoUtil.computeCnecMargin(cnec, variantId, AMPERE, true), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMostLimitingElement() {
        // CNEC 1 : margin of 1000 MW / 100 A, sum of PTDFs = 1
        CnecResult result1 = Mockito.mock(CnecResult.class);
        Mockito.when(result1.getAbsolutePtdfSum()).thenReturn(1.0);
        CnecResultExtension resultExtension1 = Mockito.mock(CnecResultExtension.class);
        Mockito.when(resultExtension1.getVariant(Mockito.anyString())).thenReturn(result1);

        BranchCnec cnec1 = Mockito.mock(BranchCnec.class);
        Mockito.when(cnec1.isOptimized()).thenReturn(true);
        Mockito.when(cnec1.getExtension(Mockito.eq(CnecResultExtension.class))).thenReturn(resultExtension1);
        Mockito.when(cnec1.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(MEGAWATT))).thenReturn(1000.);
        Mockito.when(cnec1.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(AMPERE))).thenReturn(100.);

        // CNEC 2 : margin of 600 MW / 60 A, sum of PTDFs = 0.5
        CnecResult result2 = Mockito.mock(CnecResult.class);
        Mockito.when(result2.getAbsolutePtdfSum()).thenReturn(0.5);
        CnecResultExtension resultExtension2 = Mockito.mock(CnecResultExtension.class);
        Mockito.when(resultExtension2.getVariant(Mockito.anyString())).thenReturn(result2);

        BranchCnec cnec2 = Mockito.mock(BranchCnec.class);
        Mockito.when(cnec2.isOptimized()).thenReturn(true);
        Mockito.when(cnec2.getExtension(Mockito.eq(CnecResultExtension.class))).thenReturn(resultExtension2);
        Mockito.when(cnec2.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(MEGAWATT))).thenReturn(600.);
        Mockito.when(cnec2.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(AMPERE))).thenReturn(60.);

        Set<BranchCnec> cnecs = Sets.newHashSet(cnec1, cnec2);

        // In absolute margins, cnec2 is most limiting
        assertSame(cnec2, RaoUtil.getMostLimitingElement(cnecs, variantId, MEGAWATT, false));
        assertSame(cnec2, RaoUtil.getMostLimitingElement(cnecs, variantId, AMPERE, false));

        // In relative margins, cnec1 is most limiting
        assertSame(cnec1, RaoUtil.getMostLimitingElement(cnecs, variantId, MEGAWATT, true));
        assertSame(cnec1, RaoUtil.getMostLimitingElement(cnecs, variantId, AMPERE, true));
    }
}

