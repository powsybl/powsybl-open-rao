/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.roda;

import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.InterTemporalRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.concurrent.CompletableFuture;

/**
 * Robust Optimizer for Dispatch Actions
 */
public class Roda implements InterTemporalRaoProvider {
    @Override
    public CompletableFuture<InterTemporalRaoResult> run(InterTemporalRaoInputWithNetworkPaths raoInput, RaoParameters parameters) {
        return null;
    }

    @Override
    public String getName() {
        return "Roda";
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }
}
