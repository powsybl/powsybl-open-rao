package com.farao_community.farao.data.crac_creation_util;

final class UcteUtils {

    static final int UCTE_NODE_LENGTH = 8;
    static final int ELEMENT_NAME_LENGTH = 12;
    static final int MAX_BRANCH_ID_LENGTH = UCTE_NODE_LENGTH * 2 + ELEMENT_NAME_LENGTH + 3;
    static final String TIELINE_SEPARATOR = " + ";
    static final String WILDCARD_CHARACTER = "*";

    private UcteUtils() {
    }

    /**
     * Match a node name to a node name from the network. The node name can contain a wildcard or be shorter than
     * the standard UCTE length
     */
    static boolean matchNodeNames(String nodeName, String nodeNameInNetwork, boolean completeSmallBusIdsWithWildcards) {
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
}
