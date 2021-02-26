/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.cnec.adder;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.cnec.FlowCnecImpl;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecAdderImpl extends AbstractBranchCnecAdder {

    public FlowCnecAdderImpl(SimpleCrac parent) {
        super(parent);
    }

    @Override
    public BranchCnec add() {
        super.checkCnec();
        FlowCnecImpl flowCnec = new FlowCnecImpl(id, name, networkElement, operator,
                parent.addState(contingency, instant), optimized, monitored, thresholds, reliabilityMargin);
        parent.addCnec(flowCnec);
        return parent.getBranchCnec(id);
    }
}
