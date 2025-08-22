/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.util;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnContingencyState;
import com.powsybl.openrao.data.crac.api.usagerule.OnFlowConstraintInCountry;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.api.usagerule.UsageRule;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.threshold.BranchThresholdAdder;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Misc features that clean up a CRAC to prepare it for the RAO
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CracValidator {

    private CracValidator() {
        // should not be used
    }

    public static List<String> validateCrac(Crac crac, Network network) {
        return new ArrayList<>(addOutageCnecsForAutoCnecsWithoutRas(crac, network));
    }

    /**
     * Since auto CNECs that have no RA associated cannot be secured by the RAO, this function duplicates these CNECs
     * but on the OUTAGE instant.
     * Beware that the CRAC is modified since extra CNECs are added.
     */
    private static List<String> addOutageCnecsForAutoCnecsWithoutRas(Crac crac, Network network) {
        List<String> report = new ArrayList<>();
        if (!crac.getInstants(InstantKind.AUTO).isEmpty()) {
            crac.getStates(crac.getInstant(InstantKind.AUTO))
                .forEach(state -> duplicateCnecsWithNoUsefulRaOnOutageInstant(crac, network, state, report));
        }
        return report;
    }

    private static void duplicateCnecsWithNoUsefulRaOnOutageInstant(Crac crac, Network network, State state, List<String> report) {
        if (hasNoRemedialAction(state, crac) || hasGlobalRemedialActions(state, crac)) {
            // 1. Auto state has no RA => it will not constitute a perimeter
            //    => Auto CNECs will be optimized in preventive RAO, no need to duplicate them
            // 2. If state has "global" RA (useful for all CNECs), nothing to do neither
            return;
        }
        // Find CNECs with no useful RA and duplicate them on outage instant
        Set<RemedialAction<?>> remedialActions = new HashSet<>();
        remedialActions.addAll(crac.getPotentiallyAvailableRangeActions(state));
        remedialActions.addAll(crac.getPotentiallyAvailableNetworkActions(state));

        crac.getFlowCnecs(state).stream()
            .filter(cnec -> shouldDuplicateAutoCnecInOutageState(remedialActions, cnec, network))
            .forEach(cnec -> {
                duplicateCnecOnOutageInstant(crac, cnec);
                report.add(String.format("CNEC \"%s\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO.", cnec.getId()));
            });
    }

    private static void duplicateCnecOnOutageInstant(Crac crac, FlowCnec cnec) {
        Instant outageInstant = crac.getOutageInstant();
        FlowCnecAdder adder = crac.newFlowCnec()
            .withId(cnec.getId() + " - OUTAGE DUPLICATE")
            .withNetworkElement(cnec.getNetworkElement().getId())
            .withNominalVoltage(cnec.getNominalVoltage(TwoSides.ONE), TwoSides.ONE)
            .withNominalVoltage(cnec.getNominalVoltage(TwoSides.TWO), TwoSides.TWO)
            .withReliabilityMargin(cnec.getReliabilityMargin())
            .withInstant(outageInstant.getId()).withContingency(cnec.getState().getContingency().orElseThrow().getId())
            .withOptimized(cnec.isOptimized())
            .withMonitored(cnec.isMonitored());
        cnec.getIMax(TwoSides.ONE).ifPresent(iMax -> adder.withIMax(iMax, TwoSides.ONE));
        cnec.getIMax(TwoSides.TWO).ifPresent(iMax -> adder.withIMax(iMax, TwoSides.TWO));
        copyThresholds(cnec, adder);
        adder.add();
    }

    private static boolean hasNoRemedialAction(State state, Crac crac) {
        return crac.getPotentiallyAvailableRangeActions(state).isEmpty()
            && crac.getPotentiallyAvailableNetworkActions(state).isEmpty();
    }

    private static boolean hasGlobalRemedialActions(State state, Crac crac) {
        return hasOnInstantOrOnStateUsageRules(crac.getRangeActions(state, UsageMethod.FORCED)) ||
            hasOnInstantOrOnStateUsageRules(crac.getNetworkActions(state, UsageMethod.FORCED));
    }

    private static <T extends RemedialAction<?>> boolean hasOnInstantOrOnStateUsageRules(Set<T> remedialActionSet) {
        return remedialActionSet.stream().anyMatch(rangeAction -> rangeAction.getUsageRules().stream().anyMatch(usageRule -> usageRule instanceof OnInstant || usageRule instanceof OnContingencyState));
    }

    private static void copyThresholds(FlowCnec cnec, FlowCnecAdder adder) {
        cnec.getThresholds().forEach(tr -> {
                BranchThresholdAdder trAdder = adder.newThreshold()
                    .withSide(tr.getSide())
                    .withUnit(tr.getUnit());
                if (tr.limitsByMax()) {
                    trAdder.withMax(tr.max().orElseThrow());
                }
                if (tr.limitsByMin()) {
                    trAdder.withMin(tr.min().orElseThrow());
                }
                trAdder.add();
            }
        );
    }

    /**
     * Indicates whether an auto FlowCNEC should be duplicated in the outage state or not.
     * A FlowCNEC must be duplicated if no auto remedial action can act on it, leaving only the preventive remedial
     * actions to possibly reduce the flow which means that the CNEC should be added to the preventive perimeter.
     * <p/>
     * This CNEC must however be kept in the auto instant because an overload on this line may be the triggering
     * condition of auto remedial actions that can affect other FlowCNECs of the same state.
     * <p/>
     * If no auto remedial action affects the CNEC and the CNEC does not trigger any auto remedial action, there is no
     * need to duplicate it because this means that no auto remedial action is available for this auto state at all.
     * In this case, the StateTree algorithm will automatically include all the CNECs from the state to the preventive perimeter.
     * @param remedialActions The set of remedial actions that may affect the CNEC
     * @param flowCnec The FlowCNEC to possibly duplicate
     * @param network The network
     * @return Boolean value that indicates whether the CNEC should be duplicate in the outage state or not
     */
    private static boolean shouldDuplicateAutoCnecInOutageState(Set<RemedialAction<?>> remedialActions, FlowCnec flowCnec, Network network) {
        boolean raForOtherCnecs = false;
        for (RemedialAction<?> remedialAction : remedialActions) {
            for (UsageRule usageRule : remedialAction.getUsageRules()) {
                if (usageRule instanceof OnInstant onInstant && onInstant.getInstant().equals(flowCnec.getState().getInstant())) {
                    return false;
                } else if (usageRule instanceof OnContingencyState onContingencyState && onContingencyState.getState().equals(flowCnec.getState())) {
                    return false;
                } else if (usageRule instanceof OnConstraint<?> onConstraint && onConstraint.getCnec() instanceof FlowCnec && onConstraint.getCnec().getState().equals(flowCnec.getState())) {
                    if (onConstraint.getCnec().equals(flowCnec)) {
                        return false;
                    } else {
                        raForOtherCnecs = true;
                    }
                } else if (usageRule instanceof OnFlowConstraintInCountry onFlowConstraintInCountry
                    && onFlowConstraintInCountry.getInstant().equals(flowCnec.getState().getInstant()) // TODO: why not comesBefore?
                    && (onFlowConstraintInCountry.getContingency().isEmpty() || flowCnec.getState().getContingency().equals(onFlowConstraintInCountry.getContingency()))) {
                    if (flowCnec.getLocation(network).contains(onFlowConstraintInCountry.getCountry())) {
                        return false;
                    } else {
                        raForOtherCnecs = true;
                    }
                }
            }
        }
        return raForOtherCnecs;
    }
}
