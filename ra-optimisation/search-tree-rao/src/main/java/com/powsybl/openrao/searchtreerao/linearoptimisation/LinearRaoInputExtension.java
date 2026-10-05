/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class LinearRaoInputExtension extends AbstractExtension<RaoInput> implements Extension<RaoInput> {
    private AppliedRemedialActions appliedRemedialActions;
    private FlowResult initialFlowResult;
    private PrePerimeterResult prePerimeterResult;
    private PrePerimeterResult preOptimizationResult;

    public LinearRaoInputExtension() {
        this.appliedRemedialActions = null;
        this.initialFlowResult = null;
        this.prePerimeterResult = null;
        this.preOptimizationResult = null;
    }

    public Optional<AppliedRemedialActions> getAppliedRemedialActions() {
        return Optional.ofNullable(appliedRemedialActions);
    }

    public void setAppliedRemedialActions(AppliedRemedialActions appliedRemedialActions) {
        this.appliedRemedialActions = appliedRemedialActions;
    }

    public Optional<FlowResult> getInitialFlowResult() {
        return Optional.ofNullable(initialFlowResult);
    }

    public void setInitialFlowResult(FlowResult initialFlowResult) {
        this.initialFlowResult = initialFlowResult;
    }

    public Optional<PrePerimeterResult> getPrePerimeterResult() {
        return Optional.ofNullable(prePerimeterResult);
    }

    public void setPrePerimeterResult(PrePerimeterResult prePerimeterResult) {
        this.prePerimeterResult = prePerimeterResult;
    }

    public Optional<PrePerimeterResult> getPreOptimizationResult() {
        return Optional.ofNullable(preOptimizationResult);
    }

    public void setPreOptimizationResult(PrePerimeterResult preOptimizationResult) {
        this.preOptimizationResult = preOptimizationResult;
    }

    @Override
    public String getName() {
        return "linear-rao-input";
    }

}
