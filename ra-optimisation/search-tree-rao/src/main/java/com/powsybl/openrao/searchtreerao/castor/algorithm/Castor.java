/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.google.auto.service.AutoService;
import com.google.ortools.Loader;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.EndInstantRaoStopCondition;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.RaoStopCondition;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoimpl.algorithms.OpenRao;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Deprecated RAO provider façade keeping the historical "SearchTreeRao" registry name;
 * it delegates to the default {@link OpenRao} provider (CASTOR engine).
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @deprecated since 7.6.0, use {@link OpenRao} as the default RAO provider.
 */
@AutoService(RaoProvider.class)
@Deprecated(since = "7.6.0")
public class Castor implements RaoProvider {

    static {
        try {
            Loader.loadNativeLibraries();
        } catch (UnsatisfiedLinkError e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.error("Native library jniortools could not be loaded. You can ignore this message if it is not needed.");
        }
    }

    private static final String SEARCH_TREE_RAO = "SearchTreeRao";

    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run

    @Override
    public String getName() {
        return SEARCH_TREE_RAO;
    }

    @Override
    public CompletableFuture<RaoResult> run(final RaoInput raoInput, final RaoParameters parameters, final ReportNode reportNode, final RaoStopCondition raoStopCondition) {
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
        if (parameters.hasExtension(FastRaoParameters.class)) {
            parameters.removeExtension(FastRaoParameters.class);
        }
        return new OpenRao().run(raoInput, parameters, targetEndInstant, reportNode, raoStopCondition);
    }

    @Override
    public CompletableFuture<RaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode, RaoStopCondition raoStopCondition) {
        throw new OpenRaoException("Time-coupled optimization is not supported by CASTOR");
    }
}
