/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.data.crac_api.Contingency;
import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.Map;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityAnalysisResult {
    private SensitivityComputationResults precontingencyResult;
    private Map<String, Double> preReferenceMargin;
    private Map<Contingency, SensitivityComputationResults> resultMap;
    private Map<Contingency, Map<String, Double> > contingencyReferenceMarginsMap;

    public SystematicSensitivityAnalysisResult(SensitivityComputationResults precontingencyResult, Map<String, Double> preReferenceMargin, Map<Contingency, SensitivityComputationResults> contingencySensitivityComputationResultsMap, Map<Contingency, Map<String, Double>> contingencyReferenceMarginsMap) {
        this.precontingencyResult = precontingencyResult;
        this.preReferenceMargin = preReferenceMargin;
        this.resultMap = contingencySensitivityComputationResultsMap;
        this.contingencyReferenceMarginsMap = contingencyReferenceMarginsMap;
    }

    public SensitivityComputationResults getPrecontingencyResult() {
        return precontingencyResult;
    }

    public void setPrecontingencyResult(SensitivityComputationResults precontingencyResult) {
        this.precontingencyResult = precontingencyResult;
    }

    public Map<Contingency, SensitivityComputationResults> getResultMap() {
        return resultMap;
    }

    public void setResultMap(Map<Contingency, SensitivityComputationResults> resultMap) {
        this.resultMap = resultMap;
    }

    public Map<String, Double> getPreReferenceMargin() {
        return preReferenceMargin;
    }

    public void setPreReferenceMargin(Map<String, Double> preReferenceMargin) {
        this.preReferenceMargin = preReferenceMargin;
    }

    public Map<Contingency, Map<String, Double>> getContingencyReferenceMarginsMap() {
        return contingencyReferenceMarginsMap;
    }

    public void setContingencyReferenceMarginsMap(Map<Contingency, Map<String, Double>> contingencyReferenceMarginsMap) {
        this.contingencyReferenceMarginsMap = contingencyReferenceMarginsMap;
    }
}
