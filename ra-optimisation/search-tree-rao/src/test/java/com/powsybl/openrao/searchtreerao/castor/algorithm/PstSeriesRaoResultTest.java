package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PstSeriesRaoResultTest {

    @Test
    void testUnchanged() throws IOException, ExecutionException, InterruptedException {
        Network network = Network.read("2Nodes3ParallelLinesPST.uct", getClass().getResourceAsStream("/network/2Nodes3ParallelLinesPST.uct"));
        Crac crac = Crac.read("/crac/crac-force-ra.json", PstSeriesRaoResultTest.class.getResourceAsStream("/crac/crac-force-ra.json"), network);
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

//        RaoResult result = Rao.run(RaoInput.build(network, crac).build(), parameters);
        RaoResult result = RaoResult.read(getClass().getResourceAsStream("/result/rao-result-pst-series-unchanged.json"), crac);
        //
        PstSeriesRaoResult pstSeriesRaoResult = new PstSeriesRaoResult(RaoInput.build(network, crac).build(), parameters, result);
        RaoResult forcedResult = pstSeriesRaoResult.run(Map.of()).get();

        Set<RangeAction<?>> rangeActions = forcedResult.getActivatedRangeActionsDuringState(crac.getPreventiveState());
        assertEquals(1, rangeActions.size());
        RangeAction<?> rangeAction = rangeActions.iterator().next();
        assertEquals("pst", rangeAction.getId());
        assertEquals(-10, forcedResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction("pst")));
        assertEquals(6.15, forcedResult.getFlow(crac.getPreventiveInstant(), crac.getFlowCnec("cnecPreventive"), TwoSides.ONE, Unit.MEGAWATT), .1);
        assertEquals(-10, forcedResult.getOptimizedTapOnState(crac.getState("contingency", crac.getLastInstant()), crac.getPstRangeAction("pst")));
        assertEquals(9.23, forcedResult.getFlow(crac.getLastInstant(), crac.getFlowCnec("cnecCurative"), TwoSides.ONE, Unit.MEGAWATT), .1);
    }

    @Test
    void testForcedRas() throws IOException, ExecutionException, InterruptedException {
        Network network = Network.read("2Nodes3ParallelLinesPST.uct", getClass().getResourceAsStream("/network/2Nodes3ParallelLinesPST.uct"));
        Crac crac = Crac.read("/crac/crac-force-ra.json", PstSeriesRaoResultTest.class.getResourceAsStream("/crac/crac-force-ra.json"), network);
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult result = RaoResult.read(getClass().getResourceAsStream("/result/rao-result-pst-series-unchanged.json"), crac);
        //
        PstSeriesRaoResult pstSeriesRaoResult = new PstSeriesRaoResult(RaoInput.build(network, crac).build(), parameters, result);
        //
        State curativeState = crac.getState("contingency", crac.getLastInstant());
        PstRangeAction pstRangeAction = crac.getPstRangeAction("pst");
        double preventiveSetPoint = -2.7267643331050597;
        double curativeSetPoint = -3.505407871356285;
        Map<State, Map<RangeAction<?>, Double>> forcedSetPointsByState = Map.of(crac.getPreventiveState(), Map.of(pstRangeAction, preventiveSetPoint),
                curativeState, Map.of(pstRangeAction, curativeSetPoint));
        //
        RaoResult forcedResult = pstSeriesRaoResult.run(forcedSetPointsByState).get();

        Set<RangeAction<?>> rangeActions = forcedResult.getActivatedRangeActionsDuringState(crac.getPreventiveState());
        assertEquals(1, rangeActions.size());
        RangeAction<?> rangeAction = rangeActions.iterator().next();
        assertEquals("pst", rangeAction.getId());
        assertEquals(-7, forcedResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction("pst")));
        assertEquals(104.26, forcedResult.getFlow(crac.getPreventiveInstant(), crac.getFlowCnec("cnecPreventive"), TwoSides.ONE, Unit.MEGAWATT), .1);
        assertEquals(-9, forcedResult.getOptimizedTapOnState(crac.getState("contingency", crac.getLastInstant()), crac.getPstRangeAction("pst")));
        assertEquals(58.27, forcedResult.getFlow(crac.getLastInstant(), crac.getFlowCnec("cnecCurative"), TwoSides.ONE, Unit.MEGAWATT), .1);
    }

/*    @Test
    void testForcePstRa() throws IOException {
        Network network = Network.read("2Nodes3ParallelLinesPST.uct", getClass().getResourceAsStream("/network/2Nodes3ParallelLinesPST.uct"));
        Crac crac = Crac.read("/crac/crac-force-ra.json", PstSeriesRaoResultTest.class.getResourceAsStream("/crac/crac-force-ra.json"), network);
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC.json"));

        RaoResult result = Rao.run(RaoInput.build(network, crac).build(), parameters);

        Map<State, Map<RangeAction<?>, Double>> forcedSetPointsByState = new HashMap<>();
        Map<RangeAction<?>, Double> forcedSetPointsByRangeAction = new HashMap<>();
        RangeAction<?> ra = new PstRangeActionImpl("id",
                "name",
                "operator",
                Set.of(),
                List.of(new TapRangeImplementation(-16, 16, RangeType.RELATIVE_TO_INITIAL_NETWORK)),
                null,
                "groupId",
                0,
                Map.of(),
                10,
                0,
                Map.of());
        forcedSetPointsByRangeAction.put(ra, 5.);

    }

    class TapRangeImplementation extends AbstractRange implements TapRange {

        private final int minTap;
        private final int maxTap;
        TapRangeImplementation(int minTap, int maxTap, RangeType rangeType) {
            super(rangeType, Unit.TAP);
            this.minTap = minTap;
            this.maxTap = maxTap;
        }

        @Override
        public int getMinTap() {
            return -16;
        }

        @Override
        public int getMaxTap() {
            return 16;
        }

        @Override
        public RangeType getRangeType() {
            return null;
        }

        @Override
        public Unit getUnit() {
            return null;
        }
    }*/
}