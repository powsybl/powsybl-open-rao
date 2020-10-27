/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationProvider;
import com.farao_community.farao.flowbased_computation.FlowbasedComputationResult;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowbasedComputationImplTest {
    private static final double EPSILON = 1e-3;
    private FlowbasedComputationProvider flowBasedComputationProvider;
    private Network network;
    private Crac crac;
    private GlskProvider glskProvider;
    private FlowbasedComputationParameters parameters;

    @Before
    public void setUp() {
        flowBasedComputationProvider = new FlowbasedComputationImpl();
        network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();
        glskProvider = ExampleGenerator.glskProvider();
        parameters = FlowbasedComputationParameters.load();
    }

    @Test
    public void testProviderName() {
        assertEquals("SimpleIterativeFlowBased", flowBasedComputationProvider.getName());
    }

    @Test
    public void testProviderVersion() {
        assertEquals("1.0.0", flowBasedComputationProvider.getVersion());
    }

    @Test
    public void testRun() {
        FlowbasedComputationResult result = flowBasedComputationProvider.run(network, crac, glskProvider, parameters).join();
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

        assertEquals(0., getCurativeFref(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE"), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE"), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", "10YFR-RTE------C"), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", "10YBE----------2"), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", "10YCB-GERMANY--8"), EPSILON);
        assertEquals(0., getCurativePtdf(result, "N-1 FR-BE", "FR-BE - N-1 - N-1 FR-BE", "10YNL----------L"), EPSILON);

        assertEquals(100, getCurativeFref(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE"), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE"), EPSILON);
        assertEquals(0.75, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", "10YFR-RTE------C"), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", "10YBE----------2"), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", "10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "FR-DE - N-1 - N-1 FR-BE", "10YNL----------L"), EPSILON);

        assertEquals(0, getCurativeFref(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE"), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE"), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", "10YFR-RTE------C"), EPSILON);
        assertEquals(0.75, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", "10YBE----------2"), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", "10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.25, getCurativePtdf(result, "N-1 FR-BE", "BE-NL - N-1 - N-1 FR-BE", "10YNL----------L"), EPSILON);

        assertEquals(100, getCurativeFref(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE"), EPSILON);
        assertEquals(100, getCurativeFmax(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE"), EPSILON);
        assertEquals(0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", "10YFR-RTE------C"), EPSILON);
        assertEquals(-0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", "10YBE----------2"), EPSILON);
        assertEquals(0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", "10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.5, getCurativePtdf(result, "N-1 FR-BE", "DE-NL - N-1 - N-1 FR-BE", "10YNL----------L"), EPSILON);

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

    private double getCurativeFref(FlowbasedComputationResult result, String contingency, String monitoredBranch) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchById(monitoredBranch).getFref();
    }

    private double getCurativeFmax(FlowbasedComputationResult result, String contingency, String monitoredBranch) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchById(monitoredBranch).getFmax();
    }

    private double getCurativePtdf(FlowbasedComputationResult result, String contingency, String monitoredBranch, String glskId) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchById(monitoredBranch).findPtdfByCountry(glskId).getPtdf();
    }

    private Map<String, Double> frefResultById(FlowbasedComputationResult result) {
        return result.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .collect(Collectors.toMap(
                        DataMonitoredBranch::getId,
                        DataMonitoredBranch::getFref));
    }

    private Map<String, Double> fmaxResultById(FlowbasedComputationResult result) {
        return result.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .collect(Collectors.toMap(
                        DataMonitoredBranch::getId,
                        DataMonitoredBranch::getFmax));
    }

    private Map<String, Map<String, Double>> ptdfResultById(FlowbasedComputationResult result) {
        return result.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
            .collect(Collectors.toMap(
                DataMonitoredBranch::getId,
                dataMonitoredBranch -> dataMonitoredBranch.getPtdfList().stream()
                .collect(Collectors.toMap(
                    DataPtdfPerCountry::getCountry,
                    DataPtdfPerCountry::getPtdf
                ))
            ));
    }
}
