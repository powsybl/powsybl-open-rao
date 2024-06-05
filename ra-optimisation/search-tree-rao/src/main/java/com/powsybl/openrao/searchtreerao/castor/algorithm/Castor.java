/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.parameters.*;
import com.powsybl.openrao.searchtreerao.commons.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.google.auto.service.AutoService;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class Castor implements RaoProvider {
    private static final String SEARCH_TREE_RAO = "SearchTreeRao";

    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run

    @Override
    public String getName() {
        return SEARCH_TREE_RAO;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode) {
        RaoUtil.initData(raoInput, parameters, reportNode);
        ReportNode raoReportNode = CastorAlgorithmReports.reportRao(raoInput.getNetwork().getId(), reportNode);

        // optimization is made on one given state only
        if (raoInput.getOptimizedState() != null) {
            try {
                return new CastorOneStateOnly(raoInput, parameters).run(raoReportNode);
            } catch (Exception e) {
                CastorAlgorithmReports.reportRaoFailure(raoInput.getOptimizedState().getId(), e, reportNode);
                return CompletableFuture.completedFuture(new FailedRaoResultImpl());
            }
        } else {

            // else, optimization is made on all the states
            return new CastorFullOptimization(raoInput, parameters, targetEndInstant).run(raoReportNode);
        }
    }
}
