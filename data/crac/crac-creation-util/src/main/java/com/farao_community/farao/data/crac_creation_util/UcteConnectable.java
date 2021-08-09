/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.Identifiable;

import java.util.Set;

import static com.farao_community.farao.data.crac_creation_util.UcteUtils.UCTE_NODE_LENGTH;

/**
 * Contains a Powsybl Identifiable, as well as UCTE information on this identifiable, such as
 * fromNode, toNode, orderCode and element names.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UcteConnectable implements Comparable<UcteConnectable> {

    private Identifiable<?> iidmIdentifiable;

    private String ucteFromNode;
    private String ucteToNode;
    private String ucteOrderCode;
    private Set<String> ucteElementNames; //a set is required here as tie-line in iidm format have two element names

    private Side iidmSide;
    private boolean isIidmConventionInverted; //transformer conventions between iidm and UCTE formats are inverted

    enum Side {
        ONE,
        TWO, // a tie-line contains two half-lines, each half-line point towards one side of the tie-line
        BOTH // used for all elements but tie-lines
    }

    UcteConnectable(String from, String to, String orderCode, Set<String> elementNames, Identifiable<?> iidmConnectable, boolean isIidmConventionInverted) {
        this(from, to, orderCode, elementNames, iidmConnectable, isIidmConventionInverted, Side.BOTH);
    }

    UcteConnectable(String from, String to, String orderCode, Set<String> elementNames, Identifiable<?> iidmConnectable, boolean isIidmConventionInverted, Side side) {
        // TODO : unit tests for ynodes
        this.ucteFromNode = from.replace("YNODE_", "");
        this.ucteToNode = to.replace("YNODE_", "");
        if (this.ucteFromNode.length() != UCTE_NODE_LENGTH || this.ucteToNode.length() != UCTE_NODE_LENGTH) {
            throw new IllegalArgumentException(String.format("from (%s) and to (%s) should have %d characters", this.ucteFromNode, this.ucteToNode, UCTE_NODE_LENGTH));
        }
        this.ucteOrderCode = orderCode;
        this.ucteElementNames = elementNames;
        this.iidmIdentifiable = iidmConnectable;
        this.isIidmConventionInverted = isIidmConventionInverted;
        this.iidmSide = side;
    }

    boolean doesMatch(String from, String to, String suffix) {
        return matchSuffix(suffix) && matchFromTo(from, to);
    }

    UcteMatchingResult getUcteMatchingResult(String from, String to, String suffix) {
        if (!matchSuffix(suffix) || !matchFromTo(from, to)) {
            return UcteMatchingResult.notFound();
        } else {
            return UcteMatchingResult.found(iidmSide, isIidmConventionInverted, iidmIdentifiable);
        }
    }

    @Override
    public String toString() {
        return String.format("%1$-8s %2$-8s %3$s - %4$s - side %5$s", ucteFromNode, ucteToNode, ucteOrderCode, iidmIdentifiable.getId(), iidmSide);
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
                && this.iidmIdentifiable.getId().equals(other.iidmIdentifiable.getId())
                && this.iidmSide.equals(other.iidmSide);
        }
        return false;
    }

    private boolean matchFromTo(String from, String to) {
        return UcteUtils.matchNodeNames(from, this.ucteFromNode) && UcteUtils.matchNodeNames(to, this.ucteToNode);
    }

    private boolean matchSuffix(String suffix) {
        return (suffix.equals(ucteOrderCode)) || (ucteElementNames != null && ucteElementNames.contains(suffix));
    }
}
