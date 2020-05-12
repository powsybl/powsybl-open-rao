package com.farao_community.farao.data.crac_api;

import com.google.auto.service.AutoService;

/**
 * Mock CracFactory implementation, for unit tests only
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(CracFactory.class)
public class MockCracFactory1 implements CracFactory {
    @Override
    public Crac create(String id, String name) {
        return null;
    }

    @Override
    public Crac create(String id) {
        return null;
    }

    @Override
    public String getName() {
        return "MockCracFactory1";
    }
}
