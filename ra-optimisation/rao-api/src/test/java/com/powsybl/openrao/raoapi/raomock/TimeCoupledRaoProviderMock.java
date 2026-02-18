/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.raomock;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInputWithNetworkPaths;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.concurrent.CompletableFuture;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(TimeCoupledRaoProvider.class)
public class TimeCoupledRaoProviderMock implements TimeCoupledRaoProvider {

    @Override
    public CompletableFuture<TimeCoupledRaoResult> run(TimeCoupledRaoInputWithNetworkPaths raoInput, RaoParameters parameters) {
        return CompletableFuture.completedFuture(new TimeCoupledRaoResultMock());
    }

    @Override
    public String getName() {
        return "RandomInterTemporalRAO";
    }
}
