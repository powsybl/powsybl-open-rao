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
    private SensitivityComputationResults preSensi;
    private Map<String, Double> preMargin;
    private Map<Contingency, SensitivityComputationResults> contingencySensiMap;
    private Map<Contingency, Map<String, Double> > contingencyMarginsMap;

    public SystematicSensitivityAnalysisResult(SensitivityComputationResults preSensi, Map<String, Double> preMargin, Map<Contingency, SensitivityComputationResults> contingencySensiMap, Map<Contingency, Map<String, Double>> contingencyMarginsMap) {
        this.preSensi = preSensi;
        this.preMargin = preMargin;
        this.contingencySensiMap = contingencySensiMap;
        this.contingencyMarginsMap = contingencyMarginsMap;
    }

    public SensitivityComputationResults getPreSensi() {
        return preSensi;
    }

    public void setPreSensi(SensitivityComputationResults preSensi) {
        this.preSensi = preSensi;
    }

    public Map<Contingency, SensitivityComputationResults> getContingencySensiMap() {
        return contingencySensiMap;
    }

    public void setContingencySensiMap(Map<Contingency, SensitivityComputationResults> contingencySensiMap) {
        this.contingencySensiMap = contingencySensiMap;
    }

    public Map<String, Double> getPreMargin() {
        return preMargin;
    }

    public Map<Contingency, Map<String, Double>> getContingencyMarginsMap() {
        return contingencyMarginsMap;
    }

}
