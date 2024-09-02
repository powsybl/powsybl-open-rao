/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.flowbasedcomputation.impl;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultimpl.ElementaryFlowCnecResult;
import com.powsybl.openrao.data.raoresultimpl.FlowCnecResult;
import com.powsybl.openrao.data.raoresultimpl.RaoResultImpl;
import com.powsybl.openrao.flowbasedcomputation.FlowbasedComputationParameters;
import com.powsybl.openrao.flowbasedcomputation.FlowbasedComputationProvider;
import com.powsybl.openrao.flowbasedcomputation.FlowbasedComputationResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.AMPERE;
import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class FlowbasedComputationImplTest {
    private static final double EPSILON = 1e-3;

    private FlowbasedComputationProvider flowBasedComputationProvider;
    private Network network;
    private ZonalData<SensitivityVariableSet> glsk;
    private FlowbasedComputationParameters parameters;

    @BeforeEach
    public void setUp() {
        flowBasedComputationProvider = new FlowbasedComputationImpl();
        network = ExampleGenerator.network();
        glsk = ExampleGenerator.glskProvider();
        parameters = FlowbasedComputationParameters.load();
        parameters.setSensitivityProvider("MockSensitivity");
    }

    @Test
    void testProviderName() {
        assertEquals("SimpleIterativeFlowBased", flowBasedComputationProvider.getName());
    }

    @Test
    void testProviderVersion() {
        assertEquals("1.0.0", flowBasedComputationProvider.getVersion());
    }

    @Test
    void testRunWithCra() throws IOException {
        Crac crac = ExampleGenerator.crac("crac.json", network);
        assertTrue(network.getBranch("FR-BE").getTerminal1().isConnected());
        assertTrue(network.getBranch("FR-BE").getTerminal2().isConnected());
        FlowbasedComputationResult result = flowBasedComputationProvider.run(network, crac, null, glsk, parameters).join();
        checkAssertions(result);
        checkCurativeAssertions(result);
    }

    @Test
    void testRunWithCraRaoResult() throws IOException {
        Crac crac = ExampleGenerator.crac("crac_for_rao_result.json", network);
        assertTrue(network.getBranch("FR-BE").getTerminal1().isConnected());
        assertTrue(network.getBranch("FR-BE").getTerminal2().isConnected());
        RaoResult raoResult = createRaoResult(crac, crac.getFlowCnecs(), crac.getNetworkAction("Open line FR-BE"));
        FlowbasedComputationResult result = flowBasedComputationProvider.run(network, crac, raoResult, glsk, parameters).join();
        checkAssertions(result);
        checkCurativeAssertions(result);
    }

    @Test
    void testRunPraWithForced() throws IOException {
        Crac crac = ExampleGenerator.crac("crac_with_forced.json", network);
        FlowbasedComputationResult result = flowBasedComputationProvider.run(network, crac, null, glsk, parameters).join();
        checkAssertions(result);
    }

    @Test
    void testRunPraWithExtension() throws IOException {
        Crac crac = ExampleGenerator.crac("crac_with_extension.json", network);
        FlowbasedComputationResult result = flowBasedComputationProvider.run(network, crac, null, glsk, parameters).join();
        checkAssertions(result);
    }

    private void checkAssertions(FlowbasedComputationResult result) {
        assertEquals(FlowbasedComputationResult.Status.SUCCESS, result.getStatus());

        assertEquals(50, getPreventiveFref(result, "FR-BE - N - preventive"), EPSILON);
        assertEquals(100, getPreventiveFmax(result, "FR-BE - N - preventive"), EPSILON);
        assertEquals(0.375, getPreventivePtdf(result, "FR-BE - N - preventive", "10YFR-RTE------C"), EPSILON);
        assertEquals(-0.375, getPreventivePtdf(result, "FR-BE - N - preventive", "10YBE----------2"), EPSILON);
        assertEquals(0.125, getPreventivePtdf(result, "FR-BE - N - preventive", "10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.125, getPreventivePtdf(result, "FR-BE - N - preventive", "10YNL----------L"), EPSILON);

        assertEquals(50, getPreventiveFref(result, "FR-DE - N - preventive"), EPSILON);
        assertEquals(100, getPreventiveFmax(result, "FR-DE - N - preventive"), EPSILON);
        assertEquals(0.375, getPreventivePtdf(result, "FR-DE - N - preventive", "10YFR-RTE------C"), EPSILON);
        assertEquals(0.125, getPreventivePtdf(result, "FR-DE - N - preventive", "10YBE----------2"), EPSILON);
        assertEquals(-0.375, getPreventivePtdf(result, "FR-DE - N - preventive", "10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.125, getPreventivePtdf(result, "FR-DE - N - preventive", "10YNL----------L"), EPSILON);

        assertEquals(50, getPreventiveFref(result, "BE-NL - N - preventive"), EPSILON);
        assertEquals(100, getPreventiveFmax(result, "BE-NL - N - preventive"), EPSILON);
        assertEquals(0.125, getPreventivePtdf(result, "BE-NL - N - preventive", "10YFR-RTE------C"), EPSILON);
        assertEquals(0.375, getPreventivePtdf(result, "BE-NL - N - preventive", "10YBE----------2"), EPSILON);
        assertEquals(-0.125, getPreventivePtdf(result, "BE-NL - N - preventive", "10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.375, getPreventivePtdf(result, "BE-NL - N - preventive", "10YNL----------L"), EPSILON);

        assertEquals(50, getPreventiveFref(result, "DE-NL - N - preventive"), EPSILON);
        assertEquals(100, getPreventiveFmax(result, "FR-BE - N - preventive"), EPSILON);
        assertEquals(0.125, getPreventivePtdf(result, "DE-NL - N - preventive", "10YFR-RTE------C"), EPSILON);
        assertEquals(-0.125, getPreventivePtdf(result, "DE-NL - N - preventive", "10YBE----------2"), EPSILON);
        assertEquals(0.375, getPreventivePtdf(result, "DE-NL - N - preventive", "10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.375, getPreventivePtdf(result, "DE-NL - N - preventive", "10YNL----------L"), EPSILON);

        String onOutageId = "outage";
        assertEquals(0., getCurativeFref(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", onOutageId), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", onOutageId), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", "10YFR-RTE------C", onOutageId), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", "10YBE----------2", onOutageId), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", "10YCB-GERMANY--8", onOutageId), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", "10YNL----------L", onOutageId), EPSILON);

        assertEquals(100, getCurativeFref(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", onOutageId), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", onOutageId), EPSILON);
        assertEquals(0.75, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", "10YFR-RTE------C", onOutageId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", "10YBE----------2", onOutageId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", "10YCB-GERMANY--8", onOutageId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", "10YNL----------L", onOutageId), EPSILON);

        assertEquals(0, getCurativeFref(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", onOutageId), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", onOutageId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", "10YFR-RTE------C", onOutageId), EPSILON);
        assertEquals(0.75, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", "10YBE----------2", onOutageId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", "10YCB-GERMANY--8", onOutageId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", "10YNL----------L", onOutageId), EPSILON);

        assertEquals(100, getCurativeFref(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", onOutageId), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", onOutageId), EPSILON);
        assertEquals(0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", "10YFR-RTE------C", onOutageId), EPSILON);
        assertEquals(-0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", "10YBE----------2", onOutageId), EPSILON);
        assertEquals(0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", "10YCB-GERMANY--8", onOutageId), EPSILON);
        assertEquals(-0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", "10YNL----------L", onOutageId), EPSILON);
    }

    private void checkCurativeAssertions(FlowbasedComputationResult result) {
        String afterCraId = "curative";
        assertEquals(0., getCurativeFref(result, "N-1 FR-BE", "FR-BE - AfterCra - N-1 FR-BE", afterCraId), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "FR-BE - AfterCra - N-1 FR-BE", afterCraId), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - AfterCra - N-1 FR-BE", "10YFR-RTE------C", afterCraId), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - AfterCra - N-1 FR-BE", "10YBE----------2", afterCraId), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - AfterCra - N-1 FR-BE", "10YCB-GERMANY--8", afterCraId), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - AfterCra - N-1 FR-BE", "10YNL----------L", afterCraId), EPSILON);

        assertEquals(100, getCurativeFref(result, "N-1 FR-BE", "FR-DE - AfterCra - N-1 FR-BE", afterCraId), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "FR-DE - AfterCra - N-1 FR-BE", afterCraId), EPSILON);
        assertEquals(0.75, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - AfterCra - N-1 FR-BE", "10YFR-RTE------C", afterCraId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - AfterCra - N-1 FR-BE", "10YBE----------2", afterCraId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - AfterCra - N-1 FR-BE", "10YCB-GERMANY--8", afterCraId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - AfterCra - N-1 FR-BE", "10YNL----------L", afterCraId), EPSILON);

        assertEquals(0, getCurativeFref(result, "N-1 FR-BE", "BE-NL - AfterCra - N-1 FR-BE", afterCraId), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "BE-NL - AfterCra - N-1 FR-BE", afterCraId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - AfterCra - N-1 FR-BE", "10YFR-RTE------C", afterCraId), EPSILON);
        assertEquals(0.75, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - AfterCra - N-1 FR-BE", "10YBE----------2", afterCraId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - AfterCra - N-1 FR-BE", "10YCB-GERMANY--8", afterCraId), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - AfterCra - N-1 FR-BE", "10YNL----------L", afterCraId), EPSILON);

        assertEquals(100, getCurativeFref(result, "N-1 FR-BE", "DE-NL - AfterCra - N-1 FR-BE", afterCraId), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "DE-NL - AfterCra - N-1 FR-BE", afterCraId), EPSILON);
        assertEquals(0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - AfterCra - N-1 FR-BE", "10YFR-RTE------C", afterCraId), EPSILON);
        assertEquals(-0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - AfterCra - N-1 FR-BE", "10YBE----------2", afterCraId), EPSILON);
        assertEquals(0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - AfterCra - N-1 FR-BE", "10YCB-GERMANY--8", afterCraId), EPSILON);
        assertEquals(-0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - AfterCra - N-1 FR-BE", "10YNL----------L", afterCraId), EPSILON);
    }

    private double getPreventiveFref(FlowbasedComputationResult result, String monitoredBranch) {
        return result.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(monitoredBranch).getFref();
    }

    private double getPreventiveFmax(FlowbasedComputationResult result, String monitoredBranch) {
        return result.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(monitoredBranch).getFmax();
    }

    private double getPreventivePtdf(FlowbasedComputationResult result, String monitoredBranch, String glskId) {
        return result.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(monitoredBranch).findPtdfByCountry(glskId).getPtdf();
    }

    private double getCurativeFref(FlowbasedComputationResult result, String contingency, String monitoredBranch, String instantId) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchByIdAndInstant(monitoredBranch, instantId).getFref();
    }

    private double getCurativeFmax(FlowbasedComputationResult result, String contingency, String monitoredBranch, String instantId) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchByIdAndInstant(monitoredBranch, instantId).getFmax();
    }

    private double getCurativePtdf(FlowbasedComputationResult result, String contingency, String monitoredBranch, String glskId, String instantId) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchByIdAndInstant(monitoredBranch, instantId).findPtdfByCountry(glskId).getPtdf();
    }

    private RaoResult createRaoResult(Crac crac, Set<FlowCnec> flowCnecs, NetworkAction na) {
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        Instant curativeInstant = crac.getInstant("curative");

        // Warning: these results on cnecs are not relevant, and maybe not coherent with
        // the hardcoded results of FlowbasedComputationProviderMock, used in this test class.
        flowCnecs.forEach(cnec -> {
            FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(cnec);

            flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
            ElementaryFlowCnecResult elementaryFlowCnecResult = flowCnecResult.getResult(null);

            elementaryFlowCnecResult.setFlow(TwoSides.ONE, 100., MEGAWATT);
            elementaryFlowCnecResult.setMargin(101., MEGAWATT);
            elementaryFlowCnecResult.setRelativeMargin(102., MEGAWATT);
            elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 103., MEGAWATT);
            elementaryFlowCnecResult.setCommercialFlow(TwoSides.ONE, 104., MEGAWATT);

            elementaryFlowCnecResult.setFlow(TwoSides.ONE, 110., AMPERE);
            elementaryFlowCnecResult.setMargin(111., AMPERE);
            elementaryFlowCnecResult.setRelativeMargin(112., AMPERE);
            elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 113., AMPERE);
            elementaryFlowCnecResult.setCommercialFlow(TwoSides.ONE, 114., AMPERE);

            elementaryFlowCnecResult.setPtdfZonalSum(TwoSides.ONE, 0.1);

            flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(curativeInstant);
            elementaryFlowCnecResult = flowCnecResult.getResult(curativeInstant);

            elementaryFlowCnecResult.setFlow(TwoSides.ONE, 200., MEGAWATT);
            elementaryFlowCnecResult.setMargin(201., MEGAWATT);
            elementaryFlowCnecResult.setRelativeMargin(202., MEGAWATT);
            elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 203., MEGAWATT);

            elementaryFlowCnecResult.setFlow(TwoSides.ONE, 210., AMPERE);
            elementaryFlowCnecResult.setMargin(211., AMPERE);
            elementaryFlowCnecResult.setRelativeMargin(212., AMPERE);
            elementaryFlowCnecResult.setLoopFlow(TwoSides.ONE, 213., AMPERE);

            elementaryFlowCnecResult.setPtdfZonalSum(TwoSides.ONE, 0.1);
        });

        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("N-1 FR-BE", curativeInstant));

        raoResult.setComputationStatus(ComputationStatus.DEFAULT);

        return raoResult;
    }
}
