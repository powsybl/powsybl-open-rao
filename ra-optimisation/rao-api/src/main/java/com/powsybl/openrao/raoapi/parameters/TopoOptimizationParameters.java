/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.reports.RaoApiReports;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.ABSOLUTE_MINIMUM_IMPACT_THRESHOLD;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.RELATIVE_MINIMUM_IMPACT_THRESHOLD;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.TOPOLOGICAL_ACTIONS_OPTIMIZATION_SECTION;

/**
 * Topological actions optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class TopoOptimizationParameters {
    // Default values
    private static final double DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD = 0;
    private static final double DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD = 0;
    // Attributes
    private double relativeMinImpactThreshold;
    private double absoluteMinImpactThreshold;
    private final ReportNode reportNode;

    public TopoOptimizationParameters(final ReportNode reportNode) {
        this.relativeMinImpactThreshold = DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD;
        this.absoluteMinImpactThreshold = DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD;
        this.reportNode = reportNode;
    }

    public void setRelativeMinImpactThreshold(double relativeMinImpactThreshold) {
        if (relativeMinImpactThreshold < 0) {
            RaoApiReports.reportNegativeRelativeMinimumImpactThreshold(reportNode, relativeMinImpactThreshold);
            this.relativeMinImpactThreshold = 0;
        } else if (relativeMinImpactThreshold > 1) {
            RaoApiReports.reportCappingRelativeMinimumImpactThreshold(reportNode, relativeMinImpactThreshold);
            this.relativeMinImpactThreshold = 1;
        } else {
            this.relativeMinImpactThreshold = relativeMinImpactThreshold;
        }
    }

    public void setAbsoluteMinImpactThreshold(double absoluteMinImpactThreshold) {
        this.absoluteMinImpactThreshold = absoluteMinImpactThreshold;
    }

    public double getRelativeMinImpactThreshold() {
        return relativeMinImpactThreshold;
    }

    public double getAbsoluteMinImpactThreshold() {
        return absoluteMinImpactThreshold;
    }

    public static TopoOptimizationParameters load(final PlatformConfig platformConfig, final ReportNode reportNode) {
        Objects.requireNonNull(platformConfig);
        TopoOptimizationParameters parameters = new TopoOptimizationParameters(reportNode);
        platformConfig.getOptionalModuleConfig(TOPOLOGICAL_ACTIONS_OPTIMIZATION_SECTION)
                .ifPresent(config -> {
                    parameters.setRelativeMinImpactThreshold(config.getDoubleProperty(RELATIVE_MINIMUM_IMPACT_THRESHOLD, DEFAULT_RELATIVE_MIN_IMPACT_THRESHOLD));
                    parameters.setAbsoluteMinImpactThreshold(config.getDoubleProperty(ABSOLUTE_MINIMUM_IMPACT_THRESHOLD, DEFAULT_ABSOLUTE_MIN_IMPACT_THRESHOLD));
                });
        return parameters;
    }
}
