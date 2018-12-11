/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.afs.local;

import com.google.auto.service.AutoService;
import com.google.common.base.Supplier;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationFactory;
import com.farao_community.farao.flowbased_computation.afs.FlowBasedComputationRunningService;

import java.util.Objects;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@AutoService(ServiceExtension.class)
public class LocalFlowBasedComputationServiceExtension implements ServiceExtension<FlowBasedComputationRunningService> {
    private final Supplier<FlowBasedComputationFactory> factorySupplier;

    public LocalFlowBasedComputationServiceExtension() {
        this(() -> ComponentDefaultConfig.load().newFactoryImpl(FlowBasedComputationFactory.class));
    }

    public LocalFlowBasedComputationServiceExtension(Supplier<FlowBasedComputationFactory> factorySupplier) {
        this.factorySupplier = Objects.requireNonNull(factorySupplier);
    }

    @Override
    public ServiceKey<FlowBasedComputationRunningService> getServiceKey() {
        return new ServiceKey<>(FlowBasedComputationRunningService.class, false);
    }

    @Override
    public FlowBasedComputationRunningService createService(ServiceCreationContext context) {
        return new LocalFlowBasedComputationRunningService(factorySupplier);
    }
}
