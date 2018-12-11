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

/**
 * FlowBased computation factory
 * <p>
 * Factory class for FlowBased computation instances
 * </p>
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 * @see FlowBasedComputation
 */
public interface FlowBasedComputationFactory {


    /**
     * @param criticalBranchesValuesProviderIn provider of critical branches values
     * @param glskValuesProviderIn             provider of GLSK values
     * @return an Interface of the Flowbased computation
     */
    FlowBasedComputation create(CriticalBranchesValuesProvider criticalBranchesValuesProviderIn,
                                GlskValuesProvider glskValuesProviderIn);


    FlowBasedComputation create(Network network, CracFile cracFile, ComputationManager computationManager, int i);
}
