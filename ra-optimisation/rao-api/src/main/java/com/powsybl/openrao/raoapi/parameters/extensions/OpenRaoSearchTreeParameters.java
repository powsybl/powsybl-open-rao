/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.SEARCH_TREE_PARAMETERS;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class OpenRaoSearchTreeParameters extends AbstractExtension<RaoParameters> {
    private SearchTreeRaoObjectiveFunctionParameters objectiveFunctionParameters;
    private SearchTreeRaoRangeActionsOptimizationParameters rangeActionsOptimizationParameters;
    private SearchTreeRaoTopoOptimizationParameters topoOptimizationParameters;
    private MultithreadingParameters multithreadingParameters;
    private SecondPreventiveRaoParameters secondPreventiveRaoParameters;
    private LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters;
    private Optional<SearchTreeRaoCostlyMinMarginParameters> minMarginsParameters;
    private Optional<SearchTreeRaoMnecParameters> mnecParameters;
    private Optional<SearchTreeRaoRelativeMarginsParameters> relativeMarginsParameters;
    private Optional<SearchTreeRaoLoopFlowParameters> loopFlowParameters;
    private Optional<SearchTreeRaoPstRegulationParameters> pstRegulationParameters;

    public OpenRaoSearchTreeParameters(final ReportNode reportNode) {
        this.objectiveFunctionParameters = new SearchTreeRaoObjectiveFunctionParameters();
        this.rangeActionsOptimizationParameters = new SearchTreeRaoRangeActionsOptimizationParameters();
        this.topoOptimizationParameters = new SearchTreeRaoTopoOptimizationParameters(reportNode);
        this.multithreadingParameters = new MultithreadingParameters();
        this.secondPreventiveRaoParameters = new SecondPreventiveRaoParameters();
        this.loadFlowAndSensitivityParameters = new LoadFlowAndSensitivityParameters(reportNode);
        this.minMarginsParameters = Optional.empty();
        this.mnecParameters = Optional.empty();
        this.relativeMarginsParameters = Optional.empty();
        this.loopFlowParameters = Optional.empty();
        this.pstRegulationParameters = Optional.empty();
    }

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

    public void setPstRegulationParameters(SearchTreeRaoPstRegulationParameters pstRegulationParameters) {
        this.pstRegulationParameters = Optional.of(pstRegulationParameters);
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

    public Optional<SearchTreeRaoPstRegulationParameters> getPstRegulationParameters() {
        return pstRegulationParameters;
    }

    @Override
    public String getName() {
        return SEARCH_TREE_PARAMETERS;
    }

}
