package com.farao_community.farao.data.crac_api;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface NetworkElementAdder<T extends NetworkElementParent> {
    /**
     * Set the ID of the network element
     * @param id: ID to set
     * @return the {@code NetworkElementAdder} instance
     */
    NetworkElementAdder<T> setId(String id);

    /**
     * Set the name of the network element
     * @param name: name to set
     * @return the {@code NetworkElementAdder} instance
     */
    NetworkElementAdder<T> setName(String name);

    /**
     * Add the new network element to the parent
     * @return the {@code NetworkElementParent} parent
     */
    T add();
}
