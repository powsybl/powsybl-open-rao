package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.google.auto.service.AutoService;

/**
 * Simple Crac Factory implementation.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(CracFactory.class)
public class SimpleCracFactory implements CracFactory {

    private final static String name = "SimpleCracFactory";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Crac create(String id, String name) {
        return new SimpleCrac(id, name);
    }

    @Override
    public Crac create(String id) {
        return new SimpleCrac(id, id);
    }
}
