package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;

/**
 * Simple Crac Factory implementation.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SimpleCracFactory implements CracFactory {

    private final String name = "SimpleCracFactory";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Crac create(String id, String name, String sourceFormat) {
        Crac simpleCrac = new SimpleCrac(id, name);
        return simpleCrac;
    }
}
