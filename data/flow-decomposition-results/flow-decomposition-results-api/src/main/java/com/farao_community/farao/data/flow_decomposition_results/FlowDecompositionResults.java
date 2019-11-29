/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flow_decomposition_results;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Flow decomposition results business object
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionResults {
    private final Map<String, PerBranchResult> perBranchResults = new HashMap<>();

    public Map<String, PerBranchResult> getPerBranchResults() {
        return perBranchResults;
    }

    public synchronized void addPerBranchResult(String branch, PerBranchResult perBranchResult) {
        Objects.requireNonNull(branch);
        Objects.requireNonNull(perBranchResult);
        perBranchResults.put(branch, perBranchResult);
    }

    public synchronized boolean hasPerBranchResult(String branch) {
        Objects.requireNonNull(branch);
        return perBranchResults.containsKey(branch);
    }

    public synchronized PerBranchResult getPerBranchResult(String branch) {
        Objects.requireNonNull(branch);
        return perBranchResults.get(branch);
    }
}
