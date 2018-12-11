/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.powsybl.commons.Versionable;
import com.powsybl.iidm.network.Network;

import java.util.concurrent.CompletableFuture;

/**
 * Flowbased computation interface
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public interface FlowBasedComputation extends Versionable {

    /**
     * Interface to run the flowbased computation
     *
     * @return FlowBasedComputationResults results of the flowbased computation
     */
    CompletableFuture<FlowBasedComputationResult> run(Network networkIn);

    CompletableFuture<FlowBasedComputationResult> run(String workingStateId, FlowBasedComputationParameters parameters);

}
