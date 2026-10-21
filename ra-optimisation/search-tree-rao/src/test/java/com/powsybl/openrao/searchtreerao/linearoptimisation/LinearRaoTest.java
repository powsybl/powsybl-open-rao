/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.data.timecoupledconstraints.GeneratorConstraints;
import com.powsybl.openrao.data.timecoupledconstraints.TimeCoupledConstraints;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end tests of {@link LinearRao}. They duplicate the CASTOR and MARMOT end-to-end tests that rely on CRACs
 * without network actions and check that the {@link RaoResult} is the same as the one of the original algorithm.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class LinearRaoTest {

    // ----- Tests coming from CastorFullOptimizationTest -----

    private static RaoInput buildRaoInput(String networkFile, String cracFile) throws IOException {
        Network network = Network.read(networkFile, LinearRaoTest.class.getResourceAsStream("/network/" + networkFile));
        Crac crac = Crac.read(cracFile, LinearRaoTest.class.getResourceAsStream("/crac/" + cracFile), network);
        return RaoInput.build(network, crac).build();
    }

    private static RaoParameters readParameters(String parametersFile) throws IOException {
        return JsonRaoParameters.read(LinearRaoTest.class.getResourceAsStream("/parameters/" + parametersFile), ReportNode.NO_OP);
    }

    @Test
    void testRaoWithEmptyCrac() throws IOException {
        RaoInput raoInput = buildRaoInput("4Nodes.uct", "empty-crac.json");
        RaoParameters raoParameters = readParameters("RaoParameters_2P_v2.json");

        RaoResult raoResult = new LinearRao().run(raoInput, raoParameters, ReportNode.NO_OP).join();

        assertNotNull(raoResult);
        // When no cnec is present, a default value of -1e9 is returned
        assertEquals(-1e9, raoResult.getCost(null));
    }

    @Test
    void curativeStopCriterionReachedSkipsPerimeterBuilding() throws IOException {
        RaoInput raoInput = buildRaoInput("small-network-2P.uct", "small-crac-purely-virtual-curative.json");
        Crac crac = raoInput.getCrac();
        RaoParameters raoParameters = readParameters("RaoParameters_secure.json");
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(true);

        RaoResult raoResult = new LinearRao().run(raoInput, raoParameters, ReportNode.NO_OP).join();

        // differs from CastorFullOptimization because the PST is also optimized in curative
        assertEquals(-13, raoResult.getOptimizedTapOnState(crac.getState("N-1 NL1-NL3", crac.getLastInstant()), crac.getPstRangeAction("CRA_PST_BE")));
    }

    // ----- Tests coming from MarmotTest -----

    private static Network readNetwork(String networkFile) throws IOException {
        return Network.read(networkFile, LinearRaoTest.class.getResourceAsStream("/network/" + networkFile));
    }

    private static Crac readCrac(String cracFile, Network network) throws IOException {
        return Crac.read(cracFile, LinearRaoTest.class.getResourceAsStream("/crac/" + cracFile), network);
    }

    @Test
    void testTwoTimestampsAndGradientOnGeneratorWithNoAssociatedRemedialAction() throws IOException {
        Crac crac1 = readCrac("crac-20250213.json", readNetwork("2Nodes2ParallelLinesPST_1000MW.uct"));
        Crac crac2 = readCrac("crac-20250214.json", readNetwork("2Nodes2ParallelLinesPST_1000MW.uct"));
        RaoParameters raoParameters = readParameters("RaoParameters_dc_minObjective_discretePst.json");
        // networks are read from the CRACs' own networks, so rebuild the inputs from fresh networks
        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 13, 11, 35, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 35, 0, 0, ZoneOffset.UTC);
        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>(Map.of(
            timestamp1, RaoInput.build(readNetwork("2Nodes2ParallelLinesPST_1000MW.uct"), crac1).build(),
            timestamp2, RaoInput.build(readNetwork("2Nodes2ParallelLinesPST_1000MW.uct"), crac2).build()
        ));
        TimeCoupledConstraints timeCoupledConstraints = new TimeCoupledConstraints();
        timeCoupledConstraints.addGeneratorConstraints(
            GeneratorConstraints.create()
                .withGeneratorId("FFR1AA1 _generator")
                .withLeadTime(0.0).withLagTime(0.0)
                .withUpwardPowerGradient(1000.0).withDownwardPowerGradient(-1000.0)
                .build()
        );

        TimeCoupledRaoResult results = new LinearRao().run(new TimeCoupledRaoInput(raoInputs, timeCoupledConstraints), raoParameters, ReportNode.NO_OP).join();

        assertEquals(-5, results.getOptimizedTapOnState(crac1.getPreventiveState(), crac1.getPstRangeAction("pstBeFr2")));
        assertEquals(-5, results.getOptimizedTapOnState(crac2.getPreventiveState(), crac2.getPstRangeAction("pstBeFr2")));
        assertEquals(110., results.getGlobalCost(crac1.getLastInstant()));
        assertEquals(55., results.getCost(crac1.getLastInstant(), timestamp1));
        assertEquals(55., results.getCost(crac2.getLastInstant(), timestamp2));
    }

    private record RedispatchingCase(Crac crac1, Crac crac2, Crac crac3, TimeCoupledRaoInput input) {
    }

    private static final OffsetDateTime TIMESTAMP_1 = OffsetDateTime.of(2025, 2, 14, 10, 40, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TIMESTAMP_2 = OffsetDateTime.of(2025, 2, 14, 11, 40, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TIMESTAMP_3 = OffsetDateTime.of(2025, 2, 14, 12, 40, 0, 0, ZoneOffset.UTC);

    private static RedispatchingCase buildRedispatchingCase(TimeCoupledConstraints timeCoupledConstraints) throws IOException {
        Network network1 = readNetwork("3Nodes.uct");
        Network network2 = readNetwork("3Nodes.uct");
        Network network3 = readNetwork("3Nodes.uct");
        Crac crac1 = readCrac("crac-redispatching-202502141040.json", network1);
        Crac crac2 = readCrac("crac-redispatching-202502141140.json", network2);
        Crac crac3 = readCrac("crac-redispatching-202502141240.json", network3);
        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>(Map.of(
            TIMESTAMP_1, RaoInput.build(network1, crac1).build(),
            TIMESTAMP_2, RaoInput.build(network2, crac2).build(),
            TIMESTAMP_3, RaoInput.build(network3, crac3).build()
        ));
        return new RedispatchingCase(crac1, crac2, crac3, new TimeCoupledRaoInput(raoInputs, timeCoupledConstraints));
    }

    private static TimeCoupledConstraints generatorConstraints(String generatorId, double upwardGradient, double downwardGradient) {
        TimeCoupledConstraints timeCoupledConstraints = new TimeCoupledConstraints();
        timeCoupledConstraints.addGeneratorConstraints(
            GeneratorConstraints.create()
                .withGeneratorId(generatorId)
                .withLeadTime(0.0).withLagTime(0.0)
                .withUpwardPowerGradient(upwardGradient).withDownwardPowerGradient(downwardGradient)
                .build()
        );
        return timeCoupledConstraints;
    }

    private static void assertRedispatching(RedispatchingCase testCase, TimeCoupledRaoResult results, double[] setPoints, double globalCost, double[] costs) {
        Crac[] cracs = {testCase.crac1(), testCase.crac2(), testCase.crac3()};
        OffsetDateTime[] timestamps = {TIMESTAMP_1, TIMESTAMP_2, TIMESTAMP_3};
        for (int i = 0; i < 3; i++) {
            assertEquals(setPoints[i], results.getOptimizedSetPointOnState(cracs[i].getPreventiveState(), cracs[i].getRangeAction("redispatchingAction")));
        }
        assertEquals(globalCost, results.getGlobalCost(testCase.crac1().getLastInstant()));
        for (int i = 0; i < 3; i++) {
            assertEquals(costs[i], results.getCost(cracs[i].getLastInstant(), timestamps[i]));
        }
    }

    @Test
    void testWithRedispatchingAndNoGradientOnImplicatedGenerators() throws IOException {
        RaoParameters raoParameters = readParameters("RaoParameters_minCost_megawatt_dc.json");
        RedispatchingCase testCase = buildRedispatchingCase(generatorConstraints("FFR1AA1 _generator", 250.0, -250.0));

        TimeCoupledRaoResult results = new LinearRao().run(testCase.input(), raoParameters, ReportNode.NO_OP).join();

        assertRedispatching(testCase, results, new double[] {-0.0, 530.0, 530.0}, 53020., new double[] {0., 26510., 26510.});
        // initial set points
        for (Crac crac : new Crac[] {testCase.crac1(), testCase.crac2(), testCase.crac3()}) {
            assertEquals(-0.0, results.getPreOptimizationSetPointOnState(crac.getPreventiveState(), crac.getRangeAction("redispatchingAction")));
        }
    }

    @Test
    void testWithRedispatchingAndNoGradients() throws IOException {
        RaoParameters raoParameters = readParameters("RaoParameters_minCost_megawatt_dc.json");
        RedispatchingCase testCase = buildRedispatchingCase(new TimeCoupledConstraints());

        TimeCoupledRaoResult results = new LinearRao().run(testCase.input(), raoParameters, ReportNode.NO_OP).join();

        assertRedispatching(testCase, results, new double[] {-0.0, 530.0, 530.0}, 53020., new double[] {0., 26510., 26510.});
    }

    @Test
    void testWithRedispatchingAndGradientOnImplicatedGenerators() throws IOException {
        RaoParameters raoParameters = readParameters("RaoParameters_minCost_megawatt_dc.json");
        RedispatchingCase testCase = buildRedispatchingCase(generatorConstraints("FFR3AA1 _generator", 200.0, 0.0));

        // 330 MW are activated in the first timestamp: it is the minimum necessary to be able to activate 530 MW in
        // the second one due to the maximum gradient of 200 MW/h
        TimeCoupledRaoResult results = new LinearRao().run(testCase.input(), raoParameters, ReportNode.NO_OP).join();

        assertRedispatching(testCase, results, new double[] {330.0, 530.0, 530.0}, 69530., new double[] {16510., 26510., 26510.});
    }
}
