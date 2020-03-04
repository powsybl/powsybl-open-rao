/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityComputationParameters;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowExtensionParameters  extends AbstractExtension<RaoParameters> {

    static final boolean DEFAULT_RAO_WITH_LOOP_FLOW = true;

    private SensitivityComputationParameters sensitivityComputationParameters = new SensitivityComputationParameters();
    private LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
    private boolean raoWithLoopFlow = DEFAULT_RAO_WITH_LOOP_FLOW;

    @Override
    public String getName() {
        return "LoopFlowExtensionParameters";
    }

    public void setRaoWithLoopFlow(boolean raoWithLoopFlow) {
        this.raoWithLoopFlow = raoWithLoopFlow;
    }

    public boolean isRaoWithLoopFlow() {
        return raoWithLoopFlow;
    }

    public void setSensitivityComputationParameters(SensitivityComputationParameters sensitivityComputationParameters) {
        this.sensitivityComputationParameters = sensitivityComputationParameters;
    }

    public void setLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = loadFlowParameters;
    }

    public FlowBasedComputationParameters buildFlowBasedComputationParameters() {
        FlowBasedComputationParameters flowBasedComputationParameters = new FlowBasedComputationParameters();
        flowBasedComputationParameters.setLoadFlowParameters(loadFlowParameters);
        flowBasedComputationParameters.setSensitivityComputationParameters(sensitivityComputationParameters);
        return flowBasedComputationParameters;
    }
}
