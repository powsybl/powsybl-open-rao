/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.rao_mock;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.RaoInput;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResult;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot <baptiste.seguinot at rte-france.com>
 */
@AutoService(RaoProvider.class)
public class AnotherRaoProviderMock implements RaoProvider {

    @Override
    public CompletableFuture<RaoResult> run(Network network, Crac crac, String variantId, ComputationManager computationManager, RaoParameters parameters) {
        return CompletableFuture.completedFuture(new RaoResult(RaoResult.Status.FAILURE));
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, ComputationManager computationManager, RaoParameters parameters) {
        return CompletableFuture.completedFuture(new RaoResult(RaoResult.Status.FAILURE));
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
