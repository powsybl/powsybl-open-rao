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

import java.time.Instant;

/**
 * Flowbased computation Factory implementation
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationFactoryImpl implements FlowBasedComputationFactory {

    /**
     * @param network reference network: we need a network to construct the linear glsk map from the glsk document
     * @param cracFile crac file
     * @param flowBasedGlskValuesProvider get linear glsk map from a glsk document
     * @param instant flow based domaine is time dependent
     * @param computationManager computation manager
     * @param priority priority
     * @return
     */
    @Override
    public FlowBasedComputation create(Network network,
                                       CracFile cracFile,
                                       FlowBasedGlskValuesProvider flowBasedGlskValuesProvider,
                                       Instant instant,
                                       ComputationManager computationManager,
                                       int priority) {
        LoadFlowFactory loadFlowFactory = ComponentDefaultConfig.load().newFactoryImpl(LoadFlowFactory.class);
        SensitivityComputationFactory sensitivityComputationFactory = ComponentDefaultConfig.load().newFactoryImpl(SensitivityComputationFactory.class);
        return new FlowBasedComputationImpl(network, cracFile, flowBasedGlskValuesProvider, instant, computationManager, loadFlowFactory, sensitivityComputationFactory);
    }

    /**
     * @param network reference network: we need a network to construct the linear glsk map from the glsk document
     * @param cracFile crac file
     * @param computationManager computation manager
     * @param priority priority
     * @return
     */
    @Override
    public FlowBasedComputation create(Network network, CracFile cracFile, ComputationManager computationManager, int priority) {
        LoadFlowFactory loadFlowFactory = ComponentDefaultConfig.load().newFactoryImpl(LoadFlowFactory.class);
        SensitivityComputationFactory sensitivityComputationFactory = ComponentDefaultConfig.load().newFactoryImpl(SensitivityComputationFactory.class);
        return new FlowBasedComputationImpl(network, cracFile, new FlowBasedGlskValuesProvider(), Instant.now(), computationManager, loadFlowFactory, sensitivityComputationFactory);
    }

}
