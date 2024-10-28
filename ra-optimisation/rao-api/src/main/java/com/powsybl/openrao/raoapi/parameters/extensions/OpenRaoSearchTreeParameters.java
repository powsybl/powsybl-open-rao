/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.SEARCH_TREE_PARAMETERS;

public class OpenRaoSearchTreeParameters extends AbstractExtension<RaoParameters> {
    private ObjectiveFunctionParameters objectiveFunctionParameters = new ObjectiveFunctionParameters();
    private RangeActionsOptimizationParameters rangeActionsOptimizationParameters = new RangeActionsOptimizationParameters();
    private TopoOptimizationParameters topoOptimizationParameters = new TopoOptimizationParameters();
    private MultithreadingParameters multithreadingParameters = new MultithreadingParameters();
    private SecondPreventiveRaoParameters secondPreventiveRaoParameters = new SecondPreventiveRaoParameters();
    private LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters();
    private Optional<MnecParameters> mnecParameters = Optional.empty();
    private Optional<RelativeMarginsParameters> relativeMarginsParameters = Optional.empty();
    private Optional<LoopFlowParameters> loopFlowParameters = Optional.empty();

    // Getters and setters
    public void setObjectiveFunctionParameters(ObjectiveFunctionParameters objectiveFunctionParameters) {
        this.objectiveFunctionParameters = objectiveFunctionParameters;
    }

    public void setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        this.rangeActionsOptimizationParameters = rangeActionsOptimizationParameters;
    }

    public void setTopoOptimizationParameters(TopoOptimizationParameters topoOptimizationParameters) {
        this.topoOptimizationParameters = topoOptimizationParameters;
    }

    public void setMultithreadingParameters(MultithreadingParameters multithreadingParameters) {
        this.multithreadingParameters = multithreadingParameters;
    }

    public void setSecondPreventiveRaoParameters(SecondPreventiveRaoParameters secondPreventiveRaoParameters) {
        this.secondPreventiveRaoParameters = secondPreventiveRaoParameters;
    }

    public void setLoadFlowAndSensitivityParameters(LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters) {
        this.loadFlowAndSensitivityParameters = loadFlowAndSensitivityParameters;
    }

    public void setMnecParameters(MnecParameters mnecParameters) {
        this.mnecParameters = Optional.of(mnecParameters);
    }

    public void setRelativeMarginsParameters(RelativeMarginsParameters relativeMarginsParameters) {
        this.relativeMarginsParameters = Optional.of(relativeMarginsParameters);
    }

    public void setLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
        this.loopFlowParameters = Optional.of(loopFlowParameters);
    }

    public ObjectiveFunctionParameters getObjectiveFunctionParameters() {
        return objectiveFunctionParameters;
    }

    public RangeActionsOptimizationParameters getRangeActionsOptimizationParameters() {
        return rangeActionsOptimizationParameters;
    }

    public TopoOptimizationParameters getTopoOptimizationParameters() {
        return topoOptimizationParameters;
    }

    public MultithreadingParameters getMultithreadingParameters() {
        return multithreadingParameters;
    }

    public SecondPreventiveRaoParameters getSecondPreventiveRaoParameters() {
        return secondPreventiveRaoParameters;
    }

    public LoadFlowAndSensitivityParameters getLoadFlowAndSensitivityParameters() {
        return loadFlowAndSensitivityParameters;
    }

    public Optional<MnecParameters> getMnecParameters() {
        return mnecParameters;
    }

    public Optional<RelativeMarginsParameters> getRelativeMarginsParameters() {
        return relativeMarginsParameters;
    }

    public Optional<LoopFlowParameters> getLoopFlowParameters() {
        return loopFlowParameters;
    }

    @Override
    public String getName() {
        return SEARCH_TREE_PARAMETERS;
    }

}
