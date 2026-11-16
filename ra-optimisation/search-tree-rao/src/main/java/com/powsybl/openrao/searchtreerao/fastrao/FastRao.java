/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.fastrao;

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoimpl.OpenRao;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Deprecated RAO provider façade keeping the historical "FastRao" registry name;
 * it delegates to the default {@link OpenRao} provider (FastRAO engine).
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @deprecated since 7.6.0, use {@link OpenRao} as the default RAO provider.
 */
@AutoService(RaoProvider.class)
@Deprecated(since = "7.6.0")
public class FastRao implements RaoProvider {

    private static final String FAST_RAO = "FastRao";

    // Do not store any big object in this class as it is a static RaoProvider
    // Objects stored in memory will not be released at the end of the RAO run

    @Override
    public String getName() {
        return FAST_RAO;
    }

    @Override
    public CompletableFuture<RaoResult> run(final RaoInput raoInput, final RaoParameters parameters, final ReportNode reportNode) {
        return run(raoInput, parameters, null, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(final RaoInput raoInput, final RaoParameters parameters, final Instant targetEndInstant, final ReportNode reportNode) {
        if (!parameters.hasExtension(FastRaoParameters.class)) {
            parameters.addExtension(FastRaoParameters.class, new FastRaoParameters());
        }
        return new OpenRao().run(raoInput, parameters, targetEndInstant, reportNode);
    }

    @Override
    public CompletableFuture<RaoResult> run(TimeCoupledRaoInput raoInput, RaoParameters parameters, ReportNode reportNode) {
        throw new OpenRaoException("Time-coupled optimization is not supported by FastRAO");
    }
}
