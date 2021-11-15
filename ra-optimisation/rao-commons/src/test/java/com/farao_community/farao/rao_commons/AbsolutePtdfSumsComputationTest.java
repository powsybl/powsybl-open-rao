/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.farao_community.farao.rao_api.ZoneToZonePtdfDefinition;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.PREVENTIVE;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AbsolutePtdfSumsComputationTest {
    private static final double DOUBLE_TOLERANCE = 0.001;

    private SystematicSensitivityResult systematicSensitivityResult;

    @Before
    public void setUp() {

        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(Mockito.any(LinearGlsk.class), Mockito.any(FlowCnec.class)))
            .thenAnswer(
                new Answer<Double>() {
                    @Override public Double answer(InvocationOnMock invocation) {
                        LinearGlsk linearGlsk = (LinearGlsk) invocation.getArguments()[0];
                        FlowCnec branchCnec = (FlowCnec) invocation.getArguments()[1];
                        if (branchCnec.getId().contains("cnec1")) {
                            switch (linearGlsk.getId().substring(0, EICode.EIC_LENGTH)) {
                                case "10YFR-RTE------C":
                                    return 0.1;
                                case "10YBE----------2":
                                    return 0.2;
                                case "10YCB-GERMANY--8":
                                    return 0.3;
                                case "22Y201903145---4":
                                    return 0.4;
                                case "22Y201903144---9":
                                    return 0.1;
                                default:
                                    return 0.;
                            }
                        } else if (branchCnec.getId().contains("cnec2")) {
                            switch (linearGlsk.getId().substring(0, EICode.EIC_LENGTH)) {
                                case "10YFR-RTE------C":
                                    return 0.3;
                                case "10YBE----------2":
                                    return 0.3;
                                case "10YCB-GERMANY--8":
                                    return 0.2;
                                case "22Y201903145---4":
                                    return 0.1;
                                case "22Y201903144---9":
                                    return 0.9;
                                default:
                                    return 0.;
                            }
                        } else {
                            return 0.;
                        }
                    }
                });
    }

    @Test
    public void testComputation() {

        // prepare data
        Network network = NetworkImportsUtil.import12NodesNetwork();
        ZonalData<LinearGlsk> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes_with_alegro.xml"))
                .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));
        Crac crac = CommonCracCreation.create();
        List<ZoneToZonePtdfDefinition> boundaries = Arrays.asList(
                new ZoneToZonePtdfDefinition("{FR}-{BE}"),
                new ZoneToZonePtdfDefinition("{FR}-{DE}"),
                new ZoneToZonePtdfDefinition("{DE}-{BE}"),
                new ZoneToZonePtdfDefinition("{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}"));

        // compute zToz PTDF sum
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = new AbsolutePtdfSumsComputation(glskProvider, boundaries, network);
        Map<FlowCnec, Double> ptdfSums = absolutePtdfSumsComputation.computeAbsolutePtdfSums(crac.getFlowCnecs(), systematicSensitivityResult);

        // test results
        assertEquals(0.6, ptdfSums.get(crac.getFlowCnec("cnec1basecase")), DOUBLE_TOLERANCE); // abs(0.1 - 0.2) + abs(0.1 - 0.3) + abs(0.3 - 0.2) + abs(0.2 - 0.1 - 0.3 + 0.4) = 0.1 + 0.2 + 0.1 + 0.2
        assertEquals(0.9, ptdfSums.get(crac.getFlowCnec("cnec2basecase")), DOUBLE_TOLERANCE); // abs(0.3 - 0.3) + abs(0.3 - 0.2) + abs(0.2 - 0.3) + abs(0.3 - 0.9 - 0.2 + 0.1) = 0 + 0.1 + 0.1 + 0.7
    }

    @Test
    public void testIgnoreAbsentGlsk() {

        // prepare data
        Network network = NetworkImportsUtil.import12NodesNetwork();
        ZonalData<LinearGlsk> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_proportional_12nodes_with_alegro.xml"))
                .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));
        Crac crac = CommonCracCreation.create();
        List<ZoneToZonePtdfDefinition> boundaries = Arrays.asList(
                new ZoneToZonePtdfDefinition("{FR}-{BE}"),
                new ZoneToZonePtdfDefinition("{FR}-{DE}"),
                new ZoneToZonePtdfDefinition("{DE}-{BE}"),
                new ZoneToZonePtdfDefinition("{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}"),
                new ZoneToZonePtdfDefinition("{FR}-{ES}"), // ES doesn't exist in GLSK map
                new ZoneToZonePtdfDefinition("{ES}-{DE}"), // ES doesn't exist in GLSK map
                new ZoneToZonePtdfDefinition("{22Y201903144---0}-{22Y201903144---1}")); // EICodes that don't exist in GLSK map

        // compute zToz PTDF sum
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = new AbsolutePtdfSumsComputation(glskProvider, boundaries, network);
        Map<FlowCnec, Double> ptdfSums = absolutePtdfSumsComputation.computeAbsolutePtdfSums(crac.getFlowCnecs(), systematicSensitivityResult);

        // Test that these 3 new boundaries are ignored (results should be the same as previous test)
        assertEquals(0.6, ptdfSums.get(crac.getFlowCnec("cnec1basecase")), DOUBLE_TOLERANCE);
        assertEquals(0.9, ptdfSums.get(crac.getFlowCnec("cnec2basecase")), DOUBLE_TOLERANCE);
    }

    @Test
    public void testIgnoreGlskOnDisconnectedXnodes() {

        // prepare data
        Network network = Importers.loadNetwork("network_with_alegro_hub.xiidm", getClass().getResourceAsStream("/network_with_alegro_hub.xiidm"));
        ZonalData<LinearGlsk> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk_with_virtual_hubs.xml"))
                .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));

        Crac crac = CracFactory.findDefault().create("cracId");

        crac.newContingency()
                .withId("contingency-on-internal-line")
                .withNetworkElement("FFR1AA1  FFR3AA1  1")
                .add();
        crac.newContingency()
                .withId("contingency-on-xnode")
                .withNetworkElement("FFR1AA1  XLI_OB1B 1")
                .add();

        crac.newFlowCnec()
                .withId("cnec1-in-basecase")
                .withNetworkElement("NNL2AA1  NNL3AA1  1")
                .withInstant(PREVENTIVE)
                .withOptimized(true)
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(1000.).withUnit(Unit.MEGAWATT).add()
                .add();
        crac.newFlowCnec()
                .withId("cnec1-after-internal-contingency")
                .withNetworkElement("NNL2AA1  NNL3AA1  1")
                .withInstant(CURATIVE)
                .withContingency("contingency-on-internal-line")
                .withOptimized(true)
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(1000.).withUnit(Unit.MEGAWATT).add()
                .add();
        crac.newFlowCnec()
                .withId("cnec1-after-contingency-on-xNode")
                .withNetworkElement("NNL2AA1  NNL3AA1  1")
                .withInstant(CURATIVE)
                .withContingency("contingency-on-xnode")
                .withOptimized(true)
                .newThreshold().withRule(BranchThresholdRule.ON_LEFT_SIDE).withMax(1000.).withUnit(Unit.MEGAWATT).add()
                .add();
        List<ZoneToZonePtdfDefinition> boundaries = Arrays.asList(
                new ZoneToZonePtdfDefinition("{BE}-{22Y201903144---9}"));

        // compute zToz PTDF sum
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = new AbsolutePtdfSumsComputation(glskProvider, boundaries, network);
        Map<FlowCnec, Double> ptdfSums = absolutePtdfSumsComputation.computeAbsolutePtdfSums(crac.getFlowCnecs(), systematicSensitivityResult);

        // test results
        assertEquals(0.1, ptdfSums.get(crac.getFlowCnec("cnec1-in-basecase")), DOUBLE_TOLERANCE); // abs(0.2 - 0.1)
        assertEquals(0.1, ptdfSums.get(crac.getFlowCnec("cnec1-after-internal-contingency")), DOUBLE_TOLERANCE); // abs(0.2 - 0.1)
        assertEquals(0.2, ptdfSums.get(crac.getFlowCnec("cnec1-after-contingency-on-xNode")), DOUBLE_TOLERANCE); // abs(0.2 - 0.0) PTDF of virtual hub is now 0
    }
}
