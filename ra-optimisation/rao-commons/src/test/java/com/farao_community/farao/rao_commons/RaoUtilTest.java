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
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MaxMinMarginParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MaxMinRelativeMarginParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.CostEvaluator;
import com.farao_community.farao.rao_commons.objective_function_evaluator.MinMarginObjectiveFunction;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.apache.commons.compress.utils.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

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
    private RaoData raoDataSpy;
    private RaoInput raoInput;
    private Network network;
    private Crac crac;
    private String variantId;
    private LinearOptimizerParameters linearOptimizerParameters;
    private double fallbackOverCost;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        variantId = network.getVariantManager().getWorkingVariantId();
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
                .withNetworkVariantId(variantId)
                .build();
        raoParameters = new RaoParameters();
        fallbackOverCost = raoParameters.getFallbackOverCost();
        raoData = new RaoData(network, crac, crac.getPreventiveState(), crac.getStates(Instant.PREVENTIVE), null, null, null, raoParameters);
        raoDataSpy = Mockito.spy(raoData);
        Mockito.doReturn(new CnecResults()).when(raoDataSpy).getInitialCnecResults();
        Mockito.doReturn(new HashMap<>()).when(raoDataSpy).getPrePerimeterMarginsInAbsoluteMW();
    }

    private void addGlskProvider() {
        ZonalData<LinearGlsk> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
                .getZonalGlsks(network);
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
                .withNetworkVariantId(variantId)
                .withGlskProvider(glskProvider)
                .withBaseCracVariantId(crac.getExtension(ResultVariantManager.class).getVariants().iterator().next())
                .build();
    }

    @Test
    public void createCostEvaluatorFromRaoParametersMegawatt() {
        linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(MAX_MIN_MARGIN_IN_MEGAWATT)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoDataSpy, linearOptimizerParameters, fallbackOverCost);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(MEGAWATT, costEvaluator.getUnit());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersAmps() {
        linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(MAX_MIN_MARGIN_IN_AMPERE)
                .withMaxMinMarginParameters(new MaxMinMarginParameters(0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoDataSpy, linearOptimizerParameters, fallbackOverCost);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(AMPERE, costEvaluator.getUnit());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersRelativeMW() {
        linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoDataSpy, linearOptimizerParameters, fallbackOverCost);
        assertTrue(costEvaluator instanceof MinMarginObjectiveFunction);
        assertEquals(MEGAWATT, costEvaluator.getUnit());
    }

    @Test
    public void createCostEvaluatorFromRaoParametersRelativeAmps() {
        linearOptimizerParameters = LinearOptimizerParameters.create()
                .withObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)
                .withMaxMinRelativeMarginParameters(new MaxMinRelativeMarginParameters(0.01, 1000, 0.01))
                .withPstSensitivityThreshold(0.01)
                .build();
        CostEvaluator costEvaluator = RaoUtil.createObjectiveFunction(raoDataSpy, linearOptimizerParameters, fallbackOverCost);
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
        raoData = new RaoData(
                raoInput.getNetwork(),
                raoInput.getCrac(),
                raoInput.getOptimizedState(),
                raoInput.getPerimeter(),
                raoInput.getReferenceProgram(),
                raoInput.getGlskProvider(),
                raoInput.getBaseCracVariantId(),
                raoParameters);
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoData, false);
        assertNotNull(systematicSensitivityInterface);
    }

    @Test(expected = FaraoException.class)
    public void testExceptionForGlskOnRelativeMargin() {
        raoParameters.setRelativeMarginPtdfBoundariesFromString(new ArrayList<>(Arrays.asList("FR:ES", "ES:PT")));
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
        raoParameters.setRelativeMarginPtdfBoundariesFromString(new ArrayList<>());
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        RaoUtil.checkParameters(raoParameters, raoInput);
    }

    @Test
    public void testCreateSystematicSensitivityInterfaceOnRelativeMargin() {
        raoParameters.setRelativeMarginPtdfBoundariesFromString(new ArrayList<>(Arrays.asList("{FR}-{BE}", "{BE}-{NL}", "{FR}-{DE}", "{DE}-{NL}")));
        addGlskProvider();
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT);
        raoData = new RaoData(
                raoInput.getNetwork(),
                raoInput.getCrac(),
                raoInput.getOptimizedState(),
                raoInput.getPerimeter(),
                raoInput.getReferenceProgram(),
                raoInput.getGlskProvider(),
                raoInput.getBaseCracVariantId(),
                raoParameters);
        SystematicSensitivityInterface systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoData, true);
        assertNotNull(systematicSensitivityInterface);
    }

    @Test(expected = FaraoException.class)
    public void testAmpereWithDc() {
        raoParameters.setObjectiveFunction(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        raoParameters.getDefaultSensitivityAnalysisParameters().getLoadFlowParameters().setDc(true);
        RaoUtil.checkParameters(raoParameters, raoInput);
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

    private Set<BranchCnec> setUpMockCnecs(boolean optimized, boolean monitored) {
        // CNEC 1 : margin of 1000 MW / 100 A, sum of PTDFs = 1
        CnecResult result1 = Mockito.mock(CnecResult.class);
        Mockito.when(result1.getAbsolutePtdfSum()).thenReturn(1.0);
        CnecResultExtension resultExtension1 = Mockito.mock(CnecResultExtension.class);
        Mockito.when(resultExtension1.getVariant(Mockito.anyString())).thenReturn(result1);

        BranchCnec cnec1 = Mockito.mock(BranchCnec.class);
        Mockito.when(cnec1.getId()).thenReturn("cnec1");
        Mockito.when(cnec1.isOptimized()).thenReturn(optimized);
        Mockito.when(cnec1.isMonitored()).thenReturn(monitored);
        Mockito.when(cnec1.getExtension(Mockito.eq(CnecResultExtension.class))).thenReturn(resultExtension1);
        Mockito.when(cnec1.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(MEGAWATT))).thenReturn(1000.);
        Mockito.when(cnec1.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(AMPERE))).thenReturn(100.);

        // CNEC 2 : margin of 600 MW / 60 A, sum of PTDFs = 0.5
        CnecResult result2 = Mockito.mock(CnecResult.class);
        Mockito.when(result2.getAbsolutePtdfSum()).thenReturn(0.5);
        CnecResultExtension resultExtension2 = Mockito.mock(CnecResultExtension.class);
        Mockito.when(resultExtension2.getVariant(Mockito.anyString())).thenReturn(result2);

        BranchCnec cnec2 = Mockito.mock(BranchCnec.class);
        Mockito.when(cnec2.getId()).thenReturn("cnec2");
        Mockito.when(cnec2.isOptimized()).thenReturn(optimized);
        Mockito.when(cnec2.isMonitored()).thenReturn(monitored);
        Mockito.when(cnec2.getExtension(Mockito.eq(CnecResultExtension.class))).thenReturn(resultExtension2);
        Mockito.when(cnec2.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(MEGAWATT))).thenReturn(600.);
        Mockito.when(cnec2.computeMargin(Mockito.anyDouble(), Mockito.any(), Mockito.eq(AMPERE))).thenReturn(60.);

        return Sets.newHashSet(cnec1, cnec2);
    }

    @Test
    public void testGetMostLimitingElement() {
        Set<BranchCnec> cnecs = setUpMockCnecs(true, false);

        // In absolute margins, cnec2 is most limiting
        assertEquals("cnec2", RaoUtil.getMostLimitingElement(cnecs, variantId, MEGAWATT, false).getId());
        assertEquals("cnec2", RaoUtil.getMostLimitingElement(cnecs, variantId, AMPERE, false).getId());

        // In relative margins, cnec1 is most limiting
        assertEquals("cnec1", RaoUtil.getMostLimitingElement(cnecs, variantId, MEGAWATT, true).getId());
        assertEquals("cnec1", RaoUtil.getMostLimitingElement(cnecs, variantId, AMPERE, true).getId());
    }

    @Test
    public void testGetMostLimitingElementOnPureMnecs() {
        Set<BranchCnec> cnecs = setUpMockCnecs(false, true);

        // In absolute margins, cnec2 is most limiting
        assertEquals("cnec2", RaoUtil.getMostLimitingElement(cnecs, variantId, MEGAWATT, false).getId());
        assertEquals("cnec2", RaoUtil.getMostLimitingElement(cnecs, variantId, AMPERE, false).getId());

        // In relative margins, cnec1 is most limiting
        assertEquals("cnec1", RaoUtil.getMostLimitingElement(cnecs, variantId, MEGAWATT, true).getId());
        assertEquals("cnec1", RaoUtil.getMostLimitingElement(cnecs, variantId, AMPERE, true).getId());

    }
}
