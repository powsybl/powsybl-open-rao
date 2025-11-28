/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.raomock;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.raoresult.api.InterTemporalRaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.InterTemporalRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.concurrent.CompletableFuture;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class InterTemporalRaoProviderMock implements InterTemporalRaoProvider {

    @Override
    public CompletableFuture<InterTemporalRaoResult> run(InterTemporalRaoInputWithNetworkPaths raoInput, RaoParameters parameters) {
        return CompletableFuture.completedFuture(new InterTemporalRaoResultMock());
    }

    @Override
    public String getName() {
        return "RandomInterTemporalRAO";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
