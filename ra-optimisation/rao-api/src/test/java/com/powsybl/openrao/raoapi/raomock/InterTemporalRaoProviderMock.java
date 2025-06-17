/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.raomock;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.raoapi.InterTemporalRaoInput;
import com.powsybl.openrao.raoapi.InterTemporalRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(InterTemporalRaoProvider.class)
public class InterTemporalRaoProviderMock implements InterTemporalRaoProvider {

    @Override
    public CompletableFuture<TemporalData<RaoResult>> run(InterTemporalRaoInput raoInput, RaoParameters parameters) {
        Map<OffsetDateTime, RaoResult> raoResultPerTimestamp = new HashMap<>();
        for (OffsetDateTime timestamp : raoInput.getTimestampsToRun()) {
            RaoResultImpl raoResult = new RaoResultImpl(raoInput.getRaoInputs().getData(timestamp).orElseThrow().getCrac());
            raoResult.setComputationStatus(ComputationStatus.DEFAULT);
            raoResultPerTimestamp.put(timestamp, raoResult);
        }
        return CompletableFuture.completedFuture(new TemporalDataImpl<>(raoResultPerTimestamp));
    }

    @Override
    public String getName() {
        return "RandomInterTemporalRAO";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
