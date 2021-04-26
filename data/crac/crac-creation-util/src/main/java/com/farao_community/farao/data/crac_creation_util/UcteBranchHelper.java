/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.*;

import java.util.Objects;

import static java.lang.String.format;

/**
 * UcteBranchHelper is a utility class which manages branches defined with the UCTE convention
 *
 * This utility class has been designed so as to be used in CRAC creators whose format
 * is based on a UCTE network and whose CRAC identifies critical branches with the following
 * information: a "from node", a "to node" and a suffix. Either identified in separate fields,
 * or in a common concatenated id such as "FROMNODE TO__NODE SUFFIX".
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteBranchHelper {

    private static final int UCTE_NODE_LENGTH = 8;
    private static final int ELEMENT_NAME_LENGTH = 12;
    private static final int MIN_BRANCH_ID_LENGTH = UCTE_NODE_LENGTH * 2 + 3;
    private static final int MAX_BRANCH_ID_LENGTH = UCTE_NODE_LENGTH * 2 + ELEMENT_NAME_LENGTH + 3;

    private String from;
    private String to;
    private String suffix;

    private boolean isBranchValid = true;
    private String invalidBranchReason = "";
    private String branchIdInNetwork;
    private boolean isInvertedInNetwork;
    private boolean isTieLine = false;
    private Branch.Side tieLineSide = null;

    /**
     * Constructor, based on a separate fields.
     *
     * @param fromNode, UCTE-id of the origin extremity of the branch
     * @param toNode, UCTE-id of the destination extremity of the branch
     * @param suffix, suffix of the branch, either an order code or an elementName
     * @param network, network on which the branch will be looked for, should contain UCTE aliases
     */
    public UcteBranchHelper(String fromNode, String toNode, String suffix, Network network) {

        if (Objects.isNull(fromNode) || Objects.isNull(toNode) || Objects.isNull(suffix)) {
            invalidate("fromNode, toNode and suffix must not be null");
            return;
        }

        this.from = format("%1$-8s", fromNode);
        this.to = format("%1$-8s", toNode);
        this.suffix = suffix;

        findEquivalentElementInNetwork(network);
    }

    /**
     * Constructor, based on a separate fields. Either the order code, or the element name must be
     * non-null. If the two are defined, the suffix which will be used by default is the order code.
     *
     * @param fromNode, UCTE-id of the origin extremity of the branch
     * @param toNode, UCTE-id of the destination extremity of the branch
     * @param orderCode, order code of the branch
     * @param elementName, element name of the branch
     * @param network, network on which the branch will be looked for, should contain UCTE aliases
     */
    public UcteBranchHelper(String fromNode, String toNode, String orderCode, String elementName, Network network) {

        if (Objects.isNull(fromNode) || Objects.isNull(toNode)) {
            invalidate("fromNode and toNode must not be null");
            return;
        }

        this.from = format("%1$-8s", fromNode);
        this.to = format("%1$-8s", toNode);

        if (checkSuffix(orderCode, elementName)) {
            findEquivalentElementInNetwork(network);
        }
    }

    /**
     * Constructor, based on a concatenated id.
     *
     * @param ucteBranchId, concatenated UCTE branch id, of the form "FROMNODE TO__NODE SUFFIX"
     * @param network, network on which the branch will be looked for, should contain UCTE aliases
     */
    public UcteBranchHelper(String ucteBranchId, Network network) {

        if (Objects.isNull(ucteBranchId)) {
            invalidate("ucteBranchId must not be null");
            return;
        }

        if (decomposeUcteBranchId(ucteBranchId)) {
            findEquivalentElementInNetwork(network);
        }
    }

    /**
     * Get from node, as it was originally defined for the branch
     */
    public String getOriginalFrom() {
        return from;
    }

    /**
     * Get to node, as it was originally defined for the branch
     */
    public String getOriginalTo() {
        return to;
    }

    /**
     * Get suffix of the branch
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Returns a boolean indicating whether or not the branch is considered valid in the network
     */
    public boolean isBranchValid() {
        return isBranchValid;
    }

    /**
     * If the branch is not valid, returns the reason why it is considered invalid
     */
    public String getInvalidBranchReason() {
        return invalidBranchReason;
    }

    /**
     * If the branch is valid, returns its corresponding id in the PowSyBl Network
     */
    public String getBranchIdInNetwork() {
        return branchIdInNetwork;
    }

    /**
     * If the branch is valid, returns a boolean indicating whether or not the from/to are
     * inverted in the network, compared to the values originally used in the constructor
     * of the UcteBranchHelper
     */
    public boolean isInvertedInNetwork() {
        return isInvertedInNetwork;
    }

    /**
     * If the branch is valid, returns a boolean indicating whether or not the branch is a tie-line
     */
    public boolean isTieLine() {
        return isTieLine;
    }

    /**
     * If the branch is a valid tie-line, returns a boolean indicating which half of the tie-line is
     * actually defined by the branch definition
     */
    public Branch.Side getTieLineSide() {
        return tieLineSide;
    }

    private boolean checkSuffix(String order, String elementName) {
        if (!Objects.isNull(order) && order.length() > 0) {
            this.suffix = order;
            return true;
        } else if (!Objects.isNull(elementName) && elementName.length() > 0) {
            this.suffix = elementName;
            return true;
        } else {
            invalidate("branch has neither an 'order' nor an 'elementName' field");
            return false;
        }
    }

    private boolean decomposeUcteBranchId(String ucteBranchId) {
        if (ucteBranchId.length() < MIN_BRANCH_ID_LENGTH
            || ucteBranchId.length() > MAX_BRANCH_ID_LENGTH) {

            invalidate(String.format("UCTE branch id should contain %d to %d characters (NODE1ID_ NODE2_ID SUFFIX). This id is not valid: %s", MIN_BRANCH_ID_LENGTH, MAX_BRANCH_ID_LENGTH, ucteBranchId));
            return false;

        } else if (!Character.isWhitespace(ucteBranchId.charAt(UCTE_NODE_LENGTH)) ||
            !Character.isWhitespace(ucteBranchId.charAt(UCTE_NODE_LENGTH * 2 + 1))) {

            invalidate(String.format("UCTE branch should be of the form 'NODE1ID_ NODE2_ID SUFFIX'. This id is not valid: %s", ucteBranchId));
            return false;
        } else {
            from = ucteBranchId.substring(0, UCTE_NODE_LENGTH);
            to = ucteBranchId.substring(UCTE_NODE_LENGTH + 1, UCTE_NODE_LENGTH * 2 + 1);
            suffix = ucteBranchId.substring(UCTE_NODE_LENGTH * 2 + 2);
            return true;
        }
    }

    private void findEquivalentElementInNetwork(Network network) {

         /* It is assumed that the Branches, DanglingLines and TieLines of the network have ids/aliases like below:
        - "UCTNODE1 UCTENODE2 orderCode"
        - "UCTNODE1 UCTENODE2 ELEMENTNAME"

        This is normally the case for a Network imported from a UCTE file, on which the UcteAliasesCreation has been used.

        Another possibility would be to stream all the branches of the network and check the ids of the two Buses on which
        they are connected. It would probably be less efficient, but more flexible, notably for DanglingLines and TieLines.
         */

        Identifiable<?> fromToBranch = network.getIdentifiable(getLineName(from, to, suffix));

        if (!Objects.isNull(fromToBranch)) {
            this.branchIdInNetwork = fromToBranch.getId();
            checkInversion(fromToBranch);
            return;
        }

        Identifiable<?> toFromBranch = network.getIdentifiable(getLineName(to, from, suffix));
        if (!Objects.isNull(toFromBranch)) {
            this.branchIdInNetwork = toFromBranch.getId();
            checkInversion(toFromBranch);
            return;
        }

        invalidate(format("branch was not found in the Network (from: %s, to: %s, suffix: %s)", from, to, suffix));
    }

    private void checkInversion(Identifiable<?> identifiable) {

        if (identifiable instanceof TieLine) {
            this.isTieLine = true;
            checkTieLineInversion((TieLine) identifiable);
        } else if (identifiable instanceof Branch) {
            checkBranchInversion((Branch) identifiable);
        } else if (identifiable instanceof DanglingLine) {
            checkDanglingLineInversion((DanglingLine) identifiable);
        }
        // inversion is not checked for other types of Branch
    }

    private void checkBranchInversion(Branch branch) {
        if (branch.getTerminal1().getBusBreakerView().getConnectableBus().getId().equals(from)
            && branch.getTerminal2().getBusBreakerView().getConnectableBus().getId().equals(to)) {
            this.isInvertedInNetwork = false;
            return;
        }
        if (branch.getTerminal1().getBusBreakerView().getConnectableBus().getId().equals(to)
            && branch.getTerminal2().getBusBreakerView().getConnectableBus().getId().equals(from)) {
            this.isInvertedInNetwork = true;
            return;
        }

        invalidate(format("branch direction couldn't be properly identified in the network (from: %s, to: %s, suffix: %s, networkBranchId: %s)", from, to, suffix, branch.getId()));
    }

    private void checkTieLineInversion(TieLine tieLine) {

         /*
         in UCTE import, the two Half Lines of an interconnection are merged into a TieLine
         For instance, the TieLine "UCTNODE1 X___NODE 1 + UCTNODE2 X___NODE 1" is imported by PowSybl,
         with :
          - half1 = "UCTNODE1 X___NODE 1"
          - half2 = "UCTNODE2 X___NODE 1"

         In that case :
          - if a criticial branch is defined with from = "UCTNODE1" and to = "X___NODE", the threshold
            is ok as "UCTNODE1" is in the first half of the TieLine
          - if a criticial branch is defined with from = "UCTNODE2" and to = "X___NODE", the threshold
            should be inverted as "UCTNODE2" is in the second half of the TieLine
        */

        if (tieLine.getTerminal1().getBusBreakerView().getConnectableBus().getId().equals(from)) {
            this.isInvertedInNetwork = false;
            this.tieLineSide = Branch.Side.ONE;

        } else if (tieLine.getTerminal2().getBusBreakerView().getConnectableBus().getId().equals(to)) {
            this.isInvertedInNetwork = false;
            this.tieLineSide = Branch.Side.TWO;

        } else if (tieLine.getTerminal1().getBusBreakerView().getConnectableBus().getId().equals(to)) {
            this.isInvertedInNetwork = true;
            this.tieLineSide = Branch.Side.ONE;

        } else if (tieLine.getTerminal2().getBusBreakerView().getConnectableBus().getId().equals(from)) {
            this.isInvertedInNetwork = true;
            this.tieLineSide = Branch.Side.TWO;

        } else {
            invalidate(format("tie-line direction couldn't be properly identified in the network (from: %s, to: %s, suffix: %s, networkTieLineId: %s)", from, to, suffix, tieLine.getId()));
        }
    }

    private void checkDanglingLineInversion(DanglingLine danglingLine) {

        /*
         A dangling line is an Injection with a generator convention.
         After an UCTE import, the flow on the dangling line is therefore always from the X_NODE to the other node.
        */

        if (danglingLine.getTerminal().getBusBreakerView().getConnectableBus().getId().equals(from)) {
            this.isInvertedInNetwork = true;
            return;
        }
        if (danglingLine.getTerminal().getBusBreakerView().getConnectableBus().getId().equals(to)) {
            this.isInvertedInNetwork = false;
            return;
        }

        invalidate(format("dangling line direction couldn't be properly identified in the network (from: %s, to: %s, suffix: %s, networkDanglingLineId: %s)", from, to, suffix, danglingLine.getId()));
    }

    private String getLineName(String nodeId1, String nodeId2, String suffix) {
        return format("%1$-8s %2$-8s %3$s", nodeId1, nodeId2, suffix);
    }

    private void invalidate(String invalidReason) {
        this.isBranchValid = false;
        this.invalidBranchReason = invalidReason;
    }
}
