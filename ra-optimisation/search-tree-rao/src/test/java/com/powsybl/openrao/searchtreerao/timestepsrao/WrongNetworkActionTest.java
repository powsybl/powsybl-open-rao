package com.powsybl.openrao.searchtreerao.timestepsrao;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class WrongNetworkActionTest {
    List<Network> networks;
    List<Crac> cracs;
    RaoParameters raoParameters;
    List<RaoInput> raoInputsList;

    @BeforeEach
    void init() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-wrong-network-action-0.json",
            "multi-ts/crac/crac-wrong-network-action-1.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesWrongNetworkAction.uct",
            "multi-ts/network/12NodesWrongNetworkAction.uct"
        );
        cracs = new ArrayList<>();
        networks = new ArrayList<>();
        raoInputsList = new ArrayList<>();

        for (int i = 0; i < networksPaths.size(); i++) {
            Network network = Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i)));
            networks.add(network);
            Crac crac = CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), network);
            cracs.add(crac);
            raoInputsList.add(RaoInput.build(network, crac).build());
        }
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC_SCIP.json"));
    }

    @Test
    void raoWithNetworkActions() {
        LinearOptimizationResult raoResult = TimeStepsRao.launchMultiRao(raoInputsList, raoParameters);
        RangeAction<?> rangeActionTs0 = cracs.get(0).getRangeAction("pst_be - TS0");
        RangeAction<?> rangeActionTs1 = cracs.get(1).getRangeAction("pst_be - TS1");
        State state0 = cracs.get(0).getPreventiveState();
        State state1 = cracs.get(1).getPreventiveState();
        System.out.println(raoResult.getRangeActionActivationResult().getOptimizedSetpoint(rangeActionTs0, state0));
        System.out.println(raoResult.getRangeActionActivationResult().getOptimizedSetpoint(rangeActionTs1, state1));
    }

    @Test
    void raoNoWrongNetworkActions() {
        raoInputsList.get(0).getCrac().removeNetworkAction("open-de2-nl3-1 - TS0");
        raoInputsList.get(1).getCrac().removeNetworkAction("open-de2-nl3-1 - TS1");
        LinearOptimizationResult raoResult = TimeStepsRao.launchMultiRao(raoInputsList, raoParameters);
        RangeAction<?> rangeActionTs0 = cracs.get(0).getRangeAction("pst_be - TS0");
        RangeAction<?> rangeActionTs1 = cracs.get(1).getRangeAction("pst_be - TS1");
        State state0 = cracs.get(0).getPreventiveState();
        State state1 = cracs.get(1).getPreventiveState();
        System.out.println(raoResult.getRangeActionActivationResult().getOptimizedSetpoint(rangeActionTs0, state0));
        System.out.println(raoResult.getRangeActionActivationResult().getOptimizedSetpoint(rangeActionTs1, state1));
    }

    @Test
    void raoFirstTimeStep() {
        runRaoAlone(0);
    }

    @Test
    void raoSecondTimeStep() {
        runRaoAlone(1);
    }

    private void runRaoAlone(int timeStep) {
        RaoResult result = Rao.find("SearchTreeRao").run(raoInputsList.get(timeStep), raoParameters);
        RangeAction<?> rangeAction = cracs.get(timeStep).getRangeAction("pst_be - TS" + timeStep);
        double setPoint = result.getOptimizedSetPointsOnState(raoInputsList.get(timeStep).getCrac().getPreventiveState()).get(rangeAction);
        System.out.println(setPoint);
        System.out.println(result.getActivatedNetworkActionsDuringState(raoInputsList.get(timeStep).getCrac().getPreventiveState()));
    }
}
