/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.afs.local;

import com.farao_community.farao.ra_optimisation.afs.RaoComputationRunningService;
import com.google.auto.service.AutoService;
import com.google.common.base.Supplier;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.farao_community.farao.ra_optimisation.RaoComputationFactory;

import java.util.Objects;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@AutoService(ServiceExtension.class)
public class LocalRaoComputationServiceExtension implements ServiceExtension<RaoComputationRunningService> {
    private final Supplier<RaoComputationFactory> factorySupplier;

    public LocalRaoComputationServiceExtension() {
        this(() -> ComponentDefaultConfig.load().newFactoryImpl(RaoComputationFactory.class));
    }

    public LocalRaoComputationServiceExtension(Supplier<RaoComputationFactory> factorySupplier) {
        this.factorySupplier = Objects.requireNonNull(factorySupplier);
    }

    @Override
    public ServiceKey<RaoComputationRunningService> getServiceKey() {
        return new ServiceKey<>(RaoComputationRunningService.class, false);
    }

    @Override
    public RaoComputationRunningService createService(ServiceCreationContext context) {
        return new LocalRaoComputationRunningService(factorySupplier);
    }
}
