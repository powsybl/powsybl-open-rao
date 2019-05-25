/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.SensitivityComputationFactory;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowBasedComputationFactoryImpl implements FlowBasedComputationFactory {

    @Override
    public FlowBasedComputation create(Network network, CracFile cracFile, FlowBasedGlskValuesProvider flowBasedGlskValuesProvider, ComputationManager computationManager, int priority) {
        LoadFlowFactory loadFlowFactory = ComponentDefaultConfig.load().newFactoryImpl(LoadFlowFactory.class);
        SensitivityComputationFactory sensitivityComputationFactory = ComponentDefaultConfig.load().newFactoryImpl(SensitivityComputationFactory.class);
        return new FlowBasedComputationImpl(network, cracFile, flowBasedGlskValuesProvider, computationManager, loadFlowFactory, sensitivityComputationFactory);
    }

}
