/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.utils.round_trip_crac;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracio.cse.CseCracCreationContext;

public class RoundTripCseCracCreationContext extends CseCracCreationContext {
    private Crac overridingCrac;

    public RoundTripCseCracCreationContext(CseCracCreationContext cracCreationContext, Crac overridingCrac) {
        super(cracCreationContext);
        this.overridingCrac = overridingCrac;
    }

    @Override
    public Crac getCrac() {
        return overridingCrac;
    }
}
