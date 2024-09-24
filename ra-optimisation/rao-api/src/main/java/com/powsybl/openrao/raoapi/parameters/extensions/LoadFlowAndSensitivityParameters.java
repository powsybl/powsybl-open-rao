/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;

import java.util.Objects;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
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
    private SensitivityAnalysisParameters sensitivityWithLoadFlowParameters = cleanLoadFlowParameters(new SensitivityAnalysisParameters());

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
            BUSINESS_WARNS.warn("The value {} for `sensitivity-failure-overcost` is smaller than 0. This would encourage the optimizer to make the loadflow diverge. Thus, it will be set to + {}", sensitivityFailureOvercost, -sensitivityFailureOvercost);
        }
        this.sensitivityFailureOvercost = Math.abs(sensitivityFailureOvercost);
    }

    public static LoadFlowAndSensitivityParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        LoadFlowAndSensitivityParameters parameters = new LoadFlowAndSensitivityParameters();
        platformConfig.getOptionalModuleConfig(LOAD_FLOW_AND_SENSITIVITY_COMPUTATION_SECTION)
                .ifPresent(config -> {
                    parameters.setLoadFlowProvider(config.getStringProperty(LOAD_FLOW_PROVIDER, DEFAULT_LOADFLOW_PROVIDER));
                    parameters.setSensitivityProvider(config.getStringProperty(SENSITIVITY_PROVIDER, DEFAULT_SENSITIVITY_PROVIDER));
                    parameters.setSensitivityFailureOvercost(config.getDoubleProperty(SENSITIVITY_FAILURE_OVERCOST, DEFAULT_SENSITIVITY_FAILURE_OVERCOST));
                });
        parameters.setSensitivityWithLoadFlowParameters(SensitivityAnalysisParameters.load(platformConfig));
        return parameters;
    }

    public static String getLoadFlowProvider(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getLoadFlowProvider();
        }
        return DEFAULT_LOADFLOW_PROVIDER;
    }

    public static double getSensitivityFailureOvercost(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost();
        }
        return DEFAULT_SENSITIVITY_FAILURE_OVERCOST;
    }

    public static String getSensitivityProvider(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityProvider();
        }
        return DEFAULT_SENSITIVITY_PROVIDER;
    }

    // TODO: do not set if default...
    public static SensitivityAnalysisParameters getSensitivityWithLoadFlowParameters(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters();
        }
        return cleanLoadFlowParameters(new SensitivityAnalysisParameters());
    }

    private static SensitivityAnalysisParameters cleanLoadFlowParameters(SensitivityAnalysisParameters sensitivityAnalysisParameters) {
        LoadFlowParameters loadFlowParameters = sensitivityAnalysisParameters.getLoadFlowParameters();
        // we have to clean load flow parameters.
        // the slack bus must not be written because it could pollute the sensitivity analyses.
        loadFlowParameters.setWriteSlackBus(false);
        // in DC, as emulation AC is supported for LF but not for sensitivity analyses, it could
        // lead to incoherence.
        if (loadFlowParameters.isDc() && loadFlowParameters.isHvdcAcEmulation()) {
            BUSINESS_WARNS.warn("The runs are in DC but the HvdcAcEmulation parameter is on: this is not compatible." +
                "HvdcAcEmulation parameter set to false.");
            loadFlowParameters.setHvdcAcEmulation(false);
        }
        return sensitivityAnalysisParameters;
    }
}
