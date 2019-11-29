/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.farao_community.farao.flow_decomposition.FlowDecomposition;
import com.farao_community.farao.flow_decomposition.FlowDecompositionFactory;

/**
 * Full Line Decomposition (FLD) factory
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(FlowDecompositionFactory.class)
public class FullLineDecompositionFactory implements FlowDecompositionFactory {
    @Override
    public FlowDecomposition create(Network network, ComputationManager computationManager, int priority) {
        ComponentDefaultConfig defaultConfig = ComponentDefaultConfig.load();
        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        SensitivityComputationFactory sensitivityComputationFactory = defaultConfig.newFactoryImpl(SensitivityComputationFactory.class);
        return new FullLineDecomposition(network, computationManager, loadFlowRunner, sensitivityComputationFactory);
    }
}
