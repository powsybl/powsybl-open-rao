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

    private ComputationStatus computationStatus;
    private StopCriterion stopCriterion;

    public SearchTreeRaoResult() {

    }

    public SearchTreeRaoResult(ComputationStatus c, StopCriterion s) {
        this.computationStatus = c;
        this.stopCriterion = s;
    }

    public ComputationStatus getComputationStatus() {
        return computationStatus;
    }

    public void setComputationStatus(String s) {
        this.computationStatus = Enum.valueOf(ComputationStatus.class, s);
    }

    public void setStopCriterion(String s) {
        this.stopCriterion = Enum.valueOf(StopCriterion.class, s);
    }

    public StopCriterion getStopCriterion() {
        return stopCriterion;
    }

}
