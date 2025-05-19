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

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class OpenRaoSearchTreeParameters extends AbstractExtension<RaoParameters> {
    private SearchTreeRaoObjectiveFunctionParameters objectiveFunctionParameters = new SearchTreeRaoObjectiveFunctionParameters();
    private SearchTreeRaoRangeActionsOptimizationParameters rangeActionsOptimizationParameters = new SearchTreeRaoRangeActionsOptimizationParameters();
    private SearchTreeRaoTopoOptimizationParameters topoOptimizationParameters = new SearchTreeRaoTopoOptimizationParameters();
    private MultithreadingParameters multithreadingParameters = new MultithreadingParameters();
    private SecondPreventiveRaoParameters secondPreventiveRaoParameters = new SecondPreventiveRaoParameters();
    private LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters();
    private Optional<SearchTreeRaoCostlyMinMarginParameters> minMarginsParameters = Optional.empty();
    private Optional<SearchTreeRaoMnecParameters> mnecParameters = Optional.empty();
    private Optional<SearchTreeRaoRelativeMarginsParameters> relativeMarginsParameters = Optional.empty();
    private Optional<SearchTreeRaoLoopFlowParameters> loopFlowParameters = Optional.empty();

    // Getters and setters
    public void setObjectiveFunctionParameters(SearchTreeRaoObjectiveFunctionParameters objectiveFunctionParameters) {
        this.objectiveFunctionParameters = objectiveFunctionParameters;
    }

    public void setRangeActionsOptimizationParameters(SearchTreeRaoRangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        this.rangeActionsOptimizationParameters = rangeActionsOptimizationParameters;
    }

    public void setTopoOptimizationParameters(SearchTreeRaoTopoOptimizationParameters topoOptimizationParameters) {
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

    public void setMnecParameters(SearchTreeRaoMnecParameters mnecParameters) {
        this.mnecParameters = Optional.of(mnecParameters);
    }

    public void setRelativeMarginsParameters(SearchTreeRaoRelativeMarginsParameters relativeMarginsParameters) {
        this.relativeMarginsParameters = Optional.of(relativeMarginsParameters);
    }

    public void setLoopFlowParameters(SearchTreeRaoLoopFlowParameters loopFlowParameters) {
        this.loopFlowParameters = Optional.of(loopFlowParameters);
    }

    public void setMinMarginsParameters(SearchTreeRaoCostlyMinMarginParameters minMarginsParameters) {
        this.minMarginsParameters = Optional.of(minMarginsParameters);
    }

    public SearchTreeRaoObjectiveFunctionParameters getObjectiveFunctionParameters() {
        return objectiveFunctionParameters;
    }

    public SearchTreeRaoRangeActionsOptimizationParameters getRangeActionsOptimizationParameters() {
        return rangeActionsOptimizationParameters;
    }

    public SearchTreeRaoTopoOptimizationParameters getTopoOptimizationParameters() {
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

    public Optional<SearchTreeRaoCostlyMinMarginParameters> getMinMarginsParameters() {
        return minMarginsParameters;
    }

    public Optional<SearchTreeRaoMnecParameters> getMnecParameters() {
        return mnecParameters;
    }

    public Optional<SearchTreeRaoRelativeMarginsParameters> getRelativeMarginsParameters() {
        return relativeMarginsParameters;
    }

    public Optional<SearchTreeRaoLoopFlowParameters> getLoopFlowParameters() {
        return loopFlowParameters;
    }

    @Override
    public String getName() {
        return SEARCH_TREE_PARAMETERS;
    }

}
