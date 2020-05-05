package com.farao_community.farao.data.crac_api;

/**
 * Crac Factory service interface.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface CracFactoryService {
    /**
     * Function that returns the name of the implementation
     * @return The name of the CracFactory implementation.
     */
    String getName();

    /**
     * Create a {@code CracFactory} instance.
     */
    CracFactory createFactory();
}
