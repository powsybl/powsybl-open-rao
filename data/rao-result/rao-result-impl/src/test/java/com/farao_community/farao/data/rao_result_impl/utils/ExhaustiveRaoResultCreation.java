/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl.utils;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_impl.*;

import java.util.Set;

import static com.farao_community.farao.commons.Unit.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ExhaustiveRaoResultCreation {

    private static Crac crac;

    /*
    Small CRAC used in unit tests of farao-core

    The idea of this RaoResult is to be quite exhaustive regarding the diversity of its object.
    It contains numerous object to ensure that they are all covered when testing the RaoResult
     */

    private ExhaustiveRaoResultCreation() {
    }

    public static RaoResult create(Crac crac) {
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        raoResult.setComputationStatus(ComputationStatus.DEFAULT);

        ExhaustiveRaoResultCreation.crac = crac;

        // --------------------
        // --- Cost results ---
        // --------------------

        // CostResult at initial state
        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(null);
        costResult.setFunctionalCost(100.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        // CostResult after PRA
        costResult = raoResult.getAndCreateIfAbsentCostResult(crac.getInstant(Instant.Kind.PREVENTIVE));
        costResult.setFunctionalCost(80.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        // CostResult after ARA
        costResult = raoResult.getAndCreateIfAbsentCostResult(crac.getInstant(Instant.Kind.AUTO));
        costResult.setFunctionalCost(-20.);
        costResult.setVirtualCost("loopFlow", 15.);
        costResult.setVirtualCost("MNEC", 20.);

        // CostResult after CRA
        costResult = raoResult.getAndCreateIfAbsentCostResult(crac.getInstant(Instant.Kind.CURATIVE));
        costResult.setFunctionalCost(-50.);
        costResult.setVirtualCost("loopFlow", 10.);
        costResult.setVirtualCost("MNEC", 2.);

        // ------------------------
        // --- FlowCnec results ---
        // ------------------------

        /*
         use the following logic:
         value = XXXX + YYY + ZZ + A
         with:
         - XXXX = 1000 for cnec1, 2000 for cnec2, ...
         - YYY = 000 for initial, 100 for after_pra, 200 for after_ara and 300 for after_cra
         - ZZ = 10 for MW, 20 for AMPERE, 30 for DEGREE and 40 for KILOVOLT
         - A = 0 for flow, 1 for margin, 2 for relativeMargin, 3 for loop-flows, 4 for commercial flow, 5 for angle and 6 for voltage

         Moreover:
         - only cnec 1 and cnec 2 have loop-flows and commercial flows (in practice, only cross-border CNECs)
         - pure MNEC does not have a relative margin or a PTDF sum

         Note that there is no consistency within the FlowCnecResults, and between the FlowCnecResults
         and the CostResults
         */

        for (FlowCnec cnec : crac.getFlowCnecs()) {
            FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(cnec);
            fillFlowCnecResult(flowCnecResult, cnec);
        }

        for (AngleCnec cnec : crac.getAngleCnecs()) {
            AngleCnecResult angleCnecResult = raoResult.getAndCreateIfAbsentAngleCnecResult(cnec);
            fillAngleCnecResult(angleCnecResult, cnec);
        }

        for (VoltageCnec cnec : crac.getVoltageCnecs()) {
            VoltageCnecResult voltageCnecResult = raoResult.getAndCreateIfAbsentVoltageCnecResult(cnec);
            fillVoltageCnecResult(voltageCnecResult, cnec);
        }

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        for (NetworkAction networkAction : crac.getNetworkActions()) {
            NetworkActionResult nar = raoResult.getAndCreateIfAbsentNetworkActionResult(networkAction);

            switch (networkAction.getId()) {
                case "complexNetworkActionId":
                    // free to use preventive, activated
                    nar.addActivationForState(crac.getPreventiveState());
                    break;
                case "injectionSetpointRaId" :
                    // automaton, activated
                    nar.addActivationForState(crac.getState("contingency2Id", crac.getInstant(Instant.Kind.AUTO)));
                    break;
                case "pstSetpointRaId" :
                    // forced in curative, activated
                    nar.addActivationForState(crac.getState("contingency1Id", crac.getInstant(Instant.Kind.CURATIVE)));
                    break;
                case "switchPairRaId" :
                    // available in curative, not activated
                    break;
                default:
                    // do nothing
            }
        }

        // ------------------------------
        // --- PstRangeAction results ---
        // ------------------------------

        for (PstRangeAction pstRangeAction : crac.getPstRangeActions()) {
            RangeActionResult prar = (RangeActionResult) raoResult.getAndCreateIfAbsentRangeActionResult(pstRangeAction);

            switch (pstRangeAction.getId()) {
                case "pstRange1Id":
                    // free to use preventive, activated
                    prar.setInitialSetpoint(0);
                    prar.addActivationForState(crac.getPreventiveState(), 3.0);
                    break;
                case "pstRange2Id":
                    // on flow in preventive state, not activated
                    prar.setInitialSetpoint(1.5);
                    break;
                case "pstRange3Id":
                    // on angle in curative state, not activated
                    prar.setInitialSetpoint(1.0);
                    break;
                default:
                    // do nothing
            }
        }

        // -------------------------------
        // --- HvdcRangeAction results ---
        // -------------------------------

        for (HvdcRangeAction hvdcRangeAction : crac.getHvdcRangeActions()) {
            RangeActionResult hrar = raoResult.getAndCreateIfAbsentRangeActionResult(hvdcRangeAction);

            switch (hvdcRangeAction.getId()) {
                case "hvdcRange1Id":
                    // free to use preventive, activated
                    hrar.setInitialSetpoint(0);
                    hrar.addActivationForState(crac.getPreventiveState(), -1000);
                    break;
                case "hvdcRange2Id":
                    // activated for two curative states
                    hrar.setInitialSetpoint(-100);
                    hrar.addActivationForState(crac.getState("contingency1Id", crac.getInstant(Instant.Kind.CURATIVE)), 100);
                    hrar.addActivationForState(crac.getState("contingency2Id", crac.getInstant(Instant.Kind.CURATIVE)), 400);
                    break;
                default:
                    // do nothing
            }
        }

        // ------------------------------------
        // --- InjectionRangeAction results ---
        // ------------------------------------
        RangeActionResult irar = raoResult.getAndCreateIfAbsentRangeActionResult(crac.getInjectionRangeAction("injectionRange1Id"));
        irar.setInitialSetpoint(100);
        irar.addActivationForState(crac.getState("contingency1Id", crac.getInstant(Instant.Kind.CURATIVE)), -300);

        return raoResult;
    }

    private static void fillFlowCnecResult(FlowCnecResult flowCnecResult, FlowCnec cnec) {

        double x = Integer.parseInt(String.valueOf(cnec.getId().charAt(4))) * 1000;
        boolean hasLoopFlow = cnec.getId().startsWith("cnec1") || cnec.getId().startsWith("cnec2");
        boolean isPureMnec = cnec.isMonitored() && !cnec.isOptimized();

        ElementaryFlowCnecResult initialEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
        fillElementaryResult(initialEfcr, x, 100, hasLoopFlow, isPureMnec, cnec.getMonitoredSides());
        ElementaryFlowCnecResult afterPraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.PREVENTIVE));
        fillElementaryResult(afterPraEfcr, x, 200, hasLoopFlow, isPureMnec, cnec.getMonitoredSides());

        if (cnec.getState().getInstant().isAuto() || cnec.getState().getInstant() == crac.getInstant(Instant.Kind.CURATIVE)) {
            ElementaryFlowCnecResult afterAraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.AUTO));
            fillElementaryResult(afterAraEfcr, x, 300, hasLoopFlow, isPureMnec, cnec.getMonitoredSides());
        }
        if (cnec.getState().getInstant() == crac.getInstant(Instant.Kind.CURATIVE)) {
            ElementaryFlowCnecResult afterCraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.CURATIVE));
            fillElementaryResult(afterCraEfcr, x, 400, hasLoopFlow, isPureMnec, cnec.getMonitoredSides());
        }
    }

    private static void fillAngleCnecResult(AngleCnecResult angleCnecResult, AngleCnec cnec) {

        double x = 3000;

        ElementaryAngleCnecResult initialEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
        fillElementaryResult(initialEacr, x, 100);
        ElementaryAngleCnecResult afterPraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.PREVENTIVE));
        fillElementaryResult(afterPraEacr, x, 200);

        if (cnec.getState().getInstant().isAuto() || cnec.getState().getInstant() == crac.getInstant(Instant.Kind.CURATIVE)) {
            ElementaryAngleCnecResult afterAraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.AUTO));
            fillElementaryResult(afterAraEacr, x, 300);
        }
        if (cnec.getState().getInstant() == crac.getInstant(Instant.Kind.CURATIVE)) {
            ElementaryAngleCnecResult afterCraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.CURATIVE));
            fillElementaryResult(afterCraEacr, x, 400);
        }
    }

    private static void fillVoltageCnecResult(VoltageCnecResult voltageCnecResult, VoltageCnec cnec) {

        double x = 4000;

        ElementaryVoltageCnecResult initialEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
        fillElementaryResult(initialEacr, x, 100);
        ElementaryVoltageCnecResult afterPraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.PREVENTIVE));
        fillElementaryResult(afterPraEacr, x, 200);

        if (cnec.getState().getInstant().isAuto() || cnec.getState().getInstant() == crac.getInstant(Instant.Kind.CURATIVE)) {
            ElementaryVoltageCnecResult afterAraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.AUTO));
            fillElementaryResult(afterAraEacr, x, 300);
        }
        if (cnec.getState().getInstant() == crac.getInstant(Instant.Kind.CURATIVE)) {
            ElementaryVoltageCnecResult afterCraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant(Instant.Kind.CURATIVE));
            fillElementaryResult(afterCraEacr, x, 400);
        }
    }

    private static void fillElementaryResult(ElementaryFlowCnecResult elementaryFlowCnecResult, double x, double y, boolean hasLoopFlow, boolean isPureMnec, Set<Side> sides) {
        sides.forEach(side -> fillElementaryResult(elementaryFlowCnecResult, x, y, hasLoopFlow, isPureMnec, side));
    }

    private static void fillElementaryResult(ElementaryFlowCnecResult elementaryFlowCnecResult, double x, double y, boolean hasLoopFlow, boolean isPureMnec, Side side) {
        double perturb = side.equals(Side.LEFT) ? 0 : 0.5;

        elementaryFlowCnecResult.setFlow(side, perturb + x + y + 10, MEGAWATT);
        elementaryFlowCnecResult.setFlow(side, perturb + x + y + 20, AMPERE);

        elementaryFlowCnecResult.setMargin(x + y + 11, MEGAWATT);
        elementaryFlowCnecResult.setMargin(x + y + 21, AMPERE);

        if (!isPureMnec) {
            elementaryFlowCnecResult.setRelativeMargin(x + y + 12, MEGAWATT);
            elementaryFlowCnecResult.setRelativeMargin(x + y + 22, AMPERE);
            elementaryFlowCnecResult.setPtdfZonalSum(side, perturb + x / 10000);
        }
        if (hasLoopFlow) {
            elementaryFlowCnecResult.setLoopFlow(side, perturb + x + y + 13., MEGAWATT);
            elementaryFlowCnecResult.setLoopFlow(side, perturb + x + y + 23., AMPERE);
            elementaryFlowCnecResult.setCommercialFlow(side, perturb + x + y + 14, MEGAWATT);
            elementaryFlowCnecResult.setCommercialFlow(side, perturb + x + y + 24, AMPERE);
        }
    }

    private static void fillElementaryResult(ElementaryAngleCnecResult elementaryAngleCnecResult, double x, double y) {
        elementaryAngleCnecResult.setAngle(x + y + 35, DEGREE);
        elementaryAngleCnecResult.setMargin(x + y + 31, DEGREE);
    }

    private static void fillElementaryResult(ElementaryVoltageCnecResult elementaryVoltageCnecResult, double x, double y) {
        elementaryVoltageCnecResult.setVoltage(x + y + 46, KILOVOLT);
        elementaryVoltageCnecResult.setMargin(x + y + 41, KILOVOLT);
    }
}
