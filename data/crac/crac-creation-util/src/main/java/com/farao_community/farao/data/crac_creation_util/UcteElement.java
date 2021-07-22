/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.Set;

/**
 * Represents a UCTE element, defined with a from, to, order code and element names
 * It also has a type name, because from->to and to->from can have different types
 * It also has a isInvertedConvention boolean field, for UCTE from/to elements that are inverted at import by powsybl
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteElement {
    private static final int UCTE_NODE_LENGTH = 8;

    private String from;
    private String to;
    private String orderCode;
    private Set<String> elementNames;
    private String typeName;
    private Branch.Side side;
    private boolean isConventionInverted;

    public UcteElement(String from, String to, String orderCode, Set<String> elementNames, Class type) {
        this(from, to, orderCode, elementNames, type, Branch.Side.ONE);
    }

    public UcteElement(String from, String to, String orderCode, Set<String> elementNames, Class type, Branch.Side side) {
        // TODO : unit tests for ynodes
        this.from = from.replace("YNODE_", "");
        this.to = to.replace("YNODE_", "");
        if (this.from.length() != UCTE_NODE_LENGTH || this.to.length() != UCTE_NODE_LENGTH) {
            throw new IllegalArgumentException(String.format("from (%s) and to (%s) should have %d characters", this.from, this.to, UCTE_NODE_LENGTH));
        }
        this.orderCode = orderCode;
        this.elementNames = elementNames;
        this.typeName = type.getSimpleName();
        this.side = side;
        setInversion(type);
    }

    private void setInversion(Class type) {
        if (TwoWindingsTransformer.class.isAssignableFrom(type)) {
            isConventionInverted = true;
        } else if (DanglingLine.class.isAssignableFrom(type)) {
            // TODO
            isConventionInverted = false;
        } else {
            isConventionInverted = false;
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
            return MatchResult.getMatchedResult(false, this.side);
        } else if (matchFromTo(to, from, completeSmallBusIdsWithWildcards)) {
            return MatchResult.getMatchedResult(true, this.side);
        } else {
            return MatchResult.NOT_MATCHED;
        }
    }

    private boolean matchFromTo(String from, String to, boolean completeSmallBusIdsWithWildcards) {
        return matchBusNames(from, this.from, completeSmallBusIdsWithWildcards) && matchBusNames(to, this.to, completeSmallBusIdsWithWildcards);
    }

    private boolean matchSuffix(String suffix) {
        return (suffix.equals(orderCode)) || (elementNames != null && elementNames.contains(suffix));
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
        return String.format("%1$-8s %2$-8s %3$s - %4$s - side %5$s", from, to, orderCode, typeName, side);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UcteElement) {
            UcteElement other = (UcteElement) obj;
            return this.from.equals(other.from)
                && this.to.equals(other.to)
                && this.orderCode.equals(other.orderCode)
                && this.typeName.equals(other.typeName)
                && this.side.equals(other.side);
        }
        return false;
    }

    public boolean isConventionInverted() {
        return isConventionInverted;
    }
}
