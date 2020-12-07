/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
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

    // TODO: delete this! It is unused in this repo (migrated to CracAliasesUtil.java), but still used in some files of other projects
    @Deprecated
    public static List<String> cleanCrac(Crac crac, Network network) {
        List<String> summary = new ArrayList<>();

        // remove Cnec whose NetworkElement is absent from the network
        ArrayList<Cnec> absFromNetworkCnecs = new ArrayList<>();
        crac.getCnecs().forEach(cnec -> {
            if (network.getBranch(cnec.getNetworkElement().getId()) == null) {
                absFromNetworkCnecs.add(cnec);
                summary.add(String.format("[REMOVED] Cnec %s with network element [%s] is not present in the network. It is removed from the Crac", cnec.getId(), cnec.getNetworkElement().getId()));
            }
        });
        absFromNetworkCnecs.forEach(cnec -> crac.removeCnec(cnec.getId()));

        // remove Cnecs that are neither optimized nor monitored
        ArrayList<Cnec> unmonitoredCnecsList = new ArrayList<>();
        crac.getCnecs().forEach(cnec -> {
            if (!cnec.isOptimized() && !cnec.isMonitored()) {
                unmonitoredCnecsList.add(cnec);
                summary.add(String.format("[REMOVED] Cnec %s with network element [%s] is neither optimized nor monitored. It is removed from the Crac", cnec.getId(), cnec.getNetworkElement().getId()));
            }
        });
        unmonitoredCnecsList.forEach(cnec -> crac.removeCnec(cnec.getId()));

        // remove RangeAction whose NetworkElement is absent from the network
        ArrayList<RangeAction> absFromNetworkRangeActions = new ArrayList<>();
        for (RangeAction rangeAction: crac.getRangeActions()) {
            rangeAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absFromNetworkRangeActions.add(rangeAction);
                    summary.add(String.format("[REMOVED] Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", rangeAction.getId(), networkElement.getId()));
                }
            });
        }
        absFromNetworkRangeActions.forEach(rangeAction -> crac.removeRangeAction(rangeAction.getId()));

        // remove NetworkAction whose NetworkElement is absent from the network
        ArrayList<NetworkAction> absFromNetworkNetworkActions = new ArrayList<>();
        for (NetworkAction networkAction: crac.getNetworkActions()) {
            networkAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absFromNetworkNetworkActions.add(networkAction);
                    summary.add(String.format("[REMOVED] Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", networkAction.getId(), networkElement.getId()));
                }
            });
        }
        absFromNetworkNetworkActions.forEach(networkAction -> crac.removeNetworkAction(networkAction.getId()));

        // remove Contingencies whose NetworkElement is absent from the network or does not fit a valid Powsybl Contingency
        Set<Contingency> absFromNetworkContingencies = new HashSet<>();
        for (Contingency contingency : crac.getContingencies()) {
            contingency.getNetworkElements().forEach(networkElement -> {
                Identifiable<?> identifiable = network.getIdentifiable(networkElement.getId());
                if (identifiable == null) {
                    absFromNetworkContingencies.add(contingency);
                    summary.add(String.format("[REMOVED] Contingency %s with network element [%s] is not present in the network. It is removed from the Crac", contingency.getId(), networkElement.getId()));
                } else if (!(identifiable instanceof Branch || identifiable instanceof Generator || identifiable instanceof HvdcLine || identifiable instanceof BusbarSection || identifiable instanceof DanglingLine)) {
                    summary.add(String.format("[WARNING] Contingency %s has a network element [%s] of unhandled type [%s]. This may result in unexpected behavior.", contingency.getId(), networkElement.getId(), identifiable.getClass().toString()));
                }
            });
        }

        absFromNetworkContingencies.forEach(contingency ->  {
            crac.getStatesFromContingency(contingency.getId()).forEach(state -> {
                crac.getCnecs(state).forEach(cnec -> {
                    crac.removeCnec(cnec.getId());
                    summary.add(String.format("[REMOVED] Cnec %s is removed because its associated contingency [%s] has been removed", cnec.getId(), contingency.getId()));
                });
                crac.removeState(state.getId());
            });
            crac.removeContingency(contingency.getId());
        });

        // remove Remedial Action with an empty list of NetworkElement
        ArrayList<NetworkAction> notValidAction = new ArrayList<>();
        crac.getNetworkActions().stream().filter(na -> na.getNetworkElements().isEmpty()).forEach(na -> {
            summary.add(String.format("[REMOVED] Remedial Action %s has no associated action. It is removed from the Crac", na.getId()));
            notValidAction.add(na);
        });
        notValidAction.forEach(networkAction -> crac.removeNetworkAction(networkAction.getId()));

        summary.forEach(LOGGER::warn);

        return summary;
    }
}
