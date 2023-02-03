/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintInCountry;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        crac.getStates(Instant.AUTO).forEach(state -> {
            if (hasNoRemedialAction(state, crac) || hasGlobalRemedialActions(state, crac)) {
                // 1. Auto state has no RA => it will not constitute a perimeter
                //    => Auto CNECs will be optimized in preventive RAO, no need to duplicate them
                // 2. If state has "global" RA (useful for all CNECs), nothing to do neither
                return;
            }
            // Find CNECs with no useful RA and duplicate them on outage instant
            crac.getFlowCnecs(state).stream()
                .filter(cnec -> crac.getRemedialActions().stream().noneMatch(ra -> isRaUsefulForCnec(ra, cnec, network)))
                .forEach(cnec -> {
                    duplicateCnecOnOutageInstant(crac, cnec);
                    report.add(String.format("CNEC \"%s\" has no associated automaton. It will be cloned on the OUTAGE instant in order to be secured during preventive RAO.", cnec.getId()));
                });
        });
        return report;
    }

    private static void duplicateCnecOnOutageInstant(Crac crac, FlowCnec cnec) {
        FlowCnecAdder adder = crac.newFlowCnec()
            .withId(cnec.getId() + " - OUTAGE DUPLICATE")
            .withNetworkElement(cnec.getNetworkElement().getId())
            .withIMax(cnec.getIMax(Side.LEFT), Side.LEFT)
            .withIMax(cnec.getIMax(Side.RIGHT), Side.RIGHT)
            .withNominalVoltage(cnec.getNominalVoltage(Side.LEFT), Side.LEFT)
            .withNominalVoltage(cnec.getNominalVoltage(Side.RIGHT), Side.RIGHT)
            .withReliabilityMargin(cnec.getReliabilityMargin())
            .withInstant(Instant.OUTAGE).withContingency(cnec.getState().getContingency().orElseThrow().getId())
            .withOptimized(cnec.isOptimized())
            .withMonitored(cnec.isMonitored());
        copyThresholds(cnec, adder);
        adder.add();
    }

    private static boolean hasNoRemedialAction(State state, Crac crac) {
        return crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED).isEmpty()
            && crac.getNetworkActions(state, UsageMethod.AVAILABLE, UsageMethod.TO_BE_EVALUATED, UsageMethod.FORCED).isEmpty();
    }

    private static boolean hasGlobalRemedialActions(State state, Crac crac) {
        return !crac.getRangeActions(state, UsageMethod.AVAILABLE, UsageMethod.FORCED).isEmpty()
            || !crac.getNetworkActions(state, UsageMethod.AVAILABLE, UsageMethod.FORCED).isEmpty();
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

    private static boolean isRaUsefulForCnec(RemedialAction<?> ra, FlowCnec cnec, Network network) {
        if (Set.of(UsageMethod.AVAILABLE, UsageMethod.FORCED).contains(ra.getUsageMethod(cnec.getState()))) {
            return true;
        }
        if (ra.getUsageMethod(cnec.getState()).equals(UsageMethod.TO_BE_EVALUATED)) {
            return ra.getUsageRules().stream()
                .filter(OnFlowConstraint.class::isInstance)
                .map(OnFlowConstraint.class::cast)
                .anyMatch(ofc -> isOfcUsefulForCnec(ofc, cnec))
                ||
                ra.getUsageRules().stream()
                    .filter(OnFlowConstraintInCountry.class::isInstance)
                    .map(OnFlowConstraintInCountry.class::cast)
                    .anyMatch(ofc -> isOfccUsefulForCnec(ofc, cnec, network));
        }
        return false;
    }

    /**
     * Returns true if a given OnFlowConstraint usage rule is applicable for a given FlowCnec
     */
    private static boolean isOfcUsefulForCnec(OnFlowConstraint ofc, FlowCnec cnec) {
        return ofc.getFlowCnec().equals(cnec);
    }

    /**
     * Returns true if a given OnFlowConstraintInCountry usage rule is applicable for a given FlowCnec
     */
    private static boolean isOfccUsefulForCnec(OnFlowConstraintInCountry ofcc, FlowCnec cnec, Network network) {
        return cnec.getLocation(network).contains(Optional.of(ofcc.getCountry()));
    }
}
