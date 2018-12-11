/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.afs;

import com.google.auto.service.AutoService;
import com.google.common.base.Supplier;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.farao_community.farao.flow_decomposition.FlowDecompositionFactory;

import java.util.Objects;

/**
 * Local computation of flow decomposition plugin
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(ServiceExtension.class)
public class LocalFlowDecompositionRunningServiceExtension implements ServiceExtension<FlowDecompositionRunningService> {

    private final Supplier<FlowDecompositionFactory> factorySupplier;

    public LocalFlowDecompositionRunningServiceExtension() {
        this(() -> ComponentDefaultConfig.load().newFactoryImpl(FlowDecompositionFactory.class));
    }

    public LocalFlowDecompositionRunningServiceExtension(Supplier<FlowDecompositionFactory> factorySupplier) {
        this.factorySupplier = Objects.requireNonNull(factorySupplier);
    }

    @Override
    public ServiceKey<FlowDecompositionRunningService> getServiceKey() {
        return new ServiceKey<>(FlowDecompositionRunningService.class, false);
    }

    @Override
    public FlowDecompositionRunningService createService(ServiceCreationContext context) {
        return new LocalFlowDecompositionRunningService(factorySupplier);
    }
}
