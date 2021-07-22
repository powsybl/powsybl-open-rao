/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

import java.util.Objects;

/**
 * UcteBusHelper is a utility class which manages buses defined with the UCTE convention
 * <p>
 * It helps map a bus ID to an element of the network. Wildcards and missing characters are allowed.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteBusHelper {

    private static final int UCTE_NODE_LENGTH = 8;
    private static final String WILDCARD_CHARACTER = "*";

    private String busIdInNetwork;
    private boolean isValid = false;
    private String invalidReason;

    public UcteBusHelper(String nodeName, Network network, boolean completeSmallBusIdsWithWildcards) {
        for (Bus bus : network.getBusBreakerView().getBuses()) {
            if (matchNodeNames(nodeName, bus.getId(), completeSmallBusIdsWithWildcards)) {
                if (Objects.isNull(busIdInNetwork)) {
                    isValid = true;
                    busIdInNetwork = bus.getId();
                } else {
                    invalidReason = String.format("Too many buses match name %s, for example %s and %s", nodeName, busIdInNetwork, bus.getId());
                    isValid = false;
                    busIdInNetwork = null;
                    return;
                }
            }
        }
        if (Objects.isNull(busIdInNetwork)) {
            invalidReason = String.format("No bus in the network matches bus name %s", nodeName);
        }
    }

    public UcteBusHelper(String busName, String matchingBusNameCandidate, boolean completeSmallBusIdsWithWildcards) {
        if (matchNodeNames(busName, matchingBusNameCandidate, completeSmallBusIdsWithWildcards)) {
            isValid = true;
            busIdInNetwork = matchingBusNameCandidate;
        } else {
            invalidReason = String.format("Candidate bus %s does not match the bus name %s", matchingBusNameCandidate, busName);
        }
    }

    /**
     * Match a node name to a node name from the network. The node name can contain a wildcard or be shorter than
     * the standard UCTE length
     */
    private static boolean matchNodeNames(String nodeName, String nodeNameInNetwork, boolean completeSmallBusIdsWithWildcards) {
        // TODO : unit tests for YNODE
        String modNodeNameInNetwork = nodeNameInNetwork.replace("YNODE_", "");
        if (nodeName.length() < UCTE_NODE_LENGTH) {
            if (completeSmallBusIdsWithWildcards) {
                return modNodeNameInNetwork.substring(0, nodeName.length()).equals(nodeName);
            } else {
                return modNodeNameInNetwork.equals(String.format("%1$-8s", nodeName));
            }
        } else if (nodeName.endsWith(WILDCARD_CHARACTER)) {
            return modNodeNameInNetwork.substring(0, modNodeNameInNetwork.length() - 1).equals(nodeName.substring(0, nodeName.length() - 1));
        } else {
            return modNodeNameInNetwork.equals(nodeName);
        }
    }

    public String getBusIdInNetwork() {
        return busIdInNetwork;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getInvalidReason() {
        return invalidReason;
    }
}
