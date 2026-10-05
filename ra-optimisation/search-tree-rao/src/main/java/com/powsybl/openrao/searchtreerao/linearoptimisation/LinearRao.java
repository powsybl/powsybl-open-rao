/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * RAO Provider that optimized only linear remedial actions (PST, Redispatching).
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService({RaoProvider.class, TimeCoupledRaoProvider.class})
public class LinearRao implements RaoProvider, TimeCoupledRaoProvider {
    private static final String PROVIDER_NAME = "LinearRAO"; // TODO: Use LOUTRE name? (Linear Optimizer Using Transformers and REdispatching)

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public CompletableFuture<TimeCoupledRaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return null;
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode) {
        return null;
    }
}
