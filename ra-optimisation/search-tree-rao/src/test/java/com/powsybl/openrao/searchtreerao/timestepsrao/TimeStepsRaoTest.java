/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.timestepsrao;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class TimeStepsRaoTest {
    List<Network> networks;
    List<Crac> cracs;
    RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/parameters/RaoParameters_DC_SCIP.json"));

    @Test
    void raoTwoTimeStepsPstWithNetworkActions() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-network-action-0.json",
            "multi-ts/crac/crac-network-action-1.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesProdFR.uct",
            "multi-ts/network/12NodesProdNL.uct"
        );

        cracs = new ArrayList<>();
        networks = new ArrayList<>();
        List<RaoInput> raoInputsList = new ArrayList<>();

        for (int i = 0; i < networksPaths.size(); i++) {
            Network network = Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i)));
            networks.add(network);
            Crac crac = CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), network);
            cracs.add(crac);
            raoInputsList.add(RaoInput.build(network, crac).build());
        }

        // Run RAO
        LinearOptimizationResult raoResult = TimeStepsRao.launchMultiRao(raoInputsList, raoParameters);
    }

    @Test
    void findBestTap() {
        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-network-action-0.json",
            "multi-ts/crac/crac-network-action-1.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12NodesProdFR.uct",
            "multi-ts/network/12NodesProdNL.uct"
        );

        Map<Integer, Double> marginsMap0 = computeMargins(networksPaths, 0, cracsPaths);
        Map<Integer, Double> marginsMap1 = computeMargins(networksPaths, 1, cracsPaths);

        double objFctValMax = -Double.MAX_VALUE;
        int bestTapTs0 = 0;
        int bestTapTs1 = 0;
        for (int tapTs0 = -16; tapTs0 <= 16; tapTs0++) {
            for (int tapTs1 = Math.max(-16, tapTs0 - 3); tapTs1 <= Math.min(16, tapTs0 + 3); tapTs1++) {
                double objFctVal = Math.min(marginsMap0.get(tapTs0), marginsMap1.get(tapTs1));
                if (objFctVal > objFctValMax) {
                    objFctValMax = objFctVal;
                    bestTapTs0 = tapTs0;
                    bestTapTs1 = tapTs1;
                }
            }
        }
        System.out.println(bestTapTs0);
        System.out.println(bestTapTs1);
    }

    private Map<Integer, Double> computeMargins(List<String> networksPaths, int i, List<String> cracsPaths) {
        Network network = Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i)));
        Crac crac = CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), network);

        crac.getNetworkAction("close-fr3-be2-2 - TS" + i).apply(network);
        crac.getNetworkAction("close-fr3-be2-3 - TS" + i).apply(network);
        Map<Integer, Double> marginsMap = new HashMap<>();

        for (int tap = -16; tap <= 16; tap++) {
            PstRangeAction pstRangeAction0 = crac.getPstRangeAction("pst_be - TS" + i);
            pstRangeAction0.apply(network, pstRangeAction0.convertTapToAngle(tap));
            LoadFlow.find("OpenLoadFlow").run(network, raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters());
            double p = network.getLine("BBE2AA1  FFR3AA1  1").getTerminal1().getP();
            double margin = crac.getFlowCnec("BBE2AA1  FFR3AA1  1 - preventive - TS" + i).computeMargin(p, Side.LEFT, Unit.MEGAWATT);
            marginsMap.put(tap, margin);
        }
        return marginsMap;
    }

    @Test
    void raoTwoTimeStepsInjectionWithNetworkActions() {

        List<String> cracsPaths = List.of(
            "multi-ts/crac/crac-injection-pst-ts0.json",
            "multi-ts/crac/crac-injection-pst-ts1.json"
        );
        List<String> networksPaths = List.of(
            "multi-ts/network/12Nodes3GenProdBE.uct",
            "multi-ts/network/12Nodes3GenProdBE.uct"
        );

        cracs = new ArrayList<>();
        networks = new ArrayList<>();
        List<RaoInput> raoInputsList = new ArrayList<>();

        for (int i = 0; i < networksPaths.size(); i++) {
            Network network = Network.read(networksPaths.get(i), getClass().getResourceAsStream("/" + networksPaths.get(i)));
            networks.add(network);
            Crac crac = CracImporters.importCrac(cracsPaths.get(i), getClass().getResourceAsStream("/" + cracsPaths.get(i)), network);
            cracs.add(crac);
            raoInputsList.add(RaoInput.build(network, crac).build());
        }

        // Run RAO
        LinearOptimizationResult raoResult = TimeStepsRao.launchMultiRao(raoInputsList, raoParameters);
    }
}
