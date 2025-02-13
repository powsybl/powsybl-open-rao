/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.openrao.commons.OpenRaoException;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Crac Factory interface.
 *
 * A CracFactory enables the creation of a Crac object
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface CracFactory {

    /**
     * Create a {@code Crac} object.
     *
     * @param id: ID to assign to the created Crac.
     * @param name: Name to assign to the created Crac.
     * @param timestamp: Timestamp of validity of the Crac.
     * @return the created {@code Crac} instance.
     */
    Crac create(String id, String name, OffsetDateTime timestamp);

    /**
     * Create a {@code Crac} object.
     *
     * @param id: ID to assign to the created Crac.
     * @param name: Name to assign to the created Crac.
     * @return the created {@code Crac} instance.
     */
    default Crac create(String id, String name) {
        return create(id, name, null);
    }

    /**
     * Create a {@code Crac} object. Name will be equal to id.
     *
     * @param id: ID to assign to the created Crac.
     * @return the created {@code Crac} instance with given ID, name equal to ID.
     */
    default Crac create(String id) {
        return create(id, id);
    }

    /**
     * Function that returns the name of the factory implementation
     *
     * @return The name of the CracFactory implementation.
     */
    String getName();

    /**
     * Find a {@code CracFactory} implementation by its name
     *
     * @param factoryName: The name of the {@code CracFactory} implementation.
     * @return An instance of the {@code CracFactory} implementation.
     * @throws OpenRaoException if the factory name is not recognized as an existent implementation.
     */
    static CracFactory find(String factoryName) {
        List<CracFactory> providers = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracFactory.class).getServices()).get();
        if (providers.size() == 1 && factoryName == null) {
            return providers.get(0);
        } else if (factoryName != null) {
            for (CracFactory provider : providers) {
                if (provider.getName().equals(factoryName)) {
                    return provider;
                }
            }
            throw new OpenRaoException("Crac factory '" + factoryName + "' not found");
        } else {
            throw new OpenRaoException("No CracFactory implementation found, or no default implementation set and multiple implementations found.");
        }
    }

    /**
     * Get an instance of the default {@code CracFactory} implementation
     *
     * @return An instance of the default {@code CracFactory} implementation.
     * @throws OpenRaoException if no default has been set and multiple {@code CracFactory} implementations exist.
     */
    static CracFactory findDefault() {
        String factoryName = PlatformConfig.defaultConfig().getOptionalModuleConfig("crac")
                .flatMap(mc -> mc.getOptionalStringProperty("default"))
                .orElse(null);
        return find(factoryName);
    }
}
