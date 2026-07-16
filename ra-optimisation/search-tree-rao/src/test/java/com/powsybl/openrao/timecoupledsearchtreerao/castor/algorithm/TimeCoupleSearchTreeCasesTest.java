/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.timecoupledsearchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
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

class TimeCoupleSearchTreeCasesTest {
    private static final String NETWORK = "/network/TestCase12Nodes2PSTs.uct";
    private static final String NETWORK_PARALLEL_LINES = "/network/TestCase12Nodes2PSTsWithParallelLines.uct";

    private static final OffsetDateTime TS1 = OffsetDateTime.of(2026, 3, 25, 10, 30, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TS2 = OffsetDateTime.of(2026, 3, 25, 11, 30, 0, 0, ZoneOffset.UTC);

    /**
     * Range only test : a single pst shared by both timestamps, no topological action.
     *
     * <p>independently, the two timestamps drive the shared PST pst_be in opposite directions. Timestamp 1 needs it on
     * the positive side (+6) to relieve BE1-BE3, while timestamp 2 needs it on the negative side (-16) to relieve BE1-BE2.</p>
     *
     * <p>in time-coupled, the setpoint synchronization filler forces pst_be to the same tap on both timestamps,
     * so the two opposite optima are no longer both reachable. The MIP settles on a single compromise tap at -16.
     * Initial cost in this case was : 510 = 459.21 + 50.79 and final cost after activation of both PSTs is 460.17.
     * </p>
     * */
    @Test
    void testWithRangeActionsOnly() throws IOException {
        Network network1 = Network.read(NETWORK, getClass().getResourceAsStream(NETWORK));
        Network network2 = Network.read(NETWORK, getClass().getResourceAsStream(NETWORK));
        // 1 common contingency, 2 curative CNECs, pst_be & pst_de
        Crac crac1 = Crac.read("crac_20260325_1030_pst_only.json", getClass().getResourceAsStream("/crac/crac_20260325_1030_pst_only.json"), network1);
        // 1 curative CNEC, pst_be only
        Crac crac2 = Crac.read("crac_20260325_1130_pst_only.json", getClass().getResourceAsStream("/crac/crac_20260325_1130_pst_only.json"), network2);
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_default.json"), ReportNode.NO_OP);

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>(Map.of(TS1, RaoInput.build(network1, crac1).build(), TS2, RaoInput.build(network2, crac2).build()));
        TimeCoupledRaoInput timeCoupledRaoInput = new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints());

        // all the PST taps are set to -16.
        new TimeCoupledCurativeOptimization().run(timeCoupledRaoInput, raoParameters, ReportNode.NO_OP);
    }

    /**
     * Topology union test: the union of the available network actions is applied
     *
     * <p>Both timestamps share the same 2 curative topological actions and the same two curative CNECs, only
     * the CNEC thresholds differ. No range action.</p>
     *
     * <p>independently, timestamp 1 is secured by close_de1de3_2 alone and timestamp 2 by open_be2fr3 alone</p>
     *
     * In the time-coupled search tree, both close_de1de3_2 and open_be2fr3 are applied simultaneously.
     */
    @Test
    void testTheUnionOfTheTopologicalActionsIsApplied() throws IOException {
        Network network1 = Network.read(NETWORK_PARALLEL_LINES, getClass().getResourceAsStream(NETWORK_PARALLEL_LINES));
        Network network2 = Network.read(NETWORK_PARALLEL_LINES, getClass().getResourceAsStream(NETWORK_PARALLEL_LINES));

        Crac crac1 = Crac.read("crac_20260325_1030_topological_only_union.json", getClass().getResourceAsStream("/crac/crac_20260325_1030_topological_only_union.json"), network1);
        Crac crac2 = Crac.read("crac_20260325_1130_topological_only_union.json", getClass().getResourceAsStream("/crac/crac_20260325_1130_topological_only_union.json"), network2);

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_default.json"), ReportNode.NO_OP);

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>(Map.of(TS1, RaoInput.build(network1, crac1).build(), TS2, RaoInput.build(network2, crac2).build()));
        TimeCoupledRaoInput timeCoupledRaoInput = new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints());

        new TimeCoupledCurativeOptimization().run(timeCoupledRaoInput, raoParameters, ReportNode.NO_OP);
    }

    /**
     * One action secures both, but it is not what each timestamp picks alone.
     * <p>Same actions and cnecs as the previous the only change is timestamp 1's DE1-DE3 limit,
     * changed to 550 MW.
     *
     * <p>Independently, timestamp 1 still prefers close_de1de3_2a nd timestamp 2 needs open_be2fr3.</p>
     *
     * In time-coupled close_de1de3_2 cannot secure timestamp 2, so
     * the search tree applies open_be2fr3 alone on both timestamps, even though it is not timestamp 1's own preference.
     */
    @Test
    void testOnlyOneOfTheTopologicalActionsIsApplied() throws IOException {
        Network network1 = Network.read(NETWORK_PARALLEL_LINES, getClass().getResourceAsStream(NETWORK_PARALLEL_LINES));
        Network network2 = Network.read(NETWORK_PARALLEL_LINES, getClass().getResourceAsStream(NETWORK_PARALLEL_LINES));

        Crac crac1 = Crac.read("crac_20260325_1030_topological_only.json", getClass().getResourceAsStream("/crac/crac_20260325_1030_topological_only.json"), network1);
        Crac crac2 = Crac.read("crac_20260325_1130_topological_only.json", getClass().getResourceAsStream("/crac/crac_20260325_1130_topological_only.json"), network2);

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_default.json"), ReportNode.NO_OP);

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>(Map.of(TS1, RaoInput.build(network1, crac1).build(), TS2, RaoInput.build(network2, crac2).build()));
        TimeCoupledRaoInput timeCoupledRaoInput = new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints());

        new TimeCoupledCurativeOptimization().run(timeCoupledRaoInput, raoParameters, ReportNode.NO_OP).join();
    }

    /**
     * Combined range + topology
     */
    @Test
    void testBothRangeAndTopologicalActionsAreSynchronized() throws IOException {
        Network network1 = Network.read(NETWORK_PARALLEL_LINES, getClass().getResourceAsStream(NETWORK_PARALLEL_LINES));
        Network network2 = Network.read(NETWORK_PARALLEL_LINES, getClass().getResourceAsStream(NETWORK_PARALLEL_LINES));

        Crac crac1 = Crac.read("crac_20260325_1030.json", getClass().getResourceAsStream("/crac/crac_20260325_1030.json"), network1);
        Crac crac2 = Crac.read("crac_20260325_1130.json", getClass().getResourceAsStream("/crac/crac_20260325_1130.json"), network2);

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_default.json"), ReportNode.NO_OP);

        TemporalData<RaoInput> raoInputs = new TemporalDataImpl<>(Map.of(TS1, RaoInput.build(network1, crac1).build(), TS2, RaoInput.build(network2, crac2).build()));
        TimeCoupledRaoInput timeCoupledRaoInput = new TimeCoupledRaoInput(raoInputs, new TimeCoupledConstraints());

        new TimeCoupledCurativeOptimization().run(timeCoupledRaoInput, raoParameters, ReportNode.NO_OP).join();
    }
}
