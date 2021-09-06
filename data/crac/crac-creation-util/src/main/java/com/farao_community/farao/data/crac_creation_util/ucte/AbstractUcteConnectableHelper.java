package com.farao_community.farao.data.crac_creation_util.ucte;

import java.util.Objects;

import static com.farao_community.farao.data.crac_creation_util.ucte.UcteUtils.*;
import static java.lang.String.format;

abstract public class AbstractUcteConnectableHelper {

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
        if (ucteId.length() < MIN_BRANCH_ID_LENGTH
                || ucteId.length() > MAX_BRANCH_ID_LENGTH) {

            invalidate(String.format("UCTE branch id should contain %d to %d characters (NODE1ID_ NODE2_ID SUFFIX). This id is not valid: %s", MIN_BRANCH_ID_LENGTH, MAX_BRANCH_ID_LENGTH, ucteId));
            return false;

        } else if (!Character.isWhitespace(ucteId.charAt(UCTE_NODE_LENGTH)) ||
                !Character.isWhitespace(ucteId.charAt(UCTE_NODE_LENGTH * 2 + 1))) {

            invalidate(String.format("UCTE branch should be of the form 'NODE1ID_ NODE2_ID SUFFIX'. This id is not valid: %s", ucteId));
            return false;
        } else {
            from = ucteId.substring(0, UCTE_NODE_LENGTH);
            to = ucteId.substring(UCTE_NODE_LENGTH + 1, UCTE_NODE_LENGTH * 2 + 1);
            suffix = ucteId.substring(UCTE_NODE_LENGTH * 2 + 2);
            return true;
        }
    }

}
