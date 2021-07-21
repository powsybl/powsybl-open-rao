/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.*;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * UcteBranchHelper is a utility class which manages branches defined with the UCTE convention
 * <p>
 * This utility class has been designed so as to be used in CRAC creators whose format
 * is based on a UCTE network and whose CRAC identifies critical branches with the following
 * information: a "from node", a "to node" and a suffix. Either identified in separate fields,
 * or in a common concatenated id such as "FROMNODE TO__NODE SUFFIX".
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteBranchHelper extends BranchHelper {

    private static final int UCTE_NODE_LENGTH = 8;
    private static final int ELEMENT_NAME_LENGTH = 12;
    private static final int MIN_BRANCH_ID_LENGTH = UCTE_NODE_LENGTH * 2 + 3;
    private static final int MAX_BRANCH_ID_LENGTH = UCTE_NODE_LENGTH * 2 + ELEMENT_NAME_LENGTH + 3;
    private static final String TIELINE_SEPARATOR = " + ";

    private String from;
    private String to;
    private String suffix;

    private boolean isInvertedInNetwork;
    private Branch.Side tieLineSide = null;
    private boolean isTieLine = false;

    private enum BranchMatchResult {
        NOT_MATCHED,
        MATCHED_ON_SIDE_ONE,
        MATCHED_ON_SIDE_TWO,
        INVERTED_ON_SIDE_ONE,
        INVERTED_ON_SIDE_TWO;

        public boolean matched() {
            return !this.equals(NOT_MATCHED);
        }

        public boolean isInverted() {
            return this.equals(INVERTED_ON_SIDE_ONE) || this.equals(INVERTED_ON_SIDE_TWO);
        }

        public Branch.Side getSide() {
            if (this.equals(MATCHED_ON_SIDE_ONE) || this.equals(INVERTED_ON_SIDE_ONE)) {
                return Branch.Side.ONE;
            } else if (this.equals(MATCHED_ON_SIDE_TWO) || this.equals(INVERTED_ON_SIDE_TWO)) {
                return Branch.Side.TWO;
            } else {
                return null;
            }
        }
    }

    /**
     * Constructor, based on a separate fields.
     *
     * @param fromNode, UCTE-id of the origin extremity of the branch
     * @param toNode,   UCTE-id of the destination extremity of the branch
     * @param suffix,   suffix of the branch, either an order code or an elementName
     * @param network,  network on which the branch will be looked for, should contain UCTE aliases
     */
    public UcteBranchHelper(String fromNode, String toNode, String suffix, Network network) {
        super(format("%1$-8s %2$-8s %3$-1s", fromNode, toNode, suffix));
        if (Objects.isNull(fromNode) || Objects.isNull(toNode) || Objects.isNull(suffix)) {
            invalidate("fromNode, toNode and suffix must not be null");
            return;
        }

        this.from = fromNode;
        this.to = toNode;
        this.suffix = suffix;

        interpretWithNetwork(network);
    }

    /**
     * Constructor, based on a separate fields. Either the order code, or the element name must be
     * non-null. If the two are defined, the suffix which will be used by default is the order code.
     *
     * @param fromNode,    UCTE-id of the origin extremity of the branch
     * @param toNode,      UCTE-id of the destination extremity of the branch
     * @param orderCode,   order code of the branch
     * @param elementName, element name of the branch
     * @param network,     network on which the branch will be looked for, should contain UCTE aliases
     */
    public UcteBranchHelper(String fromNode, String toNode, String orderCode, String elementName, Network network) {
        super(format("%1$-8s %2$-8s %3$-1s", fromNode, toNode, orderCode != null ? orderCode : elementName));
        if (Objects.isNull(fromNode) || Objects.isNull(toNode)) {
            invalidate("fromNode and toNode must not be null");
            return;
        }

        this.from = fromNode;
        this.to = toNode;

        if (checkSuffix(orderCode, elementName)) {
            interpretWithNetwork(network);
        }
    }

    /**
     * Constructor, based on a concatenated id.
     *
     * @param ucteBranchId, concatenated UCTE branch id, of the form "FROMNODE TO__NODE SUFFIX"
     * @param network,      network on which the branch will be looked for, should contain UCTE aliases
     */
    public UcteBranchHelper(String ucteBranchId, Network network) {
        super(ucteBranchId);
        if (Objects.isNull(ucteBranchId)) {
            invalidate("ucteBranchId must not be null");
            return;
        }

        if (decomposeUcteBranchId(ucteBranchId)) {
            interpretWithNetwork(network);
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
     * If the branch is valid, returns a boolean indicating whether or not the from/to are
     * inverted in the network, compared to the values originally used in the constructor
     * of the UcteBranchHelper
     */
    public boolean isInvertedInNetwork() {
        return isInvertedInNetwork;
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

    @Override
    protected void interpretWithNetwork(Network network) {
        Identifiable<?> networkElement = findEquivalentElementInNetwork(network);

        if (Objects.isNull(networkElement)) {
            return;
        }

        if (networkElement instanceof TieLine) {
            this.isTieLine = true;
            checkBranchNominalVoltage((TieLine) networkElement);
            checkTieLineCurrentLimits((TieLine) networkElement);

        } else if (networkElement instanceof Branch) {
            checkBranchNominalVoltage((Branch) networkElement);
            checkBranchCurrentLimits((Branch) networkElement);

        } else if (networkElement instanceof DanglingLine) {
            checkDanglingLineNominalVoltage((DanglingLine) networkElement);
            checkDanglingLineCurrentLimits((DanglingLine) networkElement);
        }
    }

    @Override
    protected Identifiable<?> findEquivalentElementInNetwork(Network network) {
        Identifiable<?> matchedElement = null;
        BranchMatchResult matchedBranchMatchResult = null;
        Set<Identifiable> identifiables = Stream.of(network.getBranchStream(), network.getDanglingLineStream(), network.getSwitchStream())
            .reduce(Stream::concat)
            .orElseGet(Stream::empty)
            .collect(Collectors.toSet());
        for (Identifiable<?> identifiable : identifiables) {
            BranchMatchResult branchMatchResult = matchBranch(identifiable);
            if (branchMatchResult != BranchMatchResult.NOT_MATCHED) {
                if (Objects.isNull(matchedElement)) {
                    matchedElement = identifiable;
                    matchedBranchMatchResult = branchMatchResult;
                } else {
                    invalidate(format("too many branches match the branch in the network (from: %s, to: %s, suffix: %s), for example %s and %s", from, to, suffix, matchedElement.getId(), identifiable.getId()));
                    return null;
                }
            }
        }
        if (Objects.isNull(matchedElement)) {
            invalidate(format("branch was not found in the Network (from: %s, to: %s, suffix: %s)", from, to, suffix));
            return null;
        } else {
            this.branchIdInNetwork = matchedElement.getId();
            this.isInvertedInNetwork = matchedBranchMatchResult.isInverted();
            this.tieLineSide = matchedBranchMatchResult.getSide();
            return matchedElement;
        }
    }

    /**
     * Match an identifiable to the line defined with from/to/suffix
     *
     * @param identifiable the identifiable to match
     * @return a BranchMatchResult indicating if and how the branches match
     */
    private BranchMatchResult matchBranch(Identifiable<?> identifiable) {
        // First match from & to
        BranchMatchResult matchResult = matchBranchFromTo(identifiable);
        if (matchResult == BranchMatchResult.NOT_MATCHED) {
            return matchResult;
        }
        // then match suffix
        if (getOrderCode(identifiable, matchResult.getSide()).equals(suffix) || (getElementNames(identifiable).contains(suffix))) {
            return matchResult;
        } else {
            return BranchMatchResult.NOT_MATCHED;
        }
    }

    /**
     * Match an identifiable to the line upon from/to fields only
     *
     * @param identifiable the identifiable to match
     * @return a BranchMatchResult indicating if and how the branches match
     */
    private BranchMatchResult matchBranchFromTo(Identifiable<?> identifiable) {
        if (identifiable instanceof TieLine) {
            String node1 = ((TieLine) identifiable).getTerminal1().getBusBreakerView().getConnectableBus().getId();
            String node2 = ((TieLine) identifiable).getTerminal2().getBusBreakerView().getConnectableBus().getId();
            String xnode = ((TieLine) identifiable).getUcteXnodeCode();
            return matchFromToWithXnode(node1, node2, xnode);
        } else if (identifiable instanceof DanglingLine) {
            // A dangling line is an Injection with a generator convention.
            // After an UCTE import, the flow on the dangling line is therefore always from the X_NODE to the other node.
            String node = ((DanglingLine) identifiable).getTerminal().getBusBreakerView().getConnectableBus().getId();
            String xnode = ((DanglingLine) identifiable).getUcteXnodeCode();
            return matchFromTo(xnode, node);
        } else if (identifiable instanceof Branch) {
            String node1 = ((Branch<?>) identifiable).getTerminal1().getBusBreakerView().getConnectableBus().getId();
            String node2 = ((Branch<?>) identifiable).getTerminal2().getBusBreakerView().getConnectableBus().getId();
            return matchFromTo(node1, node2);
        } else if (identifiable instanceof Switch) {
            String node1 = ((Switch) identifiable).getVoltageLevel().getBusBreakerView().getBus1(identifiable.getId()).getId();
            String node2 = ((Switch) identifiable).getVoltageLevel().getBusBreakerView().getBus2(identifiable.getId()).getId();
            return matchFromTo(node1, node2);
        } else {
            return BranchMatchResult.NOT_MATCHED;
        }
    }

    /**
     * Match the line's from-to to given networkNode1-networkNode2 or networkNode2-networkNode1
     *
     * @return a BranchMatchResult indicating if and how the branches match
     */
    private BranchMatchResult matchFromTo(String networkNode1, String networkNode2) {
        if (matchNodeNames(from, networkNode1) && matchNodeNames(to, networkNode2)) {
            return BranchMatchResult.MATCHED_ON_SIDE_ONE;
        } else if (matchNodeNames(from, networkNode2) && matchNodeNames(to, networkNode1)) {
            return BranchMatchResult.INVERTED_ON_SIDE_ONE;
        } else {
            return BranchMatchResult.NOT_MATCHED;
        }
    }

    /**
     * Match the line's from-to-xnode to given networkNode1-xnode-networkNode2 with multiple possible directions on the two sides of the xnode
     *
     * @return a BranchMatchResult indicating if and how the branches match
     */
    private BranchMatchResult matchFromToWithXnode(String networkNode1, String networkNode2, String networkXnode) {
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
        if (matchNodeNames(from, networkNode1) && matchNodeNames(to, networkXnode)) {
            return BranchMatchResult.MATCHED_ON_SIDE_ONE;
        } else if (matchNodeNames(from, networkXnode) && matchNodeNames(to, networkNode2)) {
            return BranchMatchResult.MATCHED_ON_SIDE_TWO;
        } else if (matchNodeNames(from, networkXnode) && matchNodeNames(to, networkNode1)) {
            return BranchMatchResult.INVERTED_ON_SIDE_ONE;
        } else if (matchNodeNames(from, networkNode2) && matchNodeNames(to, networkXnode)) {
            return BranchMatchResult.INVERTED_ON_SIDE_TWO;
        } else {
            return BranchMatchResult.NOT_MATCHED;
        }
    }

    /**
     * Match a node name to a node name from the network. The node name can contain a wildcard or be shorter than
     * the standard UCTE length
     */
    private static boolean matchNodeNames(String nodeName, String nodeNameInNetwork) {
        return new UcteBusHelper(nodeName, nodeNameInNetwork).isValid();
    }

    /**
     * Get the order code for an identifiable, on a given side (side is important for tie lines)
     */
    private static String getOrderCode(Identifiable<?> identifiable, Branch.Side side) {
        String connectableId;
        if (identifiable.getId().length() > MAX_BRANCH_ID_LENGTH) {
            int separator = identifiable.getId().indexOf(TIELINE_SEPARATOR);
            connectableId = side.equals(Branch.Side.ONE) ? identifiable.getId().substring(0, separator) : identifiable.getId().substring(separator + TIELINE_SEPARATOR.length());
        } else {
            connectableId = identifiable.getId();
        }
        return connectableId.substring(UCTE_NODE_LENGTH * 2 + 2);
    }

    /**
     * Get all the element names of an identifiable
     */
    private static Set<String> getElementNames(Identifiable<?> identifiable) {
        return identifiable.getPropertyNames().stream()
            .filter(propertyName -> propertyName.startsWith("elementName"))
            .map(identifiable::getProperty)
            .collect(Collectors.toSet());
    }

    private void checkTieLineCurrentLimits(TieLine tieLine) {
        if (!Objects.isNull(tieLine.getCurrentLimits(Branch.Side.ONE))) {
            this.currentLimitLeft = tieLine.getCurrentLimits(Branch.Side.ONE).getPermanentLimit();
        }
        if (!Objects.isNull(tieLine.getCurrentLimits(Branch.Side.TWO))) {
            this.currentLimitRight = tieLine.getCurrentLimits(Branch.Side.TWO).getPermanentLimit();
        }
        if (Objects.isNull(tieLine.getCurrentLimits(Branch.Side.ONE)) && Objects.isNull(tieLine.getCurrentLimits(Branch.Side.TWO))) {
            invalidate(String.format("couldn't identify current limits of tie-line (from: %s, to: %s, suffix: %s, networkTieLineId: %s)", from, to, suffix, tieLine.getId()));
        }
    }

    /**
     * If the branch is valid, returns a boolean indicating whether or not the branch is a tie-line
     */
    public boolean isTieLine() {
        return isTieLine;
    }
}
