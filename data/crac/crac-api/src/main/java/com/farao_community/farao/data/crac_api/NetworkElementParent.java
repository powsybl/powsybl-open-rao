package com.farao_community.farao.data.crac_api;

public interface NetworkElementParent {
    /**
     * Add a (or set the) network element using a {@code NetworkElement} object
     * @param networkElement: {@code NetworkElement}
     * @return the added {@code NetworkElement} object
     */
    NetworkElement addNetworkElement(NetworkElement networkElement);
}
