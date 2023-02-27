/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

import java.util.Objects;
import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * LoadFlow and sensitivity computation parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
// TODO : add sensitivity failure overcost
public class LoadFlowAndSensitivityParameters {
    private String loadFlowProvider;
    private String sensitivityProvider;

    private double sensitivityFailureOvercost;
    private SensitivityAnalysisParameters sensitivityWithLoadFlowParameters;

    private static final String DEFAULT_LOADFLOW_PROVIDER = "OpenLoadFlow";
    private static final String DEFAULT_SENSITIVITY_PROVIDER = "OpenLoadFlow";
    private static final double DEFAULT_SENSITIVITY_FAILURE_OVERCOST = 10000;

    private static final SensitivityAnalysisParameters DEFAULT_SENSI_WITH_LOAD_FLOW_PARAMETERS = new SensitivityAnalysisParameters();

    public LoadFlowAndSensitivityParameters(String loadFlowProvider, String sensitivityProvider, double sensitivityFailureOvercost, SensitivityAnalysisParameters sensitivityWithLoadFlowParameters) {
        this.loadFlowProvider = loadFlowProvider;
        this.sensitivityProvider = sensitivityProvider;
        this.sensitivityFailureOvercost = sensitivityFailureOvercost;
        this.sensitivityWithLoadFlowParameters = sensitivityWithLoadFlowParameters;
    }

    public static LoadFlowAndSensitivityParameters loadDefault() {
        return new LoadFlowAndSensitivityParameters(DEFAULT_LOADFLOW_PROVIDER, DEFAULT_SENSITIVITY_PROVIDER, DEFAULT_SENSITIVITY_FAILURE_OVERCOST, DEFAULT_SENSI_WITH_LOAD_FLOW_PARAMETERS);
    }

    // Getters and setters
    public SensitivityAnalysisParameters getSensitivityWithLoadFlowParameters() {
        return sensitivityWithLoadFlowParameters;
    }

    public void setSensitivityWithLoadFlowParameters(SensitivityAnalysisParameters sensitivityWithLoadFlowParameters) {
        this.sensitivityWithLoadFlowParameters = sensitivityWithLoadFlowParameters;
    }

    public String getLoadFlowProvider() {
        return loadFlowProvider;
    }

    public void setLoadFlowProvider(String loadFlowProvider) {
        this.loadFlowProvider = loadFlowProvider;
    }

    public String getSensitivityProvider() {
        return sensitivityProvider;
    }

    public void setSensitivityProvider(String sensitivityProvider) {
        this.sensitivityProvider = sensitivityProvider;
    }

    public double getSensitivityFailureOvercost() {
        return sensitivityFailureOvercost;
    }

    public void setSensitivityFailureOvercost(double sensitivityFailureOvercost) {
        this.sensitivityFailureOvercost = sensitivityFailureOvercost;
    }

    public static LoadFlowAndSensitivityParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        LoadFlowAndSensitivityParameters parameters = loadDefault();
        platformConfig.getOptionalModuleConfig(LOAD_FLOW_AND_SENSITIVITY_COMPUTATION)
                .ifPresent(config -> {
                    parameters.setLoadFlowProvider(config.getStringProperty(LOAD_FLOW_PROVIDER, DEFAULT_LOADFLOW_PROVIDER));
                    parameters.setSensitivityProvider(config.getStringProperty(SENSITIVITY_PROVIDER, DEFAULT_SENSITIVITY_PROVIDER));
                    parameters.setSensitivityFailureOvercost(config.getDoubleProperty(SENSITIVITY_FAILURE_OVERCOST, DEFAULT_SENSITIVITY_FAILURE_OVERCOST));
                });
        parameters.setSensitivityWithLoadFlowParameters(SensitivityAnalysisParameters.load(platformConfig));
        return parameters;
    }
}