/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoimpl.reports.OpenRaoReports;
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
        OpenRaoReports.reportTimeCoupledSituationDetected(reportNode);
        return new Marmot().run(raoInput, parameters, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode) {
        FastRaoParameters fastRaoParameters = parameters.getExtension(FastRaoParameters.class);
        if (raoInput.getCrac().getInstants(InstantKind.CURATIVE).size() > 1) {
            OpenRaoReports.reportMultiCurativeSituationDetected(reportNode);
            return new CastorFullOptimization(raoInput, parameters, targetEndInstant, reportNode).run();
        } else if (fastRaoParameters == null) {
            OpenRaoReports.reportCastorAlgorithmSelected(reportNode);
            return new CastorFullOptimization(raoInput, parameters, targetEndInstant, reportNode).run();
        } else {
            OpenRaoReports.reportFastRaoAlgorithmSelected(reportNode);
            return new FastRao().run(raoInput, parameters, targetEndInstant, reportNode);
        }
    }
}
