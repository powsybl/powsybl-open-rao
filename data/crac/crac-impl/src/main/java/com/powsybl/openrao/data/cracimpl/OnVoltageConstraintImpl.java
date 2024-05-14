/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.usagerule.OnVoltageConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public class OnVoltageConstraintImpl extends AbstractOnConstraintUsageRule<VoltageCnec> implements OnVoltageConstraint {
    OnVoltageConstraintImpl(UsageMethod usageMethod, Instant instant, VoltageCnec voltageCnec) {
        super(usageMethod, instant, voltageCnec);
    }

    @Override
    public VoltageCnec getVoltageCnec() {
        return cnec;
    }
}
