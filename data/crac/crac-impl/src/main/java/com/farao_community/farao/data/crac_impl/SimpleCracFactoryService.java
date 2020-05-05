package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.CracFactoryService;

/**
 * Simple Crac Factory service implementation.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SimpleCracFactoryService implements CracFactoryService {

    private final String name = "SimpleCracFactory";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CracFactory createFactory() {
        return new SimpleCracFactory();
    }
}
