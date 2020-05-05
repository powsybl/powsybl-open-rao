package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Crac Factory interface.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface CracFactory {

    /**
     * Create a {@code Crac} object.
     * @param id: ID to assign to the created Crac.
     * @param name: name to assign to the created Crac.
     * @param sourceFormat: format of the source file.
     * @return A {@code Crac} instance with given ID, name and source format.
     */
    Crac create(String id, String name, String sourceFormat);

    /**
     * Find a {@code CracFactory} implementation by its name
     *
     * @param factoryName: the name of the {@code CracFactory} implementation.
     * @return An instance of the {@code CracFactory} implementation.
     * @throws FaraoException if the factory name is not recognized as an existent implementation.
     */
    static CracFactory find(String factoryName) {
        ServiceLoader<CracFactoryService> serviceProviders = ServiceLoader.load(CracFactoryService.class);
        for (CracFactoryService provider : serviceProviders) {
            if (provider.getName().equals(factoryName)) {
                return provider.createFactory();
            }
        }
        throw new FaraoException("CracFactoryService implementation name could not be found: " + factoryName);
    }

    /**
     * Get an instance of the default {@code CracFactory} implementation
     *
     * @return An instance of the default {@code CracFactory} implementation.
     * @throws FaraoException if no default has been set and multiple {@code CracFactory} implementations exist.
     */
    static CracFactory findDefault() {
        Optional<ModuleConfig> configOptional = PlatformConfig.defaultConfig().getOptionalModuleConfig("crac");
        if (configOptional.isPresent()) {
            return find(configOptional.get().getStringProperty("default"));
        } else {
            ServiceLoader<CracFactoryService> serviceProviders = ServiceLoader.load(CracFactoryService.class);
            int count = 0;
            for (CracFactoryService provider : serviceProviders) {
                ++count;
            }
            if (count == 1) {
                return serviceProviders.iterator().next().createFactory();
            }
            else if (count == 0) {
                throw new FaraoException("No CracFactoryService implementation found.");
            }
            else {
                throw new FaraoException("No default CracFactoryService implementation set, multiple implementation found.");
            }
        }
    }

    /**
     * Function that returns the name of the implementation
     * @return The name of the CracFactory implementation.
     */
    String getName();
}
