/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.rao_mock;

import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.google.auto.service.AutoService;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot <baptiste.seguinot at rte-france.com>
 */
@AutoService(RaoProvider.class)
public class AnotherRaoProviderMock implements RaoProvider {

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant) {
        return run(raoInput, parameters);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        RaoResultImpl raoResult = new RaoResultImpl();
        raoResult.setComputationStatus(ComputationStatus.FAILURE);
        return CompletableFuture.completedFuture(raoResult);
    }

    @Override
    public String getName() {
        return "GlobalRAOptimizer";
    }

    @Override
    public String getVersion() {
        return "2.3";
    }
}
