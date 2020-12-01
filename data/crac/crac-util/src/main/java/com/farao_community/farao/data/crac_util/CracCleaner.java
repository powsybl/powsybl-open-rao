/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public final class CracCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CracCleaner.class);

    private CracCleaner() {
    }

    public static void cracAliasesUtil(Crac crac, Network network) {
        // List (without duplicates) all the crac elements that need to be found in the network
        Set<String> elementIds = new HashSet<>();
        crac.getCnecs().forEach(cnec -> elementIds.add(cnec.getNetworkElement().getId()));
        crac.getContingencies().forEach(contingency -> contingency.getNetworkElements().forEach(networkElement -> elementIds.add(networkElement.getId())));

        // Try to find a corresponding element in the network, and add elementId as an alias
        elementIds.forEach(elementId -> {
            Optional<Identifiable<?>> correspondingElement = network.getIdentifiables().stream().filter(identifiable -> anyMatch(identifiable, elementId)).findAny();
            correspondingElement.ifPresent(identifiable -> identifiable.addAlias(elementId));
        });
    }

    private static boolean anyMatch(Identifiable<?> identifiable, String cnecId) {
        return nameMatches(identifiable, cnecId, false) ||
            aliasMatches(identifiable, cnecId, false) ||
            nameMatches(identifiable, cnecId, true) ||
            aliasMatches(identifiable, cnecId, true);
    }

    private static boolean nameMatches(Identifiable<?> identifiable, String cnecId, boolean reverse) {
        return identifiable.getId().trim().matches(checkWithPattern(cnecId, reverse));
    }

    private static boolean aliasMatches(Identifiable<?> identifiable, String cnecId, boolean reverse) {
        return identifiable.getAliases().stream().anyMatch(alias -> alias.trim().matches(checkWithPattern(cnecId, reverse)));
    }

    private static String checkWithPattern(String string, boolean reverse) {
        int first = reverse ? 9 : 0;
        int second = reverse ? 0 : 9;
        return Pattern.quote(string.substring(first, first + 7)) + ".*" + " " + Pattern.quote(string.substring(second, second + 7)) + ".*" + Pattern.quote(string.substring(17)).trim();
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

        // remove Contingencies whose NetworkElement is absent from the network or does not fit a valid Powsybl Contingency
        Set<Contingency> absentFromNetworkContingencies = new HashSet<>();
        for (Contingency contingency : crac.getContingencies()) {
            contingency.getNetworkElements().forEach(networkElement -> {
                Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
                if (identifiable == null) {
                    absentFromNetworkContingencies.add(contingency);
                    report.add(String.format("[REMOVED] Contingency %s with network element [%s] is not present in the network. It is removed from the Crac", contingency.getId(), networkElement.getId()));
                } else if (!(identifiable instanceof Branch || identifiable instanceof Generator || identifiable instanceof HvdcLine || identifiable instanceof BusbarSection || identifiable instanceof DanglingLine)) {
                    report.add(String.format("[WARNING] Contingency %s has a network element [%s] of unhandled type [%s]. This may result in unexpected behavior.", contingency.getId(), networkElement.getId(), identifiable.getClass().toString()));
                }
            });
        }

        absentFromNetworkContingencies.forEach(contingency ->  {
            crac.getStatesFromContingency(contingency.getId()).forEach(state -> {
                crac.getCnecs(state).forEach(cnec -> {
                    crac.removeCnec(cnec.getId());
                    report.add(String.format("[REMOVED] Cnec %s is removed because its associated contingency [%s] has been removed", cnec.getId(), contingency.getId()));
                });
                crac.removeState(state.getId());
            });
            crac.removeContingency(contingency.getId());
        });

        // remove Remedial Action with an empty list of NetworkElement
        ArrayList<NetworkAction> noValidAction = new ArrayList<>();
        crac.getNetworkActions().stream().filter(na -> na.getNetworkElements().isEmpty()).forEach(na -> {
            report.add(String.format("[REMOVED] Remedial Action %s has no associated action. It is removed from the Crac", na.getId()));
            noValidAction.add(na);
        });
        noValidAction.forEach(networkAction -> crac.removeNetworkAction(networkAction.getId()));

        report.forEach(LOGGER::warn);

        return report;
    }
}
