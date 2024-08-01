/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.ucte;

/**
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
final class UcteUtils {

    static final int UCTE_NODE_LENGTH = 8;
    static final int ELEMENT_NAME_LENGTH = 12;
    static final int MIN_BRANCH_ID_LENGTH = UCTE_NODE_LENGTH * 2 + 3;
    static final int MAX_BRANCH_ID_LENGTH = UCTE_NODE_LENGTH * 2 + ELEMENT_NAME_LENGTH + 3;
    static final String TIELINE_SEPARATOR = " + ";
    static final String WILDCARD_CHARACTER = "*";

    private UcteUtils() {
    }

    /**
     * Match a node name to a node name from the network. The node name can contain a wildcard or be shorter than
     * the standard UCTE length
     */
    static boolean matchNodeNames(String nodeName, String nodeNameInNetwork) {
        if (nodeName.length() < UCTE_NODE_LENGTH) {
            return nodeNameInNetwork.equals(String.format("%1$-8s", nodeName));
        } else if (nodeName.endsWith(WILDCARD_CHARACTER)) {
            return nodeNameInNetwork.substring(0, nodeNameInNetwork.length() - 1).equals(nodeName.substring(0, nodeName.length() - 1));
        } else {
            return nodeNameInNetwork.equals(nodeName);
        }
    }
}
