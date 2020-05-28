/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoInput {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoInput.class);

    private RaoInput() { }

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

        ArrayList<Cnec> absentFromNetworkCnecs = new ArrayList<>();
        crac.getCnecs().forEach(cnec -> {
            if (network.getBranch(cnec.getNetworkElement().getId()) == null) {
                absentFromNetworkCnecs.add(cnec);
                report.add(String.format("[REMOVED] Cnec %s with network element [%s] is not present in the network. It is removed from the Crac", cnec.getId(), cnec.getNetworkElement().getId()));
            }
        });
        absentFromNetworkCnecs.forEach(cnec -> crac.getCnecs().remove(cnec));
        ArrayList<RangeAction> absentFromNetworkRangeActions = new ArrayList<>();
        for (RangeAction rangeAction: crac.getRangeActions()) {
            rangeAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkRangeActions.add(rangeAction);
                    report.add(String.format("[REMOVED] Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", rangeAction.getId(), networkElement.getId()));
                }
            });
        }
        absentFromNetworkRangeActions.forEach(rangeAction -> crac.getRangeActions().remove(rangeAction));

        ArrayList<NetworkAction> absentFromNetworkNetworkActions = new ArrayList<>();
        for (NetworkAction networkAction: crac.getNetworkActions()) {
            networkAction.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkNetworkActions.add(networkAction);
                    report.add(String.format("[REMOVED] Remedial Action %s with network element [%s] is not present in the network. It is removed from the Crac", networkAction.getId(), networkElement.getId()));
                }
            });
        }
        absentFromNetworkNetworkActions.forEach(networkAction -> crac.getNetworkActions().remove(networkAction));

        ArrayList<Contingency> absentFromNetworkContingencies = new ArrayList<>();
        for (Contingency contingency : crac.getContingencies()) {
            contingency.getNetworkElements().forEach(networkElement -> {
                if (network.getIdentifiable(networkElement.getId()) == null) {
                    absentFromNetworkContingencies.add(contingency);
                    report.add(String.format("[REMOVED] Contingency %s with network element [%s] is not present in the network. It is removed from the Crac", contingency.getId(), networkElement.getId()));
                }
            });
        }

        absentFromNetworkContingencies.forEach(contingency ->  {
            crac.getStatesFromContingency(contingency.getId()).forEach(state -> {
                crac.getCnecs(state).forEach(cnec -> crac.getCnecs().remove(cnec));
                crac.getStates().remove(state);
            });
            crac.getContingencies().remove(contingency);
        });

        report.forEach(LOGGER::warn);

        return report;
    }
}
