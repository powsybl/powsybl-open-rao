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
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationProvider;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowBasedComputationImplTest {
    private static final double EPSILON = 1e-3;
    private FlowBasedComputationProvider flowBasedComputationProvider;
    private Network network;
    private Crac crac;
    private GlskProvider glskProvider;
    private ComputationManager computationManager;
    private FlowBasedComputationParameters parameters;

    @Before
    public void setUp() {
        flowBasedComputationProvider = new FlowBasedComputationImpl();
        network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();
        glskProvider = ExampleGenerator.glskProvider();
        computationManager = LocalComputationManager.getDefault();
        parameters = FlowBasedComputationParameters.load();
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
        FlowBasedComputationResult result = flowBasedComputationProvider.run(network, crac, glskProvider, computationManager, network.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(FlowBasedComputationResult.Status.SUCCESS, result.getStatus());

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

    private double getPreventiveFref(FlowBasedComputationResult result, String monitoredBranch) {
        return result.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(monitoredBranch).getFref();
    }

    private double getPreventiveFmax(FlowBasedComputationResult result, String monitoredBranch) {
        return result.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(monitoredBranch).getFmax();
    }

    private double getPreventivePtdf(FlowBasedComputationResult result, String monitoredBranch, String glskId) {
        return result.getFlowBasedDomain().getDataPreContingency().findMonitoredBranchById(monitoredBranch).findPtdfByCountry(glskId).getPtdf();
    }

    private double getCurativeFref(FlowBasedComputationResult result, String contingency, String monitoredBranch) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchById(monitoredBranch).getFref();
    }

    private double getCurativeFmax(FlowBasedComputationResult result, String contingency, String monitoredBranch) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchById(monitoredBranch).getFmax();
    }

    private double getCurativePtdf(FlowBasedComputationResult result, String contingency, String monitoredBranch, String glskId) {
        return result.getFlowBasedDomain().findContingencyById(contingency).findMonitoredBranchById(monitoredBranch).findPtdfByCountry(glskId).getPtdf();
    }

    private Map<String, Double> frefResultById(FlowBasedComputationResult result) {
        return result.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .collect(Collectors.toMap(
                        DataMonitoredBranch::getId,
                        DataMonitoredBranch::getFref));
    }

    private Map<String, Double> fmaxResultById(FlowBasedComputationResult result) {
        return result.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .collect(Collectors.toMap(
                        DataMonitoredBranch::getId,
                        DataMonitoredBranch::getFmax));
    }

    private Map<String, Map<String, Double>> ptdfResultById(FlowBasedComputationResult result) {
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
