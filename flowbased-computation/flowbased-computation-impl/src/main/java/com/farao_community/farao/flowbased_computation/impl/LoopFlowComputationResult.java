/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_api.Cnec;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowComputationResult {
    /**
     * Save calculated ptdf and net position. We assume that they remain approximately the same in linear rao.
     * NOTE: we only check preventive state cnec's loopflow threshold, we compute only once loopflow at the very
     * beginning of SearchTreeRao, then we re-use the memorized ptdf and net position for linear rao. We can also
     * re-compute loopflow in linear rao, by calling calculateLoopFlows() on latest Network updated during each iteration.
     */
    private Map<Cnec, Map<String, Double>> ptdfs; //memorize previously calculated ptdf
    private Map<String, Double> netPositions; //memorize previously calculated net postions
    private Map<Cnec, Double> loopFlowShifts; //memorize previously calculated shift = PTDF * Net postions
    private Map<String, Double> loopFlows; //used to update Cnec's loopflow threshold

    public LoopFlowComputationResult() {
        this.ptdfs = new HashMap<>();
        this.netPositions = new HashMap<>();
        this.loopFlowShifts = new HashMap<>();
        this.loopFlows = new HashMap<>();
    }

    public LoopFlowComputationResult(Map<Cnec, Map<String, Double>> ptdfResults, Map<String, Double> referenceNetPositionByCountry, Map<Cnec, Double> loopFlowShifts, Map<String, Double> loopFlows) {
        this.ptdfs = ptdfResults;
        this.netPositions = referenceNetPositionByCountry;
        this.loopFlowShifts = loopFlowShifts;
        this.loopFlows = loopFlows;
    }

    public Map<Cnec, Map<String, Double>> getPtdfs() {
        return ptdfs;
    }

    public void setPtdfs(Map<Cnec, Map<String, Double>> ptdfs) {
        this.ptdfs = ptdfs;
    }

    public Map<String, Double> getNetPositions() {
        return netPositions;
    }

    public void setNetPositions(Map<String, Double> netPositions) {
        this.netPositions = netPositions;
    }

    public Map<String, Double> getLoopFlows() {
        return loopFlows;
    }

    public void setLoopFlows(Map<String, Double> loopFlows) {
        this.loopFlows = loopFlows;
    }

    public Map<Cnec, Double> getLoopFlowShifts() {
        return loopFlowShifts;
    }

    public void setLoopFlowShifts(Map<Cnec, Double> loopFlowShifts) {
        this.loopFlowShifts = loopFlowShifts;
    }
}
