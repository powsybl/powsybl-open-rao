/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.raoapi.ZoneToZonePtdfDefinition;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class AbsolutePtdfSumsComputationTest {
    private static final double DOUBLE_TOLERANCE = 0.001;

    private SystematicSensitivityResult systematicSensitivityResult;

    @BeforeEach
    public void setUp() {

        systematicSensitivityResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(systematicSensitivityResult.getSensitivityOnFlow(Mockito.any(SensitivityVariableSet.class), Mockito.any(FlowCnec.class), Mockito.any(TwoSides.class)))
            .thenAnswer(
                (Answer<Double>) invocation -> {
                    SensitivityVariableSet linearGlsk = (SensitivityVariableSet) invocation.getArguments()[0];
                    FlowCnec branchCnec = (FlowCnec) invocation.getArguments()[1];
                    if (branchCnec.getId().contains("cnec1")) {
                        return switch (linearGlsk.getId().substring(0, EICode.EIC_LENGTH)) {
                            case "10YFR-RTE------C", "22Y201903144---9" -> 0.1;
                            case "10YBE----------2" -> 0.2;
                            case "10YCB-GERMANY--8" -> 0.3;
                            case "22Y201903145---4" -> 0.4;
                            default -> 0.;
                        };
                    } else if (branchCnec.getId().contains("cnec2")) {
                        return switch (linearGlsk.getId().substring(0, EICode.EIC_LENGTH)) {
                            case "10YFR-RTE------C", "10YBE----------2" -> 0.3;
                            case "10YCB-GERMANY--8" -> 0.2;
                            case "22Y201903145---4" -> 0.1;
                            case "22Y201903144---9" -> 0.9;
                            default -> 0.;
                        };
                    } else {
                        return 0.;
                    }
                });
    }

    @Test
    void testComputation() {

        // prepare data
        Network network = NetworkImportsUtil.import12NodesNetwork();
        ZonalData<SensitivityVariableSet> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk/glsk_proportional_12nodes_with_alegro.xml"))
                .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));
        Crac crac = CommonCracCreation.create(Set.of(TwoSides.ONE, TwoSides.TWO));
        List<ZoneToZonePtdfDefinition> boundaries = Arrays.asList(
                new ZoneToZonePtdfDefinition("{FR}-{BE}"),
                new ZoneToZonePtdfDefinition("{FR}-{DE}"),
                new ZoneToZonePtdfDefinition("{DE}-{BE}"),
                new ZoneToZonePtdfDefinition("{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}"));

        // compute zToz PTDF sum
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = new AbsolutePtdfSumsComputation(glskProvider, boundaries);
        Map<FlowCnec, Map<TwoSides, Double>> ptdfSums = absolutePtdfSumsComputation.computeAbsolutePtdfSums(crac.getFlowCnecs(), systematicSensitivityResult);

        // test results
        assertEquals(0.6, ptdfSums.get(crac.getFlowCnec("cnec1basecase")).get(TwoSides.ONE), DOUBLE_TOLERANCE); // abs(0.1 - 0.2) + abs(0.1 - 0.3) + abs(0.3 - 0.2) + abs(0.2 - 0.1 - 0.3 + 0.4) = 0.1 + 0.2 + 0.1 + 0.2
        assertEquals(0.9, ptdfSums.get(crac.getFlowCnec("cnec2basecase")).get(TwoSides.TWO), DOUBLE_TOLERANCE); // abs(0.3 - 0.3) + abs(0.3 - 0.2) + abs(0.2 - 0.3) + abs(0.3 - 0.9 - 0.2 + 0.1) = 0 + 0.1 + 0.1 + 0.7
    }

    @Test
    void testIgnoreZtoZWithLessThan2ZtoS() {

        // prepare data
        Network network = NetworkImportsUtil.import12NodesNetwork();
        ZonalData<SensitivityVariableSet> glskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/glsk/glsk_proportional_12nodes_with_alegro.xml"))
                .getZonalGlsks(network, Instant.parse("2016-07-28T22:30:00Z"));
        Crac crac = CommonCracCreation.create(Set.of(TwoSides.ONE, TwoSides.TWO));
        List<ZoneToZonePtdfDefinition> boundaries = Arrays.asList(
                new ZoneToZonePtdfDefinition("{FR}-{BE}"),
                new ZoneToZonePtdfDefinition("{FR}-{DE}"),
                new ZoneToZonePtdfDefinition("{DE}-{BE}"),
                new ZoneToZonePtdfDefinition("{BE}-{22Y201903144---0}-{DE}+{22Y201903144---1}"), // wrong EIC for Alegro, only {BE}-{DE} will be taken into account
                new ZoneToZonePtdfDefinition("{FR}-{ES}"), // ES doesn't exist in GLSK map, must be filtered
                new ZoneToZonePtdfDefinition("{ES}-{DE}")); // ES doesn't exist in GLSK map, must be filtered

        // compute zToz PTDF sum
        AbsolutePtdfSumsComputation absolutePtdfSumsComputation = new AbsolutePtdfSumsComputation(glskProvider, boundaries);
        Map<FlowCnec, Map<TwoSides, Double>> ptdfSums = absolutePtdfSumsComputation.computeAbsolutePtdfSums(crac.getFlowCnecs(), systematicSensitivityResult);

        // Test that these 3 new boundaries are ignored (results should be the same as previous test)
        assertEquals(0.5, ptdfSums.get(crac.getFlowCnec("cnec1basecase")).get(TwoSides.TWO), DOUBLE_TOLERANCE); // abs(0.1 - 0.2) + abs(0.1 - 0.3) + abs(0.3 - 0.2) + abs(0.2 - 0.3) = 0.1 + 0.2 + 0.1 + 0.1
        assertEquals(0.3, ptdfSums.get(crac.getFlowCnec("cnec2basecase")).get(TwoSides.ONE), DOUBLE_TOLERANCE); // abs(0.3 - 0.3) + abs(0.3 - 0.2) + abs(0.2 - 0.3) + abs(0.3 - 0.2) = 0 + 0.1 + 0.1 + 0.1
    }
}
