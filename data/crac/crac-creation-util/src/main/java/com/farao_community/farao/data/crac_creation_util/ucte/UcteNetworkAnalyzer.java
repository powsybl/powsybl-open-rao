/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util.ucte;

import com.powsybl.iidm.network.*;

import static com.farao_community.farao.data.crac_creation_util.ConnectableType.*;
import static com.farao_community.farao.data.crac_creation_util.ucte.UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS;

/**
 * A utility class, that stores network information so as to speed up
 * the identification of Ucte branches within a Iidm network.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkAnalyzer {

    private final Network network;
    private final UcteConnectableCollection connectablesInNetwork;
    private final UcteNetworkAnalyzerProperties properties;

    public UcteNetworkAnalyzer(Network network, UcteNetworkAnalyzerProperties properties) {
        if (!network.getSourceFormat().equals("UCTE")) {
            throw new IllegalArgumentException("UcteNetworkHelper can only be used for an UCTE network");
        }
        this.network = network;
        this.properties = properties;
        this.connectablesInNetwork = new UcteConnectableCollection(network);
    }

    public Network getNetwork() {
        return network;
    }

    public UcteNetworkAnalyzerProperties getProperties() {
        return properties;
    }

    UcteMatchingResult findContingencyElement(String from, String to, String suffix) {
        return connectablesInNetwork.lookForConnectable(completeNodeName(from), completeNodeName(to), suffix, properties.getBusIdMatchPolicy(),
                INTERNAL_LINE, TIE_LINE, DANGLING_LINE, VOLTAGE_TRANSFORMER, PST, HVDC);
    }

    UcteMatchingResult findCnecElement(String from, String to, String suffix) {
        return connectablesInNetwork.lookForConnectable(completeNodeName(from), completeNodeName(to), suffix, properties.getBusIdMatchPolicy(),
                INTERNAL_LINE, TIE_LINE, DANGLING_LINE, VOLTAGE_TRANSFORMER, PST);
    }

    UcteMatchingResult findTopologicalElement(String from, String to, String suffix) {
        return connectablesInNetwork.lookForConnectable(completeNodeName(from), completeNodeName(to), suffix, properties.getBusIdMatchPolicy(),
                INTERNAL_LINE, TIE_LINE, DANGLING_LINE, VOLTAGE_TRANSFORMER, PST, SWITCH);
    }

    UcteMatchingResult findPstElement(String from, String to, String suffix) {
        return connectablesInNetwork.lookForConnectable(completeNodeName(from), completeNodeName(to), suffix, properties.getBusIdMatchPolicy(), PST);
    }

    UcteMatchingResult findHvdcElement(String from, String to, String suffix) {
        return connectablesInNetwork.lookForConnectable(completeNodeName(from), completeNodeName(to), suffix, properties.getBusIdMatchPolicy(), HVDC);
    }

    private String completeNodeName(String nodeName) {
        if (nodeName.length() == UcteUtils.UCTE_NODE_LENGTH) {
            return nodeName;
        } else if (properties.getBusIdMatchPolicy().equals(COMPLETE_WITH_WILDCARDS)) {
            return String.format("%1$-7s", nodeName) + UcteUtils.WILDCARD_CHARACTER;
        } else {
            return String.format("%1$-8s", nodeName);
        }
    }
}
