/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.commons.Versionable;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.concurrent.CompletableFuture;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface InterTemporalRaoProvider extends Versionable {

    /**
     * @param raoInput: Data to optimize. Contains Cracs and Networks for each timestamp and gradient constraints
     * @param parameters: RAO parameters.
     * @return A completable future of a RaoComputationResult for each timestamp.
     */
    CompletableFuture<TemporalData<RaoResult>> run(InterTemporalRaoInput raoInput, RaoParameters parameters);
}
