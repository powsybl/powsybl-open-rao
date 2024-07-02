/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi.triggercondition;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.data.cracapi.RemedialActionAdder;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface TriggerConditionAdder<T extends RemedialActionAdder<T>> {
    TriggerConditionAdder<T> withCnec(String cnecId);

    TriggerConditionAdder<T> withContingency(String contingencyId);

    TriggerConditionAdder<T> withCountry(Country country);

    TriggerConditionAdder<T> withInstant(String instantId);

    TriggerConditionAdder<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
