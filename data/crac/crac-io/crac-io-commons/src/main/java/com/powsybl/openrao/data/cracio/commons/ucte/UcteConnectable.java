/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.openrao.data.cracio.commons.ConnectableType;
import com.powsybl.iidm.network.Identifiable;

import java.util.Arrays;
import java.util.Set;

/**
 * A UcteConnectable refers to a network element which connect two buses of a UCTE network.
 * For instance, tie-line, dangling-line, transformers and switch are UcteConnectables.
 *
 * The UcteConnectable class store the iidm Identifiable of the Connectable, as well as some
 * UCTE information about this Identifiable (fromNode, toNode, orderCode,...).
 *
 * It is used within the {@link UcteConnectableCollection}
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UcteConnectable implements Comparable<UcteConnectable> {

    private final Identifiable<?> iidmIdentifiable;

    private final String ucteFromNode;
    private final String ucteToNode;
    private final String ucteOrderCode;
    private final Set<String> ucteElementNames; //a set is required here as tie-line in iidm format have two element names

    private final Side iidmSide;
    private final boolean isIidmConventionInverted; //transformer conventions between iidm and UCTE formats are inverted
    private final ConnectableType type;

    enum Side {
        ONE,
        TWO, // a tie-line contains two half-lines, each half-line point towards one side of the tie-line
        BOTH // used for all elements but tie-lines
    }

    UcteConnectable(String from, String to, String orderCode, Set<String> elementNames, Identifiable<?> iidmConnectable, boolean isIidmConventionInverted) {
        this(from, to, orderCode, elementNames, iidmConnectable, isIidmConventionInverted, Side.BOTH);
    }

    UcteConnectable(String from, String to, String orderCode, Set<String> elementNames, Identifiable<?> iidmConnectable, boolean isIidmConventionInverted, Side side) {
        this.ucteFromNode = from;
        this.ucteToNode = to;
        if (this.ucteFromNode.length() != UcteUtils.UCTE_NODE_LENGTH || this.ucteToNode.length() != UcteUtils.UCTE_NODE_LENGTH) {
            throw new IllegalArgumentException(String.format("from (%s) and to (%s) should have %d characters", this.ucteFromNode, this.ucteToNode, UcteUtils.UCTE_NODE_LENGTH));
        }
        this.ucteOrderCode = orderCode;
        this.ucteElementNames = elementNames;
        this.iidmIdentifiable = iidmConnectable;
        this.isIidmConventionInverted = isIidmConventionInverted;
        this.iidmSide = side;
        this.type = ConnectableType.getType(iidmConnectable);
    }

    boolean doesMatch(String from, String to, String suffix, ConnectableType... connectableTypes) {
        return matchSuffix(suffix) && matchFromTo(from, to) && matchType(connectableTypes);
    }

    UcteMatchingResult getUcteMatchingResult(String from, String to, String suffix, ConnectableType... connectableTypes) {
        if (!doesMatch(from, to, suffix, connectableTypes)) {
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
        if (obj instanceof UcteConnectable other) {
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
        return suffix.equals(ucteOrderCode)
            || ucteElementNames != null && ucteElementNames.contains(suffix);
    }

    private boolean matchType(ConnectableType... connectableTypes) {
        return Arrays.stream(connectableTypes).anyMatch(cType -> cType.equals(type));
    }

}
