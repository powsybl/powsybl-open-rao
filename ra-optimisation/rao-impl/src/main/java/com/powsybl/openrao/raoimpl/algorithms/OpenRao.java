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
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.raoapi.EndInstantRaoStopCondition;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoInterruptedException;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.RaoStopCondition;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoimpl.reports.OpenRaoReports;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * OpenRAO main RAO provider dealing with topological, linear, single-timestamped and time-coupled situations.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class OpenRao implements RaoProvider {
    @Override
    public String getName() {
        return "OpenRAO";
    }

    @Override
    public CompletableFuture<RaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode, RaoStopCondition raoStopCondition) {
        OpenRaoReports.reportTimeCoupledSituationDetected(reportNode);
        try {
            return new Marmot().run(raoInput, parameters, reportNode, raoStopCondition);
        } catch (RaoInterruptedException interruptedException) {
            return CompletableFuture.completedFuture(
                interruptedException.getRaoResult().orElse(RaoResultImpl.builder(raoInput.getRaoInputs().map(RaoInput::getCrac)).build())
            );
        }
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode, RaoStopCondition raoStopCondition) {
        return run(raoInput, parameters, null, reportNode, raoStopCondition);
    }

    /**
     * @deprecated since 7.6.0, use {@link EndInstantRaoStopCondition} instead of {@code targetEndInstant}.
     */
    @Deprecated(since = "7.6.0")
    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode) {
        return run(raoInput, parameters, targetEndInstant, reportNode, RaoStopCondition.NO_OP);
    }

    /**
     * @deprecated since 7.6.0, use {@link EndInstantRaoStopCondition} instead of {@code targetEndInstant}.
     * This method will be made private in 8.0.0.
     */
    @Deprecated(since = "7.6.0")
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode, RaoStopCondition raoStopCondition) {
        Crac crac = raoInput.getCrac();
        FastRaoParameters fastRaoParameters = parameters.getExtension(FastRaoParameters.class);
        if (raoInput.getCrac().getInstants(InstantKind.CURATIVE).size() > 1) {
            OpenRaoReports.reportMultiCurativeSituationDetected(reportNode);
            return wrapEngineWithInterruptionCheck(crac, () -> new CastorFullOptimization(raoInput, parameters, targetEndInstant, reportNode, raoStopCondition).run());
        } else if (fastRaoParameters == null) {
            OpenRaoReports.reportCastorAlgorithmSelected(reportNode);
            return wrapEngineWithInterruptionCheck(crac, () -> new CastorFullOptimization(raoInput, parameters, targetEndInstant, reportNode, raoStopCondition).run());
        } else {
            OpenRaoReports.reportFastRaoAlgorithmSelected(reportNode);
            return wrapEngineWithInterruptionCheck(crac, () -> new FastRao().run(raoInput, parameters, targetEndInstant, reportNode, raoStopCondition));
        }
    }

    private static CompletableFuture<RaoResult> wrapEngineWithInterruptionCheck(Crac crac, Callable<CompletableFuture<RaoResult>> engine) {
        try {
            return engine.call();
        } catch (RaoInterruptedException interruptedException) {
            return CompletableFuture.completedFuture(interruptedException.getRaoResult().orElse(RaoResultImpl.builder(crac).build()));
        } catch (Exception e) {
            throw new OpenRaoException(e.getMessage());
        }
    }
}
