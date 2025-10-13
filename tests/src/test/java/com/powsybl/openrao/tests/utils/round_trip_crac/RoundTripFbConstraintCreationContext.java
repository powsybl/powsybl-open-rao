/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.tests.utils.round_trip_crac;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RoundTripFbConstraintCreationContext extends FbConstraintCreationContext {
    private final Crac overridingCrac;

    public RoundTripFbConstraintCreationContext(FbConstraintCreationContext cracCreationContext, Crac overridingCrac) {
        super(cracCreationContext);
        this.overridingCrac = overridingCrac;
    }

    @Override
    public Crac getCrac() {
        return overridingCrac;
    }
}
