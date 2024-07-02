/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.raoapi.RaoReports;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

import java.util.Objects;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * LoadFlow and sensitivity computation parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class LoadFlowAndSensitivityParameters {
    private static final String DEFAULT_LOADFLOW_PROVIDER = "OpenLoadFlow";
    private static final String DEFAULT_SENSITIVITY_PROVIDER = "OpenLoadFlow";
    private static final double DEFAULT_SENSITIVITY_FAILURE_OVERCOST = 10000;
    private String loadFlowProvider = DEFAULT_LOADFLOW_PROVIDER;
    private String sensitivityProvider = DEFAULT_SENSITIVITY_PROVIDER;

    private double sensitivityFailureOvercost = DEFAULT_SENSITIVITY_FAILURE_OVERCOST;
    private final ReportNode reportNode;
    private SensitivityAnalysisParameters sensitivityWithLoadFlowParameters;

    public LoadFlowAndSensitivityParameters(ReportNode reportNode) {
        this.reportNode = reportNode;
        this.sensitivityWithLoadFlowParameters = cleanLoadFlowParameters(new SensitivityAnalysisParameters());
    }

    // Getters and setters
    public SensitivityAnalysisParameters getSensitivityWithLoadFlowParameters() {
        return sensitivityWithLoadFlowParameters;
    }

    public void setSensitivityWithLoadFlowParameters(SensitivityAnalysisParameters sensitivityWithLoadFlowParameters) {
        this.sensitivityWithLoadFlowParameters = cleanLoadFlowParameters(sensitivityWithLoadFlowParameters);
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
        if (sensitivityFailureOvercost < 0) {
            RaoReports.reportNegativeSensitivityFailureOvercost(reportNode, sensitivityFailureOvercost);
        }
        this.sensitivityFailureOvercost = Math.abs(sensitivityFailureOvercost);
    }

    public static LoadFlowAndSensitivityParameters load(PlatformConfig platformConfig, ReportNode reportNode) {
        Objects.requireNonNull(platformConfig);
        LoadFlowAndSensitivityParameters parameters = new LoadFlowAndSensitivityParameters(reportNode);
        platformConfig.getOptionalModuleConfig(LOAD_FLOW_AND_SENSITIVITY_COMPUTATION_SECTION)
                .ifPresent(config -> {
                    parameters.setLoadFlowProvider(config.getStringProperty(LOAD_FLOW_PROVIDER, DEFAULT_LOADFLOW_PROVIDER));
                    parameters.setSensitivityProvider(config.getStringProperty(SENSITIVITY_PROVIDER, DEFAULT_SENSITIVITY_PROVIDER));
                    parameters.setSensitivityFailureOvercost(config.getDoubleProperty(SENSITIVITY_FAILURE_OVERCOST, DEFAULT_SENSITIVITY_FAILURE_OVERCOST));
                });
        parameters.setSensitivityWithLoadFlowParameters(SensitivityAnalysisParameters.load(platformConfig));
        return parameters;
    }

    private SensitivityAnalysisParameters cleanLoadFlowParameters(SensitivityAnalysisParameters sensitivityAnalysisParameters) {
        LoadFlowParameters loadFlowParameters = sensitivityAnalysisParameters.getLoadFlowParameters();
        // we have to clean load flow parameters.
        // the slack bus must not be written because it could pollute the sensitivity analyses.
        loadFlowParameters.setWriteSlackBus(false);
        // in DC, as emulation AC is supported for LF but not for sensitivity analyses, it could
        // lead to incoherence.
        if (loadFlowParameters.isDc()) {
            RaoReports.reportDisablingHvdcAcEmulation(reportNode);
            loadFlowParameters.setHvdcAcEmulation(false);
        }
        return sensitivityAnalysisParameters;
    }
}
