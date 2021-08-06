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
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.Set;

import static com.farao_community.farao.data.crac_creation_util.UcteUtils.UCTE_NODE_LENGTH;

/**
 * Represents a UCTE element, defined with a from, to, order code and element names
 * It also has a type name, because from->to and to->from can have different types
 * It also has a isInvertedConvention boolean field, for UCTE from/to elements that are inverted at import by powsybl
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteConnectable implements Comparable<UcteConnectable> {

    private String ucteFromNode;
    private String ucteToNode;
    private String ucteOrderCode;
    private Set<String> ucteElementNames; //a set is required here as tie-line in iidm format have two element names

    private String iidmId;
    private String iidmClassName;
    private Branch.Side iidmSide;
    private boolean isIidmConventionInverted;

    public UcteConnectable(String from, String to, String orderCode, Set<String> elementNames, Identifiable iidmConnectable) {
        this(from, to, orderCode, elementNames, iidmConnectable, Branch.Side.ONE);
    }

    public UcteConnectable(String from, String to, String orderCode, Set<String> elementNames, Identifiable iidmConnectable, Branch.Side side) {
        // TODO : unit tests for ynodes
        this.ucteFromNode = from.replace("YNODE_", "");
        this.ucteToNode = to.replace("YNODE_", "");
        if (this.ucteFromNode.length() != UCTE_NODE_LENGTH || this.ucteToNode.length() != UCTE_NODE_LENGTH) {
            throw new IllegalArgumentException(String.format("from (%s) and to (%s) should have %d characters", this.ucteFromNode, this.ucteToNode, UCTE_NODE_LENGTH));
        }
        this.ucteOrderCode = orderCode;
        this.ucteElementNames = elementNames;
        this.iidmId = iidmConnectable.getId();
        this.iidmClassName = iidmConnectable.getClass().getSimpleName();
        this.iidmSide = side;
        setInversion(iidmConnectable);
    }

    public String getIidmId() {
        return iidmId;
    }

    private void setInversion(Identifiable iidmConnectable) {
        if (iidmConnectable instanceof TwoWindingsTransformer) {
            isIidmConventionInverted = true;
        } else if (iidmConnectable instanceof DanglingLine) {
            // TODO
            isIidmConventionInverted = false;
        } else {
            isIidmConventionInverted = false;
        }
    }

    enum MatchResult {
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

        public static MatchResult getMatchedResult(boolean inverted, Branch.Side side) {
            if (!inverted) {
                if (side.equals(Branch.Side.ONE)) {
                    return MATCHED_ON_SIDE_ONE;
                } else {
                    return MATCHED_ON_SIDE_TWO;
                }
            } else {
                if (side.equals(Branch.Side.ONE)) {
                    return INVERTED_ON_SIDE_ONE;
                } else {
                    return INVERTED_ON_SIDE_TWO;
                }
            }
        }
    }

    public MatchResult tryMatching(String from, String to, String suffix, boolean completeSmallBusIdsWithWildcards) {
        if (!matchSuffix(suffix)) {
            return MatchResult.NOT_MATCHED;
        }
        if (matchFromTo(from, to, completeSmallBusIdsWithWildcards)) {
            return MatchResult.getMatchedResult(false, this.iidmSide);
        } else if (matchFromTo(to, from, completeSmallBusIdsWithWildcards)) {
            return MatchResult.getMatchedResult(true, this.iidmSide);
        } else {
            return MatchResult.NOT_MATCHED;
        }
    }

    private boolean matchFromTo(String from, String to, boolean completeSmallBusIdsWithWildcards) {
        return UcteUtils.matchNodeNames(from, this.ucteFromNode, completeSmallBusIdsWithWildcards) && UcteUtils.matchNodeNames(to, this.ucteToNode, completeSmallBusIdsWithWildcards);
    }

    private boolean matchSuffix(String suffix) {
        return (suffix.equals(ucteOrderCode)) || (ucteElementNames != null && ucteElementNames.contains(suffix));
    }

    /**
     * Match a node name to a node name from the network. The node name can contain a wildcard or be shorter than
     * the standard UCTE length
     */
    private static boolean matchBusNames(String busName, String busNameInNetwork, boolean completeSmallBusIdsWithWildcards) {
        return new UcteBusHelper(busName, busNameInNetwork, completeSmallBusIdsWithWildcards).isValid();
    }

    @Override
    public String toString() {
        return String.format(" %2$-8s %3$s - %4$s - side %5$s", ucteFromNode, ucteToNode, ucteOrderCode, iidmId, iidmSide);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public int compareTo(UcteConnectable o) {
        return this.hashCode() - o.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UcteConnectable) {
            UcteConnectable other = (UcteConnectable) obj;
            return this.ucteFromNode.equals(other.ucteFromNode)
                && this.ucteToNode.equals(other.ucteToNode)
                && this.ucteOrderCode.equals(other.ucteOrderCode)
                && this.ucteElementNames.equals(other.ucteElementNames)
                && this.iidmId.equals(other.iidmId);
        }
        return false;
    }

    //todo: delete if not used
    public boolean isConventionInverted() {
        return isIidmConventionInverted;
    }
}
