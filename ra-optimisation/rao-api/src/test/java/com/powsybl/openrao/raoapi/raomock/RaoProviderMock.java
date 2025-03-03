/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.raomock;

import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.google.auto.service.AutoService;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class RaoProviderMock implements RaoProvider {

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        RaoResultImpl raoResult = new RaoResultImpl(raoInput.getCrac());
        raoResult.setComputationStatus(ComputationStatus.DEFAULT);
        return CompletableFuture.completedFuture(raoResult);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
        return run(raoInput, parameters);
    }

    @Override
    public String getName() {
        return "RandomRAO";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
