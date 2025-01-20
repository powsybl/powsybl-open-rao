/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.InterTemporalRaoProvider;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.searchtreerao.marmot.TopologyChanger.applyPreventiveNetworkActions;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class Marmot implements InterTemporalRaoProvider {
    private static final String INTER_TEMPORAL_RAO = "InterTemporalRao";
    private static final String VERSION = "1.0.0";

    @Override
    public CompletableFuture<TemporalData<RaoResult>> run(InterTemporalRaoInput raoInput, RaoParameters parameters) {
        TemporalData<RaoResult> raoResults = raoInput.getRaoInputs().map(individualRaoInput -> Rao.run(individualRaoInput, parameters));
        if (raoInput.getPowerGradientConstraints().isEmpty()) {
            return CompletableFuture.completedFuture(raoResults);
        }
        applyPreventiveNetworkActions(raoInput.getRaoInputs(), raoResults);

        // TODO: apply topological remedial actions (at least preventive ones, maybe curative too but how?)
        // TODO: run sensitivity analysis on all timestamps
        // TODO: create big MIP with all timestamps
        // TODO: iterate MIP
        // TODO: compile RaoResults by merging topological and linear results
        return CompletableFuture.completedFuture(raoResults);
    }

    @Override
    public String getName() {
        return INTER_TEMPORAL_RAO;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }
}
