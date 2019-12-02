/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.rao_mock;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot <baptiste.seguinot at rte-france.com>
 */
@AutoService(RaoProvider.class)
public class RaoProviderMock implements RaoProvider {

    @Override
    public CompletableFuture<RaoComputationResult> run(Network network, Crac crac, String variantId, ComputationManager computationManager, RaoParameters parameters) {
        return CompletableFuture.completedFuture(new RaoComputationResult(RaoComputationResult.Status.SUCCESS));
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
