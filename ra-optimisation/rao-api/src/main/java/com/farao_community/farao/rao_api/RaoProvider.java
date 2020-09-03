/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.data.crac_io_api.RaoInput;
import com.powsybl.commons.Versionable;
import com.powsybl.computation.ComputationManager;

import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RaoProvider extends Versionable {

    /**
     * @param raoInput: Data to optimize. Contains a Crac, a Network, the ID of the current network variant, and more
     * @param computationManager: Computation configuration.
     * @param parameters: RAO parameters.
     * @return A completable future of a RaoComputationResult it gathers all the optimization results.
     */
    CompletableFuture<RaoResult> run(RaoInput raoInput, ComputationManager computationManager, RaoParameters parameters);
}
