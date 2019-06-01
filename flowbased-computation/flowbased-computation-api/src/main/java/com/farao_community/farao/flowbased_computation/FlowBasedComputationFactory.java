/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_file.CracFile;

import java.time.Instant;

/**
 * FlowBased computation factory
 * Factory class for FlowBased computation instances
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public interface FlowBasedComputationFactory {

    /**
     * @param network
     * @param cracFile
     * @param flowBasedGlskValuesProvider
     * @param instant
     * @param computationManager
     * @param priority
     * @return
     */
    FlowBasedComputation create(Network network,
                                CracFile cracFile,
                                FlowBasedGlskValuesProvider flowBasedGlskValuesProvider,
                                Instant instant,
                                ComputationManager computationManager,
                                int priority);

    /**
     * @param network
     * @param cracFile
     * @param computationManager
     * @param priority
     * @return
     */
    //Remove this create(., ., ., .), after updating in afs-local.
    FlowBasedComputation create(Network network, CracFile cracFile, ComputationManager computationManager, int priority);
}
