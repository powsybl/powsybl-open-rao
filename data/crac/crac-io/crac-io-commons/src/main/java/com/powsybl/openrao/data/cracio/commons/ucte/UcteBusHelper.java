/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.openrao.data.cracio.commons.ElementHelper;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.cracio.commons.ucte.UcteUtils.UCTE_NODE_LENGTH;
import static com.powsybl.openrao.data.cracio.commons.ucte.UcteUtils.WILDCARD_CHARACTER;

/**
 * UcteBusHelper is a utility class which manages buses defined with the UCTE convention
 * <p>
 * It helps map a bus ID to an element of the network. Wildcards and missing characters are allowed.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteBusHelper implements ElementHelper {

    private Set<Bus> busMatchesInNetwork = new HashSet<>();
    private boolean isValid = false;
    private String invalidReason;

    public UcteBusHelper(String nodeName, UcteNetworkAnalyzer ucteNetworkAnalyzer) {

        String modNodeName = nodeName;

        // full id without wildcard
        if (nodeName.length() == UCTE_NODE_LENGTH && !nodeName.endsWith(WILDCARD_CHARACTER)) {
            lookForBusWithIdInNetwork(nodeName, ucteNetworkAnalyzer.getNetwork());

            if (!isValid && ucteNetworkAnalyzer.getProperties().getBusIdMatchPolicy().equals(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD)) {
                // if no bus is found and policy is REPLACE_8TH_CHARACTER_WITH_WILDCARD, replace 8 character by *
                modNodeName = String.format("%1$-7s", nodeName).substring(0, 7) + UcteUtils.WILDCARD_CHARACTER;
            } else {
                return;
            }
        }

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
        lookForBusIdMatches(modNodeName, ucteNetworkAnalyzer);
    }

    private void lookForBusIdMatches(String nodeName, UcteNetworkAnalyzer ucteNetworkAnalyzer) {
        busMatchesInNetwork = ucteNetworkAnalyzer.getNetwork().getBusBreakerView().getBusStream()
            .filter(bus -> UcteUtils.matchNodeNames(nodeName, bus.getId()))
            .collect(Collectors.toSet());
        if (busMatchesInNetwork.isEmpty()) {
            isValid = false;
            invalidReason = String.format("No bus in the network matches bus name %s", nodeName);
        } else {
            isValid = true;
            invalidReason = null;
        }
    }

    /**
     * Look for a bus in the network, knowing its full id (without wildcard)
     */
    private void lookForBusWithIdInNetwork(String busId, Network network) {
        Bus bus = network.getBusBreakerView().getBus(busId);

        if (bus != null) {
            busMatchesInNetwork.add(bus);
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
        if (busMatchesInNetwork.isEmpty()) {
            return null;
        } else if (busMatchesInNetwork.size() == 1) {
            return busMatchesInNetwork.iterator().next().getId();
        } else {
            throw new UnsupportedOperationException("Too many buses in the network match the given bus name. Access the list using getBusIdMatchesInNetwork() instead");
        }
    }

    public Set<Bus> getBusMatchesInNetwork() {
        return busMatchesInNetwork;
    }
}
