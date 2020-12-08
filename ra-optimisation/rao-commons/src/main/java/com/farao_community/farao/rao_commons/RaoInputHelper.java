/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoInputHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoInputHelper.class);

    private RaoInputHelper() { }

    public static void synchronize(Crac crac, Network network) {
        if (!crac.isSynchronized()) {
            crac.synchronize(network);
            LOGGER.debug("Crac {} has been synchronized with network {}", crac.getId(), network.getId());
        } else {
            LOGGER.debug("Crac {} is already synchronized", crac.getId());
        }
    }

    public static List<String> cleanCrac(Crac crac, Network network) {
        List<String> report = new ArrayList<>();

        // remove Cnec whose NetworkElement is absent from the network
        ArrayList<Cnec> absentFromNetworkCnecs = new ArrayList<>();
        crac.getCnecs().forEach(cnec -> {
            if (network.getBranch(cnec.getNetworkElement().getId()) == null) {
                absentFromNetworkCnecs.add(cnec);
                report.add(String.format("[REMOVED] Cnec %s with network element [%s] is not present in the network. It is removed from the Crac", cnec.getId(), cnec.getNetworkElement().getId()));
            }
        });
        absentFromNetworkCnecs.forEach(cnec -> crac.removeCnec(cnec.getId()));

        // remove Cnecs that are neither optimized nor monitored
        ArrayList<Cnec> unmonitoredCnecs = new ArrayList<>();
        crac.getCnecs().forEach(cnec -> {
            if (!cnec.isOptimized() && !cnec.isMonitored()) {
                unmonitoredCnecs.add(cnec);
                report.add(String.format("[REMOVED] Cnec %s with network element [%s] is neither optimized nor monitored. It is removed from the Crac", cnec.getId(), cnec.getNetworkElement().getId()));
            }
        });
        unmonitoredCnecs.forEach(cnec -> crac.removeCnec(cnec.getId()));

        // remove RangeAction whose NetworkElement is absent from the network
        ArrayList<RangeAction> absentFromNetworkRangeActions = new ArrayList<>();
        for (RangeAction rangeAction: crac.getRangeActions()) {
            rangeAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkRangeActions.add(rangeAction);
                    report.add(String.format("[REMOVED] Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", rangeAction.getId(), networkElement.getId()));
                }
            });
        }
        absentFromNetworkRangeActions.forEach(rangeAction -> crac.removeRangeAction(rangeAction.getId()));

        // remove NetworkAction whose NetworkElement is absent from the network
        ArrayList<NetworkAction> absentFromNetworkNetworkActions = new ArrayList<>();
        for (NetworkAction networkAction: crac.getNetworkActions()) {
            networkAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkNetworkActions.add(networkAction);
                    report.add(String.format("[REMOVED] Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", networkAction.getId(), networkElement.getId()));
                }
            });
        }
        absentFromNetworkNetworkActions.forEach(networkAction -> crac.removeNetworkAction(networkAction.getId()));

        // list Contingencies whose NetworkElement is absent from the network or does not fit a valid Powsybl Contingency
        Set<Contingency> removedContingencies = new HashSet<>();
        for (Contingency contingency : crac.getContingencies()) {
            contingency.getNetworkElements().forEach(networkElement -> {
                Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
                if (identifiable == null) {
                    removedContingencies.add(contingency);
                    report.add(String.format("[REMOVED] Contingency %s with network element [%s] is not present in the network. It is removed from the Crac", contingency.getId(), networkElement.getId()));
                } else if (!(identifiable instanceof Branch || identifiable instanceof Generator || identifiable instanceof HvdcLine || identifiable instanceof BusbarSection || identifiable instanceof DanglingLine)) {
                    report.add(String.format("[WARNING] Contingency %s has a network element [%s] of unhandled type [%s]. This may result in unexpected behavior.", contingency.getId(), networkElement.getId(), identifiable.getClass().toString()));
                }
                // do not delete contingencies now as they are needed to check which associated states/cnecs should be removed as well
            });
        }

        // list States whose contingency does not exist anymore
        Set<State> removedStates = new HashSet<>();
        for (Contingency contingency : removedContingencies) {
            crac.getStates(contingency).forEach(state -> {
                report.add(String.format("[REMOVED] State %s is removed because its associated contingency [%s] has been removed", state.getId(), contingency.getId()));
                removedStates.add(state);
            });
            // do not delete states now as they are needed to check which associated ra/cnecs should be removed as well
        }

        // remove Cnec whose contingency does not exist anymore
        removedContingencies.forEach(contingency ->
            crac.getStatesFromContingency(contingency.getId()).forEach(state ->
                crac.getCnecs(state).forEach(cnec -> {
                    crac.removeCnec(cnec.getId());
                    report.add(String.format("[REMOVED] Cnec %s is removed because its associated contingency [%s] has been removed", cnec.getId(), contingency.getId()));
                })
            )
        );

        // remove Remedial Action with an empty list of NetworkElement
        Set<RemedialAction<?>> noValidAction = new HashSet<>();
        crac.getNetworkActions().stream().filter(na -> na.getNetworkElements().isEmpty()).forEach(na -> {
            report.add(String.format("[REMOVED] Remedial Action %s has no associated action. It is removed from the Crac", na.getId()));
            noValidAction.add(na);
        });
        crac.getRangeActions().stream().filter(ra -> ra.getNetworkElements().isEmpty()).forEach(ra -> {
            report.add(String.format("[REMOVED] Remedial Action %s has no associated action. It is removed from the Crac", ra.getId()));
            noValidAction.add(ra);
        });
        noValidAction.forEach(networkAction -> crac.removeNetworkAction(networkAction.getId()));

        // remove On State usage rule with invalid state
        crac.getRangeActions().forEach(ra -> checkUsageRules(ra, removedStates, report));
        crac.getNetworkActions().forEach(ra -> checkUsageRules(ra, removedStates, report));

        // remove states and contingencies
        removedContingencies.forEach(contingency -> crac.removeContingency(contingency.getId()));
        removedStates.forEach(state -> crac.removeState(state.getId()));

        report.forEach(LOGGER::warn);

        return report;
    }

    private static void checkUsageRules(RemedialAction<?> remedialAction, Set<State> removedStates, List<String> report) {
        Set<UsageRule> removedUr = new HashSet<>();
        remedialAction.getUsageRules().forEach(usageRule -> {
            if (usageRule instanceof OnState && removedStates.contains(((OnState) usageRule).getState())) {
                report.add(String.format("[REMOVED] OnState usage rule of RA %s is removed because its associated state [%s] has been removed",
                    remedialAction.getId(),
                    ((OnState) usageRule).getState().getId()));
                removedUr.add(usageRule);
            }
        });
        remedialAction.getUsageRules().removeAll(removedUr);
    }
}
