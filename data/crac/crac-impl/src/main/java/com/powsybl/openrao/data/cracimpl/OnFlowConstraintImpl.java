/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.usagerule.OnFlowConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintImpl extends AbstractOnConstraintUsageRule<FlowCnec> implements OnFlowConstraint {
    OnFlowConstraintImpl(UsageMethod usageMethod, Instant instant, FlowCnec flowCnec) {
        super(usageMethod, instant, flowCnec);
    }

    @Override
    public FlowCnec getFlowCnec() {
        return cnec;
    }
}
