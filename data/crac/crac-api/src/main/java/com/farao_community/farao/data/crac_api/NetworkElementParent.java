package com.farao_community.farao.data.crac_api;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface NetworkElementParent {
    /**
     * Add a (or set the) network element using a {@code NetworkElement} object
     * @param networkElement: {@code NetworkElement}
     * @return the added {@code NetworkElement} object
     */
    NetworkElement addNetworkElement(NetworkElement networkElement);
}
