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

import static com.farao_community.farao.data.crac_creation_util.UcteUtils.UCTE_NODE_LENGTH;
import static com.farao_community.farao.data.crac_creation_util.UcteUtils.WILDCARD_CHARACTER;

/**
 * UcteBusHelper is a utility class which manages buses defined with the UCTE convention
 * <p>
 * It helps map a bus ID to an element of the network. Wildcards and missing characters are allowed.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteBusHelper {

    private String busIdInNetwork;
    private boolean isValid = false;
    private String invalidReason;

    public UcteBusHelper(String nodeName, Network network, boolean completeSmallBusIdsWithWildcards) {

        // full id without wildcard
        if (nodeName.length() == UCTE_NODE_LENGTH && !nodeName.endsWith(WILDCARD_CHARACTER)) {
            lookForBusWithIdInNetwork(nodeName, network);
            return;
        }

        // incomplete id, automatically completed with blank spaces
        if (nodeName.length() < UCTE_NODE_LENGTH && !completeSmallBusIdsWithWildcards) {
            lookForBusWithIdInNetwork(String.format("%1$-8s", nodeName), network);
            return;
        }

        // complex search with wildcard (either *, or incomplete ids)
        for (Bus bus : network.getBusBreakerView().getBuses()) {
            if (UcteUtils.matchNodeNames(nodeName, bus.getId(), completeSmallBusIdsWithWildcards)) {
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

    // todo: delete constructor, use util class instead
    public UcteBusHelper(String busName, String matchingBusNameCandidate, boolean completeSmallBusIdsWithWildcards) {
        if (UcteUtils.matchNodeNames(busName, matchingBusNameCandidate, completeSmallBusIdsWithWildcards)) {
            isValid = true;
            busIdInNetwork = matchingBusNameCandidate;
        } else {
            invalidReason = String.format("Candidate bus %s does not match the bus name %s", matchingBusNameCandidate, busName);
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
