/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Mutable state threaded through the successive steps of {@link CastorFullOptimization#run()}. Each step method
 * reads the fields it needs and fills in the ones it produces; reporting/logging of the results stays in run().
 * <p>
 * Fields are private: they can only be read through the getters below, which fail fast with a clear message
 * if a step is reached before the step that is supposed to populate the field it needs has run.
 *
 * * @author Maxence Fleury {@literal <maxence.fleury at artelys.com>}
 */
final class RaoRunContext {
    @NonNull private final ToolProvider toolProvider;
    @Nullable private StateTree stateTree;
    @Nullable private PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis;
    @Nullable private PrePerimeterResult initialOutput;
    @Nullable private OptimizationResult preventiveResult;
    @Nullable private Long preventiveRaoTime;
    @Nullable private PostPerimeterResult postPreventiveResult;
    @Nullable private PrePerimeterResult preCurativeSensitivityAnalysisOutput;
    @Nullable private CastorContingencyScenarios castorContingencyScenarios;
    @Nullable private Map<State, PostPerimeterResult> postContingencyResults;
    @Nullable private RaoResult mergedRaoResults;
    @Nullable private PostPerimeterResult finalSecondPreventiveResult;
    @Nullable private Map<State, PostPerimeterResult> finalPostContingencyResults;
    @Nullable private Boolean logFinalResultsOutsideOfSecondPreventive;

    @Nullable private ToolProvider workingToolProvider;
    @Nullable private RaoParameters dcRaoParameters;
    @Nullable private PrePerimeterResult workingInitialOutput;
    @Nullable private Boolean useWorkingVars;

    RaoRunContext(@NonNull ToolProvider toolProvider) {
        this.toolProvider = Objects.requireNonNull(toolProvider);
    }

    @NonNull ToolProvider getToolProvider() {
        return toolProvider;
    }

    void setStateTree(@NonNull StateTree stateTree) {
        this.stateTree = stateTree;
    }

    @NonNull StateTree getStateTree() {
        return Objects.requireNonNull(stateTree, "stateTree has not been set yet");
    }

    void setPrePerimeterSensitivityAnalysis(@NonNull PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis) {
        this.prePerimeterSensitivityAnalysis = prePerimeterSensitivityAnalysis;
    }

    @NonNull PrePerimeterSensitivityAnalysis getPrePerimeterSensitivityAnalysis() {
        return Objects.requireNonNull(prePerimeterSensitivityAnalysis, "prePerimeterSensitivityAnalysis has not been computed yet");
    }

    void setInitialOutput(@NonNull PrePerimeterResult initialOutput) {
        this.initialOutput = initialOutput;
    }

    @NonNull PrePerimeterResult getInitialOutput() {
        return Objects.requireNonNull(initialOutput, "initialOutput has not been computed yet");
    }

    void setPreventiveResult(@NonNull OptimizationResult preventiveResult) {
        this.preventiveResult = preventiveResult;
    }

    @NonNull OptimizationResult getPreventiveResult() {
        return Objects.requireNonNull(preventiveResult, "preventiveResult has not been computed yet");
    }

    void setPreventiveRaoTime(long preventiveRaoTime) {
        this.preventiveRaoTime = preventiveRaoTime;
    }

    long getPreventiveRaoTime() {
        return Objects.requireNonNull(preventiveRaoTime, "preventiveRaoTime has not been computed yet");
    }

    void setPostPreventiveResult(@NonNull PostPerimeterResult postPreventiveResult) {
        this.postPreventiveResult = postPreventiveResult;
    }

    @NonNull PostPerimeterResult getPostPreventiveResult() {
        return Objects.requireNonNull(postPreventiveResult, "postPreventiveResult has not been computed yet");
    }

    void setPreCurativeSensitivityAnalysisOutput(@NonNull PrePerimeterResult preCurativeSensitivityAnalysisOutput) {
        this.preCurativeSensitivityAnalysisOutput = preCurativeSensitivityAnalysisOutput;
    }

    @NonNull PrePerimeterResult getPreCurativeSensitivityAnalysisOutput() {
        return Objects.requireNonNull(preCurativeSensitivityAnalysisOutput, "preCurativeSensitivityAnalysisOutput has not been computed yet");
    }

    void setCastorContingencyScenarios(@NonNull CastorContingencyScenarios castorContingencyScenarios) {
        this.castorContingencyScenarios = castorContingencyScenarios;
    }

    @NonNull CastorContingencyScenarios getCastorContingencyScenarios() {
        return Objects.requireNonNull(castorContingencyScenarios, "castorContingencyScenarios has not been computed yet");
    }

    void setPostContingencyResults(@NonNull Map<State, PostPerimeterResult> postContingencyResults) {
        this.postContingencyResults = postContingencyResults;
    }

    @NonNull Map<State, PostPerimeterResult> getPostContingencyResults() {
        return Objects.requireNonNull(postContingencyResults, "postContingencyResults has not been computed yet");
    }

    void setMergedRaoResults(@NonNull RaoResult mergedRaoResults) {
        this.mergedRaoResults = mergedRaoResults;
    }

    @NonNull RaoResult getMergedRaoResults() {
        return Objects.requireNonNull(mergedRaoResults, "mergedRaoResults has not been computed yet");
    }

    void setFinalSecondPreventiveResult(@NonNull PostPerimeterResult finalSecondPreventiveResult) {
        this.finalSecondPreventiveResult = finalSecondPreventiveResult;
    }

    @NonNull PostPerimeterResult getFinalSecondPreventiveResult() {
        return Objects.requireNonNull(finalSecondPreventiveResult, "finalSecondPreventiveResult has not been computed yet");
    }

    void setFinalPostContingencyResults(@NonNull Map<State, PostPerimeterResult> finalPostContingencyResults) {
        this.finalPostContingencyResults = finalPostContingencyResults;
    }

    @NonNull Map<State, PostPerimeterResult> getFinalPostContingencyResults() {
        return Objects.requireNonNull(finalPostContingencyResults, "finalPostContingencyResults has not been computed yet");
    }

    void setLogFinalResultsOutsideOfSecondPreventive(boolean logFinalResultsOutsideOfSecondPreventive) {
        this.logFinalResultsOutsideOfSecondPreventive = logFinalResultsOutsideOfSecondPreventive;
    }

    boolean isLogFinalResultsOutsideOfSecondPreventive() {
        return Objects.requireNonNull(logFinalResultsOutsideOfSecondPreventive, "logFinalResultsOutsideOfSecondPreventive has not been set yet");
    }

    @NonNull Boolean useWorkingVars() {
        return Objects.requireNonNull(useWorkingVars, "useWorkingVars has not been set yet");
    }

    void setUseWorkingVars(boolean useWorkingVars) {
        this.useWorkingVars = useWorkingVars;
    }

    @NonNull PrePerimeterResult getWorkingInitialOutput() {
        return Objects.requireNonNull(workingInitialOutput, "workingInitialOutput has not been computed yet");
    }

    void setWorkingInitialOutput(@NonNull PrePerimeterResult workingInitialOutput) {
        this.workingInitialOutput = workingInitialOutput;
    }

    @NonNull RaoParameters getDcRaoParameters() {
        return Objects.requireNonNull(dcRaoParameters, "dcRaoParameters has not been set yet");
    }

    void setDcRaoParameters(@NonNull RaoParameters dcRaoParameters) {
        this.dcRaoParameters = dcRaoParameters;
    }

    @NonNull ToolProvider getWorkingToolProvider() {
        return Objects.requireNonNull(workingToolProvider, "workingToolProvider has not been set yet");
    }

    void setWorkingToolProvider(@NonNull ToolProvider workingToolProvider) {
        this.workingToolProvider = workingToolProvider;
    }
}
