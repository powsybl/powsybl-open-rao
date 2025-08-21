/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.usagerule;

import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.iidm.network.Country;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface OnFlowConstraintInCountryAdder<T extends RemedialActionAdder<T>> {
    OnFlowConstraintInCountryAdder<T> withInstant(String instantId);

    OnFlowConstraintInCountryAdder<T> withContingency(String contingencyId);

    OnFlowConstraintInCountryAdder<T> withCountry(Country country);

    OnFlowConstraintInCountryAdder<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
