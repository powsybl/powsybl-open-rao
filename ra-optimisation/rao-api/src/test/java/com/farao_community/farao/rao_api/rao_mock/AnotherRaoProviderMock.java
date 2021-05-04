/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.rao_mock;

import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResultImpl;
import com.farao_community.farao.rao_api.results.RaoResult;
import com.google.auto.service.AutoService;

import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot <baptiste.seguinot at rte-france.com>
 */
@AutoService(RaoProvider.class)
public class AnotherRaoProviderMock implements RaoProvider {

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        return CompletableFuture.completedFuture(new RaoResultImpl(RaoResultImpl.Status.FAILURE));
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
