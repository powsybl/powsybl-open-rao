/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Castor;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.marmot.Marmot;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * OpenRAO main RAO provider dealing with topological, linear, single-timestamped and time-coupled situations.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService({RaoProvider.class, TimeCoupledRaoProvider.class})
public class OpenRao implements RaoProvider, TimeCoupledRaoProvider {
    @Override
    public String getName() {
        return "OpenRAO";
    }

    @Override
    public CompletableFuture<RaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Time-coupled situation detected; using MARMOT algorithm as the computation engine");
        return new Marmot().run(raoInput, parameters, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode) {
        FastRaoParameters fastRaoParameters = parameters.getExtension(FastRaoParameters.class);
        RaoProvider provider;
        if (raoInput.getCrac().getInstants(InstantKind.CURATIVE).size() > 1) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Multi-curative situation detected; using CASTOR algorithm as the computation engine");
            provider = new Castor();
        } else if (fastRaoParameters == null) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Using CASTOR (search-tree RAO) algorithm as the computation engine");
            provider = new Castor();
        } else {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("FastRAO parameters extension found; using FastRAO algorithm as the computation engine");
            provider = new FastRao();
        }
        return provider.run(raoInput, parameters, targetEndInstant, reportNode);
    }
}
