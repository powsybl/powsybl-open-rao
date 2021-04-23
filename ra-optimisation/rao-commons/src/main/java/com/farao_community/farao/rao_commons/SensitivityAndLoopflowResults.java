/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SensitivityAndLoopflowResults {
    private SystematicSensitivityResult systematicSensitivityResult;
    private Map<BranchCnec, Double> commercialFlows;

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.commercialFlows = null;
    }

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult, Map<BranchCnec, Double> commercialFlows) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.commercialFlows = commercialFlows;
    }

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult, LoopFlowResult loopFlowResult, Set<BranchCnec> loopflowCnecs) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.commercialFlows = new HashMap<>();
        loopflowCnecs.forEach(cnec -> commercialFlows.put(cnec, loopFlowResult.getCommercialFlow(cnec)));
    }

    public SystematicSensitivityResult getSystematicSensitivityResult() {
        return systematicSensitivityResult;
    }

    public double getCommercialFlow(BranchCnec cnec) {
        return this.commercialFlows.get(cnec);
    }

    public Map<BranchCnec, Double> getCommercialFlows() {
        return commercialFlows;
    }

    public double getLoopflow(BranchCnec cnec) {
        return systematicSensitivityResult.getReferenceFlow(cnec) - this.commercialFlows.get(cnec);
    }
}
