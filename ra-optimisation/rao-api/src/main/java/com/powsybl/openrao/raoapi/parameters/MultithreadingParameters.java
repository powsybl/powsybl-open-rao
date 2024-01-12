/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;

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
    private static final int DEFAULT_CURATIVE_LEAVES_IN_PARALLEL = 1;
    private int contingencyScenariosInParallel = DEFAULT_CONTINGENCY_SCENARIOS_IN_PARALLEL;
    private int preventiveLeavesInParallel = DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL;
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

    public void setCurativeLeavesInParallel(int curativeLeavesInParallel) {
        this.curativeLeavesInParallel = curativeLeavesInParallel;
    }

    public int getPreventiveLeavesInParallel() {
        return preventiveLeavesInParallel;
    }

    public int getCurativeLeavesInParallel() {
        return curativeLeavesInParallel;
    }

    public static MultithreadingParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        MultithreadingParameters parameters = new MultithreadingParameters();
        platformConfig.getOptionalModuleConfig(MULTI_THREADING_SECTION)
                .ifPresent(config -> {
                    parameters.setContingencyScenariosInParallel(config.getIntProperty(CONTINGENCY_SCENARIOS_IN_PARALLEL, DEFAULT_CONTINGENCY_SCENARIOS_IN_PARALLEL));
                    parameters.setPreventiveLeavesInParallel(config.getIntProperty(PREVENTIVE_LEAVES_IN_PARALLEL, DEFAULT_PREVENTIVE_LEAVES_IN_PARALLEL));
                    parameters.setCurativeLeavesInParallel(config.getIntProperty(CURATIVE_LEAVES_IN_PARALLEL, DEFAULT_CURATIVE_LEAVES_IN_PARALLEL));

                });
        return parameters;
    }
}
