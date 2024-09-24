/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Objects;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
/**
 * Multi-threading optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class MultithreadingParameters {
    private static final int DEFAULT_CONTINGENCY_SCENARIOS_IN_PARALLEL = 1;
    private static final int DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL = 1;
    private static final int DEFAULT_AUTO_LEAVES_IN_PARALLEL = 1;
    private static final int DEFAULT_CURATIVE_LEAVES_IN_PARALLEL = 1;
    private int contingencyScenariosInParallel = DEFAULT_CONTINGENCY_SCENARIOS_IN_PARALLEL;
    private int preventiveLeavesInParallel = DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL;
    private int autoLeavesInParallel = DEFAULT_AUTO_LEAVES_IN_PARALLEL;
    private int curativeLeavesInParallel = DEFAULT_CURATIVE_LEAVES_IN_PARALLEL;

    public int getContingencyScenariosInParallel() {
        return contingencyScenariosInParallel;
    }

    public void setContingencyScenariosInParallel(int contingencyScenariosInParallel) {
        this.contingencyScenariosInParallel = contingencyScenariosInParallel;
    }

    public void setPreventiveLeavesInParallel(int preventiveLeavesInParallel) {
        this.preventiveLeavesInParallel = preventiveLeavesInParallel;
    }

    public void setAutoLeavesInParallel(int autoLeavesInParallel) {
        this.autoLeavesInParallel = autoLeavesInParallel;
    }

    public void setCurativeLeavesInParallel(int curativeLeavesInParallel) {
        this.curativeLeavesInParallel = curativeLeavesInParallel;
    }

    public int getPreventiveLeavesInParallel() {
        return preventiveLeavesInParallel;
    }

    public int getAutoLeavesInParallel() {
        return autoLeavesInParallel;
    }

    public int getCurativeLeavesInParallel() {
        return curativeLeavesInParallel;
    }

    public static MultithreadingParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        MultithreadingParameters parameters = new MultithreadingParameters();
        platformConfig.getOptionalModuleConfig(MULTI_THREADING_SECTION)
                .ifPresent(config -> {
                    int availableCpus = config.getIntProperty(AVAILABLE_CPUS, 1);
                    parameters.setContingencyScenariosInParallel(availableCpus);
                    parameters.setPreventiveLeavesInParallel(availableCpus);
                    parameters.setAutoLeavesInParallel(1);
                    parameters.setCurativeLeavesInParallel(1);
                });
        return parameters;
    }

    public static int getPreventiveLeavesInParallel(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getMultithreadingParameters().getPreventiveLeavesInParallel();
        }
        return DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL;
    }

    public static int getCurativeLeavesInParallel(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getMultithreadingParameters().getCurativeLeavesInParallel();
        }
        return DEFAULT_CURATIVE_LEAVES_IN_PARALLEL;
    }

    public static int getAutoLeavesInParallel(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getMultithreadingParameters().getAutoLeavesInParallel();
        }
        return DEFAULT_AUTO_LEAVES_IN_PARALLEL;
    }

    public static int getContingencyScenariosInParallel(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getMultithreadingParameters().getContingencyScenariosInParallel();
        }
        return DEFAULT_CONTINGENCY_SCENARIOS_IN_PARALLEL;
    }
}
