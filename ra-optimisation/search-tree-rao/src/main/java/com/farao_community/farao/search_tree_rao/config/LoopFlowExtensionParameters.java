/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LoopFlowExtensionParameters  extends AbstractExtension<RaoParameters> {

    static final boolean DEFAULT_RAO_WITH_LOOP_FLOW = false; //loop flow is for CORE D2CC, default value set to false

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

}
