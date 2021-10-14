/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.util.ucte;

import com.farao_community.farao.data.crac_creation.util.ElementHelper;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

import java.util.Objects;

import static com.farao_community.farao.data.crac_creation.util.ucte.UcteUtils.UCTE_NODE_LENGTH;
import static com.farao_community.farao.data.crac_creation.util.ucte.UcteUtils.WILDCARD_CHARACTER;

/**
 * UcteBusHelper is a utility class which manages buses defined with the UCTE convention
 * <p>
 * It helps map a bus ID to an element of the network. Wildcards and missing characters are allowed.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteBusHelper implements ElementHelper {

    private String busIdInNetwork;
    private boolean isValid = false;
    private String invalidReason;

    public UcteBusHelper(String nodeName, UcteNetworkAnalyzer ucteNetworkAnalyzer) {

        // full id without wildcard
        if (nodeName.length() == UCTE_NODE_LENGTH && !nodeName.endsWith(WILDCARD_CHARACTER)) {
            lookForBusWithIdInNetwork(nodeName, ucteNetworkAnalyzer.getNetwork());
            return;
        }

        String modNodeName = nodeName;
        // incomplete id, automatically complete id with...
        if (nodeName.length() < UCTE_NODE_LENGTH) { // blank spaces,
            if (ucteNetworkAnalyzer.getProperties().getBusIdMatchPolicy().equals(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES)) {
                lookForBusWithIdInNetwork(String.format("%1$-8s", nodeName), ucteNetworkAnalyzer.getNetwork());
                return;
            } else {  // or, with wildcards
                modNodeName = String.format("%1$-7s", nodeName.substring(0, Math.min(nodeName.length(), 7))) + WILDCARD_CHARACTER;
            }
        }

        // complex search with wildcard (either *, or incomplete ids)
        for (Bus bus : ucteNetworkAnalyzer.getNetwork().getBusBreakerView().getBuses()) {
            if (UcteUtils.matchNodeNames(modNodeName, bus.getId())) {
                if (Objects.isNull(busIdInNetwork)) {
                    isValid = true;
                    busIdInNetwork = bus.getId();
                } else {
                    invalidReason = String.format("Too many buses match name %s, for example %s and %s", modNodeName, busIdInNetwork, bus.getId());
                    isValid = false;
                    busIdInNetwork = null;
                    return;
                }
            }
        }
        if (Objects.isNull(busIdInNetwork)) {
            invalidReason = String.format("No bus in the network matches bus name %s", modNodeName);
        }
    }

    /**
     * Look for a bus in the network, knowing its full id (without wildcard)
     */
    private void lookForBusWithIdInNetwork(String busId, Network network) {
        Bus bus = network.getBusBreakerView().getBus(busId);

        if (bus != null) {
            busIdInNetwork = busId;
            isValid = true;
        } else {
            invalidReason = String.format("No bus in the network matches bus id %s", busId);
        }
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public String getInvalidReason() {
        return invalidReason;
    }

    @Override
    public String getIdInNetwork() {
        return busIdInNetwork;
    }
}
