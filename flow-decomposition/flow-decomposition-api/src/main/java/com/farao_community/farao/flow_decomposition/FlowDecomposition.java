/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.flow_decomposition_results.FlowDecompositionResults;

import java.util.concurrent.CompletableFuture;

/**
 * Flow decomposition feature interface.
 * Asynchronous computation of flow decomposition is foreseen
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public interface FlowDecomposition {
    CompletableFuture<FlowDecompositionResults> run(String workingStateId, FlowDecompositionParameters parameters, CracFile contingenciesProvider);
}
