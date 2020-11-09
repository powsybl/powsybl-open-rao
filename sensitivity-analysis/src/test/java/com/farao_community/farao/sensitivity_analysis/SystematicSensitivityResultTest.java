/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.glsk.api.GlskProvider;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SystematicSensitivityResultTest {
    private static final double EPSILON = 1e-2;

    private Network network;
    private Cnec nStateCnec;
    private Cnec contingencyCnec;
    private RangeAction rangeAction;
    private LinearGlsk linearGlsk;

    private RangeActionSensitivityProvider rangeActionSensitivityProvider;
    private PtdfSensitivityProvider ptdfSensitivityProvider;

    @Before
    public void setUp() throws SAXException, ParserConfigurationException, IOException {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPstRange();

        GlskProvider glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes.xml"))
            .getGlskProvider(network, Instant.parse("2016-07-28T22:30:00Z"));

        // Ra Provider
        rangeActionSensitivityProvider = new RangeActionSensitivityProvider();
        rangeActionSensitivityProvider.addSensitivityFactors(crac.getRangeActions(), crac.getCnecs());

        // Ptdf Provider
        ptdfSensitivityProvider = new PtdfSensitivityProvider(glskProvider);
        ptdfSensitivityProvider.addCnecs(crac.getCnecs());

        nStateCnec = crac.getCnec("cnec1basecase");
        rangeAction = crac.getRangeAction("pst");
        contingencyCnec = crac.getCnec("cnec1stateCurativeContingency1");
        linearGlsk = glskProvider.getLinearGlsk("10YFR-RTE------C");
    }

    @Test
    public void testCompleteRaResultManipulation() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.run(network, network.getVariantManager().getWorkingVariantId(), rangeActionSensitivityProvider, rangeActionSensitivityProvider, SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult(sensitivityAnalysisResult);

        // Then
        assertTrue(result.isSuccess());

        //  in basecase
        assertEquals(10, result.getReferenceFlow(nStateCnec), EPSILON);
        assertEquals(100, result.getReferenceIntensity(nStateCnec), EPSILON);
        assertEquals(0.5, result.getSensitivityOnFlow(rangeAction, nStateCnec), EPSILON);
        assertEquals(0.25, result.getSensitivityOnIntensity(rangeAction, nStateCnec), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec), EPSILON);
        assertEquals(-5, result.getSensitivityOnFlow(rangeAction, contingencyCnec), EPSILON);
        assertEquals(-5, result.getSensitivityOnIntensity(rangeAction, contingencyCnec), EPSILON);
    }

    @Test
    public void testCompletePtdfResultManipulation() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.run(network, network.getVariantManager().getWorkingVariantId(), ptdfSensitivityProvider, ptdfSensitivityProvider, SensitivityAnalysisParameters.load());
        SystematicSensitivityResult result = new SystematicSensitivityResult(sensitivityAnalysisResult);

        // Then
        assertTrue(result.isSuccess());

        //  in basecase
        assertEquals(40, result.getReferenceFlow(nStateCnec), EPSILON);
        assertEquals(0.140, result.getSensitivityOnFlow(linearGlsk, nStateCnec), EPSILON);

        //  after contingency
        assertEquals(-13, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(6, result.getSensitivityOnFlow(linearGlsk, contingencyCnec), EPSILON);
    }

    @Test
    public void testIncompleteSensiResult() {
        // When
        SensitivityAnalysisResult sensitivityAnalysisResult = Mockito.mock(SensitivityAnalysisResult.class);
        Mockito.when(sensitivityAnalysisResult.isOk()).thenReturn(false);
        SystematicSensitivityResult result = new SystematicSensitivityResult(sensitivityAnalysisResult);

        // Then
        assertFalse(result.isSuccess());
    }

    @AutoService(SensitivityAnalysisProvider.class)
    public static final class MockSensiProvider implements SensitivityAnalysisProvider {
        @Override
        public CompletableFuture<SensitivityAnalysisResult> run(Network network, String s, SensitivityFactorsProvider sensitivityFactorsProvider, ContingenciesProvider contingenciesProvider, SensitivityAnalysisParameters sensitivityAnalysisParameters, ComputationManager computationManager) {
            List<SensitivityValue> nStateValues = sensitivityFactorsProvider.getFactors(network).stream()
                    .map(factor -> {
                        if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof PhaseTapChangerAngle) {
                            return new SensitivityValue(factor, 0.5, 10, 10);
                        } else if (factor.getFunction() instanceof BranchIntensity && factor.getVariable() instanceof PhaseTapChangerAngle) {
                            return new SensitivityValue(factor, 0.25, 100, -10);
                        } else if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof LinearGlsk) {
                            return new SensitivityValue(factor, 0.140, 40, -11);
                        } else {
                            throw new AssertionError();
                        }
                    })
                    .collect(Collectors.toList());
            Map<String, List<SensitivityValue>> contingenciesValues = contingenciesProvider.getContingencies(network).stream()
                    .collect(Collectors.toMap(
                        contingency -> contingency.getId(),
                        contingency -> sensitivityFactorsProvider.getFactors(network).stream()
                                .map(factor -> {
                                    if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof PhaseTapChangerAngle) {
                                        return new SensitivityValue(factor, -5, -20, 20);
                                    } else if (factor.getFunction() instanceof BranchIntensity && factor.getVariable() instanceof PhaseTapChangerAngle) {
                                        return new SensitivityValue(factor, 5, 200, -20);
                                    } else if (factor.getFunction() instanceof BranchFlow && factor.getVariable() instanceof LinearGlsk) {
                                        return new SensitivityValue(factor, 6, -13, 15);
                                    } else {
                                        throw new AssertionError();
                                    }
                                })
                                .collect(Collectors.toList())
                    ));
            return CompletableFuture.completedFuture(new SensitivityAnalysisResult(true, Collections.emptyMap(), "", nStateValues, contingenciesValues));
        }

        @Override
        public String getName() {
            return "MockSensi";
        }

        @Override
        public String getVersion() {
            return "0";
        }
    }
}
