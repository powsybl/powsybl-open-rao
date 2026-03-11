/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl.utils;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.impl.AngleCnecResult;
import com.powsybl.openrao.data.raoresult.impl.CostResult;
import com.powsybl.openrao.data.raoresult.impl.ElementaryAngleCnecResult;
import com.powsybl.openrao.data.raoresult.impl.ElementaryFlowCnecResult;
import com.powsybl.openrao.data.raoresult.impl.ElementaryVoltageCnecResult;
import com.powsybl.openrao.data.raoresult.impl.FlowCnecResult;
import com.powsybl.openrao.data.raoresult.impl.NetworkActionResult;
import com.powsybl.openrao.data.raoresult.impl.RangeActionResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.data.raoresult.impl.VoltageCnecResult;

import java.util.Set;

import static com.powsybl.openrao.commons.Unit.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ExhaustiveRaoResultCreation {

    /*
    Small CRAC used in unit tests of open-rao

    The idea of this RaoResult is to be quite exhaustive regarding the diversity of its object.
    It contains numerous object to ensure that they are all covered when testing the RaoResult
     */

    private ExhaustiveRaoResultCreation() {
    }

    public static RaoResult create(Crac crac) {
        RaoResultImpl raoResult = new RaoResultImpl(crac);
        raoResult.setComputationStatus(ComputationStatus.DEFAULT);
        raoResult.setExecutionDetails("The RAO only went through first preventive and went through voltage monitoring and went through angle monitoring");

        // --------------------
        // --- Cost results ---
        // --------------------

        // CostResult at initial state
        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult("initial");
        costResult.setFunctionalCost(100.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        // CostResult after PRA
        costResult = raoResult.getAndCreateIfAbsentCostResult("preventive");
        costResult.setFunctionalCost(80.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        // CostResult after ARA
        costResult = raoResult.getAndCreateIfAbsentCostResult("auto");
        costResult.setFunctionalCost(-20.);
        costResult.setVirtualCost("loopFlow", 15.);
        costResult.setVirtualCost("MNEC", 20.);

        // CostResult after CRA
        costResult = raoResult.getAndCreateIfAbsentCostResult("curative");
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
            fillFlowCnecResult(flowCnecResult, cnec, crac);
        }

        for (AngleCnec cnec : crac.getAngleCnecs()) {
            AngleCnecResult angleCnecResult = raoResult.getAndCreateIfAbsentAngleCnecResult(cnec);
            fillAngleCnecResult(angleCnecResult, cnec, crac);
        }

        for (VoltageCnec cnec : crac.getVoltageCnecs()) {
            VoltageCnecResult voltageCnecResult = raoResult.getAndCreateIfAbsentVoltageCnecResult(cnec);
            fillVoltageCnecResult(voltageCnecResult, cnec, crac);
        }

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        Instant autoInstant = crac.getInstant("auto");
        Instant curativeInstant = crac.getInstant("curative");
        for (NetworkAction networkAction : crac.getNetworkActions()) {
            NetworkActionResult nar = raoResult.getAndCreateIfAbsentNetworkActionResult(networkAction);

            switch (networkAction.getId()) {
                case "complexNetworkActionId" :
                    // free to use preventive, activated
                    nar.addActivationForState(crac.getPreventiveState());
                    break;
                case "injectionSetpointRaId" :
                    // automaton, activated
                    nar.addActivationForState(crac.getState("contingency2Id", autoInstant));
                    break;
                case "pstSetpointRaId" :
                    // forced in curative, activated
                    nar.addActivationForState(crac.getState("contingency1Id", curativeInstant));
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
                    hrar.addActivationForState(crac.getState("contingency1Id", curativeInstant), 100);
                    hrar.addActivationForState(crac.getState("contingency2Id", curativeInstant), 400);
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
        irar.addActivationForState(crac.getState("contingency1Id", curativeInstant), -300);

        return raoResult;
    }

    private static void fillFlowCnecResult(FlowCnecResult flowCnecResult, FlowCnec cnec, Crac crac) {

        double x = Integer.parseInt(String.valueOf(cnec.getId().charAt(4))) * 1000;
        boolean hasLoopFlow = cnec.getId().startsWith("cnec1") || cnec.getId().startsWith("cnec2");
        boolean isPureMnec = cnec.isMonitored() && !cnec.isOptimized();

        ElementaryFlowCnecResult initialEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
        fillElementaryResult(initialEfcr, x, 100, hasLoopFlow, isPureMnec, cnec.getMonitoredSides());
        ElementaryFlowCnecResult afterPraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("preventive"));
        fillElementaryResult(afterPraEfcr, x, 200, hasLoopFlow, isPureMnec, cnec.getMonitoredSides());

        if (cnec.getState().getInstant().isAuto() || cnec.getState().getInstant().isCurative()) {
            ElementaryFlowCnecResult afterAraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("auto"));
            fillElementaryResult(afterAraEfcr, x, 300, hasLoopFlow, isPureMnec, cnec.getMonitoredSides());
        }
        if (cnec.getState().getInstant().isCurative()) {
            ElementaryFlowCnecResult afterCraEfcr = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("curative"));
            fillElementaryResult(afterCraEfcr, x, 400, hasLoopFlow, isPureMnec, cnec.getMonitoredSides());
        }
    }

    private static void fillAngleCnecResult(AngleCnecResult angleCnecResult, AngleCnec cnec, Crac crac) {

        double x = 3000;

        ElementaryAngleCnecResult initialEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
        fillElementaryResult(initialEacr, x, 100);
        ElementaryAngleCnecResult afterPraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("preventive"));
        fillElementaryResult(afterPraEacr, x, 200);

        if (cnec.getState().getInstant().isAuto() || cnec.getState().getInstant().isCurative()) {
            ElementaryAngleCnecResult afterAraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("auto"));
            fillElementaryResult(afterAraEacr, x, 300);
        }
        if (cnec.getState().getInstant().isCurative()) {
            ElementaryAngleCnecResult afterCraEacr = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("curative"));
            fillElementaryResult(afterCraEacr, x, 400);
        }
    }

    private static void fillVoltageCnecResult(VoltageCnecResult voltageCnecResult, VoltageCnec cnec, Crac crac) {

        double x = 4000;

        ElementaryVoltageCnecResult initialEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
        fillElementaryResult(initialEacr, x, 100);
        ElementaryVoltageCnecResult afterPraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("preventive"));
        fillElementaryResult(afterPraEacr, x, 200);

        if (cnec.getState().getInstant().isAuto() || cnec.getState().getInstant().isCurative()) {
            ElementaryVoltageCnecResult afterAraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("auto"));
            fillElementaryResult(afterAraEacr, x, 300);
        }
        if (cnec.getState().getInstant().isCurative()) {
            ElementaryVoltageCnecResult afterCraEacr = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(crac.getInstant("curative"));
            fillElementaryResult(afterCraEacr, x, 400);
        }
    }

    private static void fillElementaryResult(ElementaryFlowCnecResult elementaryFlowCnecResult, double x, double y, boolean hasLoopFlow, boolean isPureMnec, Set<TwoSides> sides) {
        sides.forEach(side -> fillElementaryResult(elementaryFlowCnecResult, x, y, hasLoopFlow, isPureMnec, side));
    }

    private static void fillElementaryResult(ElementaryFlowCnecResult elementaryFlowCnecResult, double x, double y, boolean hasLoopFlow, boolean isPureMnec, TwoSides side) {
        double perturb = side.equals(TwoSides.ONE) ? 0 : 0.5;

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
        elementaryVoltageCnecResult.setMinVoltage(x + y + 46, KILOVOLT);
        elementaryVoltageCnecResult.setMaxVoltage(x + y + 56, KILOVOLT);
        elementaryVoltageCnecResult.setMargin(x + y + 41, KILOVOLT);
    }
}
