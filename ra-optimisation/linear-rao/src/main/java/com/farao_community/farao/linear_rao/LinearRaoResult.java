/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.rao_api.RaoResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class LinearRaoResult extends AbstractExtension<RaoResult> {

    public enum SystematicSensitivityAnalysisStatus {
        DEFAULT,
        FALLBACK,
        FAILURE
    }

    public enum LpStatus {
        NOT_RUN,
        RUN_OK,
        FAILURE
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SystematicSensitivityAnalysisStatus systematicSensitivityAnalysisStatus;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LpStatus lpStatus;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;

    @Override
    public String getName() {
        return "LinearRaoResult";
    }

    public SystematicSensitivityAnalysisStatus getSystematicSensitivityAnalysisStatus() {
        return systematicSensitivityAnalysisStatus;
    }

    public void setSystematicSensitivityAnalysisStatus(SystematicSensitivityAnalysisStatus systematicSensitivityAnalysisStatus) {
        this.systematicSensitivityAnalysisStatus = systematicSensitivityAnalysisStatus;
    }

    public void setSuccessfulSystematicSensitivityAnalysisStatus(boolean lastSensiIsFallback) {
        this.systematicSensitivityAnalysisStatus = lastSensiIsFallback ? SystematicSensitivityAnalysisStatus.FALLBACK : SystematicSensitivityAnalysisStatus.DEFAULT;
    }

    public LpStatus getLpStatus() {
        return lpStatus;
    }

    public void setLpStatus(LpStatus lpStatus) {
        this.lpStatus = lpStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
