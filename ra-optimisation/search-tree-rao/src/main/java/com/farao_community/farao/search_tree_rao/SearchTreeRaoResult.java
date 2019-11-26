/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SearchTreeRaoResult extends AbstractExtension<RaoComputationResult> {

    @Override
    public String getName() {
        return "SearchTreeRaoResult";
    }

    public enum ComputationStatus {
        SECURE,
        UNSECURE,
        ERROR
    }

    public enum StopCriterion {
        OPTIMIZATION_FINISHED,
        NO_COMPUTATION,
        DIVERGENCE,
        TIME_OUT,
        OPTIMIZATION_TIME_OUT
    }

    private final ComputationStatus computationStatus;
    private final StopCriterion stopCriterion;

    public SearchTreeRaoResult(final ComputationStatus computationStatus, final StopCriterion stopCriterion) {
        this.computationStatus = computationStatus;
        this.stopCriterion = stopCriterion;
    }

    public ComputationStatus getComputationStatus() {
        return computationStatus;
    }

    public StopCriterion getStopCriterion() {
        return stopCriterion;
    }

}
