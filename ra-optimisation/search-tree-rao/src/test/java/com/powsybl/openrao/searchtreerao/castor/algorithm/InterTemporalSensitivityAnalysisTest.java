/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterTemporalSensitivityAnalysisTest {
    private Crac crac1;
    private Crac crac2;
    private Crac crac3;
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2024, 12, 10, 16, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2024, 12, 10, 17, 21, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2024, 12, 10, 18, 21, 0, 0, ZoneOffset.UTC);
    private InterTemporalSensitivityAnalysis sensitivityAnalysis;

    @BeforeEach
    void setUp() throws IOException {
        Network network = Network.read("12Nodes_2_pst.uct", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/network/12Nodes_2_pst.uct"));

        crac1 = Crac.read("small-crac-2pst-1600.json", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/crac/small-crac-2pst-1600.json"), network);
        crac2 = Crac.read("small-crac-2pst-1700.json", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/crac/small-crac-2pst-1700.json"), network);
        crac3 = Crac.read("small-crac-2pst-1800.json", InterTemporalSensitivityAnalysisTest.class.getResourceAsStream("/crac/small-crac-2pst-1800.json"), network);

        RaoInput raoInput1 = RaoInput.build(network, crac1).build();
        RaoInput raoInput2 = RaoInput.build(network, crac2).build();
        RaoInput raoInput3 = RaoInput.build(network, crac3).build();

        InterTemporalRaoInput input = new InterTemporalRaoInput(new TemporalDataImpl<>(Map.of(timestamp1, raoInput1, timestamp2, raoInput2, timestamp3, raoInput3)));
        RaoParameters parameters = new RaoParameters();

        sensitivityAnalysis = new InterTemporalSensitivityAnalysis(input, parameters);
    }

    @Test
    void getRangeActionsPerTimestamp() {
        assertEquals(
                Map.of(timestamp1, Set.of(crac1.getRangeAction("pstBe - 1600")),
                        timestamp2, Set.of(crac1.getRangeAction("pstBe - 1600"), crac2.getRangeAction("pstBe - 1700")),
                        timestamp3, Set.of(crac1.getRangeAction("pstBe - 1600"), crac2.getRangeAction("pstBe - 1700"), crac3.getRangeAction("pstBe - 1800"), crac3.getRangeAction("pstDe - 1800"))),
                sensitivityAnalysis.getRangeActionsPerTimestamp());

    }

    @Test
    void getFlowCnecsPerTimestamp() {
        assertEquals(
                Map.of(timestamp1, Set.of(crac1.getFlowCnec("cnecDeNlPrev - 1600"), crac1.getFlowCnec("cnecDeNlOut - 1600")),
                        timestamp2, Set.of(crac2.getFlowCnec("cnecDeNlPrev - 1700"), crac2.getFlowCnec("cnecDeNlOut - 1700")),
                        timestamp3, Set.of(crac3.getFlowCnec("cnecDeNlPrev - 1800"), crac3.getFlowCnec("cnecDeNlOut - 1800"))),
                sensitivityAnalysis.getFlowCnecsPerTimestamp());

    }

    @Test
    void testRunInitialSensitivityAnalysis(){
        TemporalData<Pair<FlowResult, SensitivityResult>> result = sensitivityAnalysis.runInitialSensitivityAnalysis();
        assertEquals(3, result.getDataPerTimestamp().size());
        // TODO: Check flows and sensis
    }
}
