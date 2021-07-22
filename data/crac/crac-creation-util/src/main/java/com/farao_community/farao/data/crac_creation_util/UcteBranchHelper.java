/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TieLine;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

import static java.lang.String.format;

/**
 * UcteBranchHelper is a utility class which manages branches defined with the UCTE convention
 * <p>
 * This utility class has been designed so as to be used in CRAC creators whose format
 * is based on a UCTE network and whose CRAC identifies network elements with the following
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

    private String from;
    private String to;
    private String suffix;

    private boolean isInvertedInNetwork = false;
    private Branch.Side tieLineSide = null;
    private boolean isTieLine = false;

    /**
     * Constructor, based on a separate fields.
     *
     * @param fromNode,      UCTE-id of the origin extremity of the branch
     * @param toNode,        UCTE-id of the destination extremity of the branch
     * @param suffix,        suffix of the branch, either an order code or an elementName
     * @param networkHelper, UcteNetworkHelper object built upon the network
     */
    public UcteBranchHelper(String fromNode, String toNode, String suffix, UcteNetworkHelper networkHelper) {
        super(format("%1$-8s %2$-8s %3$-1s", fromNode, toNode, suffix));
        if (Objects.isNull(fromNode) || Objects.isNull(toNode) || Objects.isNull(suffix)) {
            invalidate("fromNode, toNode and suffix must not be null");
            return;
        }

        this.from = fromNode;
        this.to = toNode;
        this.suffix = suffix;

        interpretWithNetwork(networkHelper);
    }

    /**
     * Constructor, based on a separate fields. Either the order code, or the element name must be
     * non-null. If the two are defined, the suffix which will be used by default is the order code.
     *
     * @param fromNode,      UCTE-id of the origin extremity of the branch
     * @param toNode,        UCTE-id of the destination extremity of the branch
     * @param orderCode,     order code of the branch
     * @param elementName,   element name of the branch
     * @param networkHelper, UcteNetworkHelper object built upon the network
     */
    public UcteBranchHelper(String fromNode, String toNode, String orderCode, String elementName, UcteNetworkHelper networkHelper) {
        super(format("%1$-8s %2$-8s %3$-1s", fromNode, toNode, orderCode != null ? orderCode : elementName));
        if (Objects.isNull(fromNode) || Objects.isNull(toNode)) {
            invalidate("fromNode and toNode must not be null");
            return;
        }

        this.from = fromNode;
        this.to = toNode;

        if (checkSuffix(orderCode, elementName)) {
            interpretWithNetwork(networkHelper);
        }
    }

    /**
     * Constructor, based on a concatenated id.
     *
     * @param ucteBranchId,  concatenated UCTE branch id, of the form "FROMNODE TO__NODE SUFFIX"
     * @param networkHelper, UcteNetworkHelper object built upon the network
     */
    public UcteBranchHelper(String ucteBranchId, UcteNetworkHelper networkHelper) {
        super(ucteBranchId);
        if (Objects.isNull(ucteBranchId)) {
            invalidate("ucteBranchId must not be null");
            return;
        }

        if (decomposeUcteBranchId(ucteBranchId)) {
            interpretWithNetwork(networkHelper);
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

    protected void interpretWithNetwork(UcteNetworkHelper networkHelper) {
        Pair<Identifiable<?>, UcteElement.MatchResult> networkElementMatch = null;
        try {
            networkElementMatch = networkHelper.findNetworkElement(from, to, suffix);
        } catch (Exception e) {
            invalidate(e.getMessage());
            return;
        }

        if (Objects.isNull(networkElementMatch)) {
            invalidate(format("branch was not found in the Network (from: %s, to: %s, suffix: %s)", from, to, suffix));
            return;
        }
        this.isInvertedInNetwork = networkElementMatch.getRight().isInverted();
        this.branchIdInNetwork = networkElementMatch.getLeft().getId();

        Identifiable<?> networkElement = networkElementMatch.getLeft();
        if (networkElement instanceof TieLine) {
            this.isTieLine = true;
            this.tieLineSide = networkElementMatch.getRight().getSide();
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
