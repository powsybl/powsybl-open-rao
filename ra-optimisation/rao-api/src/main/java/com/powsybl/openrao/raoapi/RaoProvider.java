/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RaoProvider {
    String getName();

    /**
     * @param raoInput Data to optimize. Contains a Crac, a Network, the ID of the current network variant, and more
     * @param parameters RAO parameters.
     * @return A completable future of a RaoComputationResult it gathers all the optimization results.
     */
    CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters);

    CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant);
}
