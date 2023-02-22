/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;
import static com.farao_community.farao.rao_api.RaoParametersConstants.*;
/**
 * Second preventive parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SecondPreventiveRaoParameters {
    private ExecutionCondition executionCondition;
    private boolean reOptimizeCurativeRangeActions;
    private boolean hintFromFirstPreventiveRao;
    static final ExecutionCondition DEFAULT_EXECUTION_CONDITION = ExecutionCondition.DISABLED;
    static final boolean DEFAULT_RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS = false;
    static final boolean DEFAULT_HINT_FROM_FIRST_PREVENTIVE_RAO = false;

    public SecondPreventiveRaoParameters(ExecutionCondition executionCondition, boolean reOptimizeCurativeRangeActions, boolean hintFromFirstPreventiveRao) {
        this.executionCondition = executionCondition;
        this.reOptimizeCurativeRangeActions = reOptimizeCurativeRangeActions;
        this.hintFromFirstPreventiveRao = hintFromFirstPreventiveRao;
    }

    public static SecondPreventiveRaoParameters loadDefault() {
        return new SecondPreventiveRaoParameters(DEFAULT_EXECUTION_CONDITION, DEFAULT_RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS, DEFAULT_HINT_FROM_FIRST_PREVENTIVE_RAO);
    }

    public enum ExecutionCondition {
        DISABLED, // do not run 2nd preventive RAO
        POSSIBLE_CURATIVE_IMPROVEMENT, // run 2nd preventive RAO if curative results can be improved, taking into account the curative RAO stop criterion
        COST_INCREASE // run 2nd preventive RAO if curative results can be improved + only if the overall cost has increased during RAO (ie if preventive RAO degraded a curative CNEC's margin or created a curative virtual cost)
    }

    public void setExecutionCondition(ExecutionCondition executionCondition) {
        this.executionCondition = executionCondition;
    }

    public void setReOptimizeCurativeRangeActions(boolean reOptimizeCurativeRangeActions) {
        this.reOptimizeCurativeRangeActions = reOptimizeCurativeRangeActions;
    }

    public void setHintFromFirstPreventiveRao(boolean hintFromFirstPreventiveRao) {
        this.hintFromFirstPreventiveRao = hintFromFirstPreventiveRao;
    }

    public ExecutionCondition getExecutionCondition() {
        return executionCondition;
    }

    public boolean getReOptimizeCurativeRangeActions() {
        return reOptimizeCurativeRangeActions;
    }

    public boolean getHintFromFirstPreventiveRao() {
        return hintFromFirstPreventiveRao;
    }

    public static SecondPreventiveRaoParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        SecondPreventiveRaoParameters parameters = loadDefault();
        platformConfig.getOptionalModuleConfig(SECOND_PREVENTIVE_RAO)
                .ifPresent(config -> {
                    parameters.setExecutionCondition(config.getEnumProperty(EXECUTION_CONDITION, ExecutionCondition.class, DEFAULT_EXECUTION_CONDITION));
                    parameters.setReOptimizeCurativeRangeActions(config.getBooleanProperty(RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS, DEFAULT_RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS));
                    parameters.setHintFromFirstPreventiveRao(config.getBooleanProperty(HINT_FROM_FIRST_PREVENTIVE_RAO, DEFAULT_HINT_FROM_FIRST_PREVENTIVE_RAO));

                });
        return parameters;
    }
}
