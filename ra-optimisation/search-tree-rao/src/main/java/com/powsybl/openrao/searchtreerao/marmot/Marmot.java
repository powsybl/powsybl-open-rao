/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.EndInstantRaoStopCondition;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.RaoStopCondition;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoimpl.algorithms.OpenRao;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Deprecated time-coupled RAO provider façade keeping the historical "TimeCoupledRao" registry name;
 * it delegates to the default {@link OpenRao} provider (MARMOT engine).
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @deprecated since 7.6.0, use {@link OpenRao} as the default RAO provider.
 */
@AutoService({RaoProvider.class, TimeCoupledRaoProvider.class})
@Deprecated(since = "7.6.0")
public class Marmot implements RaoProvider, TimeCoupledRaoProvider {

    private static final String TIME_COUPLED_RAO = "TimeCoupledRao";

    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run

    @Override
    public String getName() {
        return TIME_COUPLED_RAO;
    }

    @Override
    public CompletableFuture<RaoResult> run(final TimeCoupledRaoInput timeCoupledRaoInput,
                                            final RaoParameters parameters,
                                            final ReportNode reportNode,
                                            final RaoStopCondition raoStopCondition) {
        return new OpenRao().run(timeCoupledRaoInput, parameters, reportNode, raoStopCondition);
    }

    @Override
    public CompletableFuture<RaoResult> run(final RaoInput raoInput,
                                            final RaoParameters parameters,
                                            final ReportNode reportNode,
                                            final RaoStopCondition raoStopCondition) {
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
    public CompletableFuture<RaoResult> run(final RaoInput raoInput,
                                            final RaoParameters parameters,
                                            final Instant targetEndInstant,
                                            final ReportNode reportNode,
                                            final RaoStopCondition raoStopCondition) {
        throw new OpenRaoException("MARMOT should not be used for single timestamp cases; use CASTOR instead");
    }
}
