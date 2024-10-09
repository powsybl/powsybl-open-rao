/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.ucte;

import java.util.Objects;

import static java.lang.String.format;

/**
 * Common abstract class for all UcteConnectableHelper
 *
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractUcteConnectableHelper {

    protected String connectableId;
    protected String from;
    protected String to;
    protected String suffix;

    protected boolean isValid = true;
    protected String invalidReason;

    protected String connectableIdInNetwork;

    AbstractUcteConnectableHelper(String fromNode, String toNode, String suffix) {
        connectableId = format("%1$-8s %2$-8s %3$-1s", fromNode, toNode, suffix);
        if (Objects.isNull(fromNode) || Objects.isNull(toNode) || Objects.isNull(suffix)) {
            invalidate("fromNode, toNode and suffix must not be null");
            return;
        }

        this.from = fromNode;
        this.to = toNode;
        this.suffix = suffix;
    }

    AbstractUcteConnectableHelper(String fromNode, String toNode, String orderCode, String elementName) {
        connectableId = format("%1$-8s %2$-8s %3$-1s", fromNode, toNode, orderCode != null ? orderCode : elementName);

        if (Objects.isNull(fromNode) || Objects.isNull(toNode)) {
            invalidate("fromNode and toNode must not be null");
            return;
        }

        this.from = fromNode;
        this.to = toNode;

        checkSuffix(orderCode, elementName);
    }

    AbstractUcteConnectableHelper(String ucteId) {
        connectableId = ucteId;
        if (Objects.isNull(ucteId)) {
            invalidate("ucteId must not be null");
            return;
        }
        decomposeUcteId(ucteId);
    }

    /**
     * Get ucteId, as it was originally defined for the ucte element
     */
    public String getUcteId() {
        return connectableId;
    }

    /**
     * Get from node, as it was originally defined for the ucte element
     */
    public String getOriginalFrom() {
        return from;
    }

    /**
     * Get to node, as it was originally defined for the ucte element
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
     * Returns a boolean indicating if the connectable is valid
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * If the connectable is not valid, returns the reason why
     */
    public String getInvalidReason() {
        return invalidReason;
    }

    /**
     * If the connectable is valid, returns its id in the iidm network
     */
    public String getIdInNetwork() {
        return connectableIdInNetwork;
    }

    protected void invalidate(String invalidReason) {
        this.isValid = false;
        this.invalidReason = invalidReason;
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

    private boolean decomposeUcteId(String ucteId) {
        if (ucteId.length() < UcteUtils.MIN_BRANCH_ID_LENGTH
                || ucteId.length() > UcteUtils.MAX_BRANCH_ID_LENGTH) {

            invalidate(format("UCTE branch id should contain %d to %d characters (NODE1ID_ NODE2_ID SUFFIX). This id is not valid: %s", UcteUtils.MIN_BRANCH_ID_LENGTH, UcteUtils.MAX_BRANCH_ID_LENGTH, ucteId));
            return false;

        } else if (!Character.isWhitespace(ucteId.charAt(UcteUtils.UCTE_NODE_LENGTH)) ||
                !Character.isWhitespace(ucteId.charAt(UcteUtils.UCTE_NODE_LENGTH * 2 + 1))) {

            invalidate(String.format("UCTE branch should be of the form 'NODE1ID_ NODE2_ID SUFFIX'. This id is not valid: %s", ucteId));
            return false;
        } else {
            from = ucteId.substring(0, UcteUtils.UCTE_NODE_LENGTH);
            to = ucteId.substring(UcteUtils.UCTE_NODE_LENGTH + 1, UcteUtils.UCTE_NODE_LENGTH * 2 + 1);
            suffix = ucteId.substring(UcteUtils.UCTE_NODE_LENGTH * 2 + 2);
            return true;
        }
    }

}
