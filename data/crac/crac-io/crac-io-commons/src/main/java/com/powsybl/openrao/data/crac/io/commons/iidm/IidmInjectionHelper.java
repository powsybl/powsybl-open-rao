/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.iidm;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class IidmInjectionHelper {

    private IidmInjectionHelper() {
    }

    public static double getCurrentSetpoint(Network network, Map<String, Double> injectionDistributionKeys) {
        List<Double> currentSetpoints = injectionDistributionKeys.entrySet().stream()
            .map(entry -> getInjectionSetpoint(network, entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

        if (currentSetpoints.size() == 1) {
            return currentSetpoints.get(0);
        } else {
            Collections.sort(currentSetpoints);
            if (Math.abs(currentSetpoints.get(0) - currentSetpoints.get(currentSetpoints.size() - 1)) < 1) {
                return currentSetpoints.get(0);
            } else {
                throw new OpenRaoException(String.format("Cannot evaluate reference setpoint of InjectionRangeAction, as the injections are not distributed according to their key"));
            }
        }
    }

    public static double getInjectionSetpoint(Network network, String injectionId, double distributionKey) {
        Generator generator = network.getGenerator(injectionId);
        if (generator != null) {
            return generator.getTargetP() / distributionKey;
        }

        Load load = network.getLoad(injectionId);
        if (load != null) {
            return -load.getP0() / distributionKey;
        }

        if (network.getIdentifiable(injectionId) == null) {
            throw new OpenRaoException(String.format("Injection %s not found in network", injectionId));
        } else {
            throw new OpenRaoException(String.format("%s refers to an object of the network which is not an handled Injection (not a Load, not a Generator)", injectionId));
        }
    }

}
