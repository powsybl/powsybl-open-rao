/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.api.util.converters;

import com.farao_community.farao.data.glsk.api.AbstractGlskRegisteredResource;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class NetworkUtil {
    private static final double MINIMAL_ABS_POWER_VALUE = 1e-5;

    private NetworkUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static double pseudoTargetP(Generator generator) {
        return Math.max(MINIMAL_ABS_POWER_VALUE, Math.abs(generator.getTargetP()));
    }

    static double pseudoP0(Load load) {
        return Math.max(MINIMAL_ABS_POWER_VALUE, Math.abs(load.getP0()));
    }

    static List<AbstractGlskRegisteredResource> getAvailableGeneratorsAsResources(List<AbstractGlskRegisteredResource> registeredResources, Network network) {
        return registeredResources.stream()
                .filter(generatorResource -> network.getGenerator(generatorResource.getGeneratorId()) != null)
                .filter(generatorResource -> NetworkUtil.isCorrectGenerator(network.getGenerator(generatorResource.getGeneratorId())))
                .filter(generatorResource -> {
                    double targetP = network.getGenerator(generatorResource.getGeneratorId()).getTargetP();
                    return targetP >= getMinimumGeneratorCapacity(generatorResource, network) && targetP <= getMaximumGeneratorCapacity(generatorResource, network);
                })
                .collect(Collectors.toList());
    }

    static double getMaximumGeneratorCapacity(AbstractGlskRegisteredResource resource, Network network) {
        Generator generator = network.getGenerator(resource.getGeneratorId());
        return resource.getMaximumCapacity().orElse(generator.getMaxP());
    }

    static double getMinimumGeneratorCapacity(AbstractGlskRegisteredResource resource, Network network) {
        Generator generator = network.getGenerator(resource.getGeneratorId());
        return resource.getMinimumCapacity().orElse(generator.getMinP());
    }

    static List<Generator> getAvailableGenerators(List<AbstractGlskRegisteredResource> registeredResources, Network network) {
        return NetworkUtil.getAvailableGeneratorsAsResources(registeredResources, network).stream()
                .map(AbstractGlskRegisteredResource::getGeneratorId)
                .map(network::getGenerator)
                .collect(Collectors.toList());
    }

    static List<Generator> getCountryGenerators(Country country, Network network) {
        return network.getGeneratorStream()
                .filter(generator -> country.equals(generator.getTerminal().getVoltageLevel().getSubstation().getNullableCountry()))
                .filter(NetworkUtil::isCorrectGenerator)
                .filter(generator -> {
                    double targetP = generator.getTargetP();
                    return targetP >= generator.getMinP() && targetP <= generator.getMaxP();
                })
                .collect(Collectors.toList());
    }

    static List<AbstractGlskRegisteredResource> getAvailableLoadsAsResources(List<AbstractGlskRegisteredResource> registeredResources, Network network) {
        return registeredResources.stream()
                .filter(loadResource -> network.getLoad(loadResource.getLoadId()) != null)
                .filter(loadResource -> NetworkUtil.isCorrectLoad(network.getLoad(loadResource.getLoadId())))
                .collect(Collectors.toList());
    }

    static List<Load> getAvailableLoads(List<AbstractGlskRegisteredResource> registeredResources, Network network) {
        return NetworkUtil.getAvailableLoadsAsResources(registeredResources, network).stream()
                .map(AbstractGlskRegisteredResource::getLoadId)
                .map(network::getLoad)
                .collect(Collectors.toList());
    }

    static List<Load> getCountryLoads(Country country, Network network) {
        return network.getLoadStream()
                .filter(load -> country.equals(load.getTerminal().getVoltageLevel().getSubstation().getNullableCountry()))
                .filter(NetworkUtil::isCorrectLoad)
                .collect(Collectors.toList());
    }

    static boolean isCorrectGenerator(Generator generator) {
        return generator.getTerminal().isConnected() &&
                generator.getTerminal().getBusView().getBus() != null &&
                generator.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }

    static boolean isCorrectLoad(Load load) {
        return load.getTerminal().isConnected() &&
                load.getTerminal().getBusView().getBus() != null &&
                load.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }
}
