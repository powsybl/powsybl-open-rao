/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoimpl.commons.RaoUtil;
import com.powsybl.openrao.raoimpl.reports.CommonReports;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * OpenRAO main RAO provider dealing with topological, linear, single-timestamped and time-coupled situations.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService({RaoProvider.class})
public class OpenRao implements RaoProvider {
    @Override
    public String getName() {
        return "OpenRAO";
    }

    @Override
    public CompletableFuture<RaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Time-coupled situation detected; using MARMOT algorithm as the computation engine");
        return runMarmot(raoInput, parameters, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode) {
        FastRaoParameters fastRaoParameters = parameters.getExtension(FastRaoParameters.class);
        if (raoInput.getCrac().getInstants(InstantKind.CURATIVE).size() > 1) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Multi-curative situation detected; using CASTOR algorithm as the computation engine");
            return runCastor(raoInput, parameters, targetEndInstant, reportNode);
        } else if (fastRaoParameters == null) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Using CASTOR (search-tree RAO) algorithm as the computation engine");
            return runCastor(raoInput, parameters, targetEndInstant, reportNode);
        } else {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("FastRAO parameters extension found; using FastRAO algorithm as the computation engine");
            return runFastRao(raoInput, parameters, targetEndInstant, reportNode);
        }
    }

    /**
     * Runs the CASTOR (search-tree RAO) engine.
     */
    private CompletableFuture<RaoResult> runCastor(final RaoInput raoInput, final RaoParameters parameters, final Instant targetEndInstant, final ReportNode reportNode) {
        try {
            RaoUtil.initData(raoInput, parameters, reportNode);
        } catch (OpenRaoException e) {
            String failure = String.format("Data initialisation failed: %s", e);
            CommonReports.reportExceptionMessage(reportNode, failure);
            return CompletableFuture.completedFuture(RaoResultImpl.builder(raoInput.getCrac()).build());
        }

        return new CastorFullOptimization(raoInput, parameters, targetEndInstant, reportNode).run();
    }

    /**
     * Runs the FastRAO engine.
     */
    private CompletableFuture<RaoResult> runFastRao(final RaoInput raoInput, final RaoParameters parameters, final Instant targetEndInstant, final ReportNode reportNode) {
        return new FastRao().run(raoInput, parameters, targetEndInstant, reportNode);
    }

    /**
     * Runs the MARMOT engine.
     */
    private CompletableFuture<RaoResult> runMarmot(final TimeCoupledRaoInput timeCoupledRaoInput, final RaoParameters parameters, final ReportNode reportNode) {
        return new Marmot().run(timeCoupledRaoInput, parameters, reportNode);
    }
}
