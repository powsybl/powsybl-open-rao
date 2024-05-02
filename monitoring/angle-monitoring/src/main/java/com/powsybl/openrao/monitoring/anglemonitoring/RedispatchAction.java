package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.iidm.network.Network;

/**
 * A redispatch action collects a quantity of power to be redispatched (powerToBeRedispatched) in country (countryName)
 * according to glsks that exclude networkElementsToBeExcluded.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface RedispatchAction {
    /**
     * Scales power to redispatch (positive for generation, negative for load) on network.
     */
    void apply(Network network, double powerToRedispatch);
}
