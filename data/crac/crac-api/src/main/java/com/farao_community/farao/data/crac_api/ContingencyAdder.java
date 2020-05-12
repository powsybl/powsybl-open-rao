package com.farao_community.farao.data.crac_api;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface ContingencyAdder extends NetworkElementParent {

    /**
     * Set the ID of the contingency
     * @param id: ID to set
     * @return the {@code ContingencyAdder} instance
     */
    ContingencyAdder setId(String id);

    /**
     * Set the name of the contingency
     * @param name: name to set
     * @return the {@code ContingencyAdder} instance
     */
    ContingencyAdder setName(String name);

    /**
     * Add a network element to the contingency
     * @return a {@code NetworkElementAdder<ContingencyAdder>} instance to construct a network element
     */
    NetworkElementAdder<ContingencyAdder> newNetworkElement();

    /**
     * Add the new state to the Crac
     * @return the created {@code Contingency} instance
     */
    Contingency add();
}
