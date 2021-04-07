/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.cnec.adder;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;

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
        if (contingency != null && instant != Instant.PREVENTIVE) {
            parent.addCnec(id, name, networkElement.getId(), operator, thresholds, contingency, instant, reliabilityMargin, optimized, monitored);
        } else if (contingency == null && instant == Instant.PREVENTIVE) {
            parent.addPreventiveCnec(id, name, networkElement.getId(), operator, thresholds, reliabilityMargin, optimized, monitored);
        } else {
            throw new FaraoException("Impossible to add CNEC in preventive after a contingency.");
        }
        return parent.getBranchCnec(id);
    }
}
