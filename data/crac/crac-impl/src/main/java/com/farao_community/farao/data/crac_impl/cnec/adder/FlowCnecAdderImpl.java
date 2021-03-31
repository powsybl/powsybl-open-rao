/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.cnec.adder;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.cnec.FlowCnecImpl;

import static java.lang.String.format;

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
        State state;
        if (contingency != null) {
            parent.addContingency(contingency);
            if (parent.getState(contingency, instant) == null) {
                throw new FaraoException(format("State %s - %s does not exist. Impossible to add %s.", contingency.getId(), instant, id));
            }
            state = parent.getState(contingency, instant);
        } else if (instant.equals(Instant.PREVENTIVE)) {
            state = parent.getPreventiveState();
        } else {
            throw new FaraoException("Adding a CNEC on a post-contingency instant requires a contingency which is not null");
        }

        FlowCnecImpl flowCnec = new FlowCnecImpl(id, name, networkElement, operator, state, optimized, monitored, thresholds, reliabilityMargin);
        parent.addCnec(flowCnec);
        return parent.getBranchCnec(id);
    }
}
