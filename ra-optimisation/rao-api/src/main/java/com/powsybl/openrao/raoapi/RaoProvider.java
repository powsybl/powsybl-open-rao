/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RaoProvider {
    String getName();

    /**
     * @param raoInput         Data to optimize. Contains a Crac, a Network, the ID of the current network variant, and more
     * @param parameters       RAO parameters.
     * @param reportNode       Parent reportNode.
     * @param raoStopCondition Rao stop condition.
     * @return A completable future of a RaoResult it gathers all the optimization results.
     */
    CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode, RaoStopCondition raoStopCondition);

    /**
     * @param raoInput         Data to optimize. Contains a Crac, a Network, the ID of the current network variant, and more
     * @param parameters       RAO parameters.
     * @param reportNode       Parent reportNode.
     * @return A completable future of a RaoResult it gathers all the optimization results.
     */
    default CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, reportNode, RaoStopCondition.NO_OP);
    }

    /**
     * @param raoInput         Data to optimize. Contains a Crac, a Network, the ID of the current network variant, and more
     * @param parameters       RAO parameters.
     * @param targetEndInstant Instant until which the RAO is allowed to run.
     * @param reportNode       Parent reportNode.
     * @return A completable future of a RaoResult it gathers all the optimization results.
     * @deprecated since 7.6.0, {@code targetEndInstant} can be replaced by {@link EndInstantRaoStopCondition}
     */
    @Deprecated(since = "7.6.0")
    CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters, Instant targetEndInstant, ReportNode reportNode);

    /**
     * @param raoInput         Data to optimize. Contains Cracs and Networks for each timestamp and gradient constraints
     * @param parameters       RAO parameters.
     * @param reportNode       Parent reportNode.
     * @param raoStopCondition Rao stop condition.
     * @return A completable future of a TimeCoupledRaoResult for each timestamp.
     */
    default CompletableFuture<RaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode, RaoStopCondition raoStopCondition) {
        // note: default behavior not to break external implementers
        throw new OpenRaoException("Time-coupled optimization is not supported");
    }

    default CompletableFuture<RaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        return run(raoInput, parameters, reportNode, RaoStopCondition.NO_OP);
    }
}
