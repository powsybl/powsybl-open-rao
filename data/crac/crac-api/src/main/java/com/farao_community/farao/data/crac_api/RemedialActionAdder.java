/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.data.crac_api.usage_rule.FreeToUseAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnStateAdder;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface RemedialActionAdder<T extends RemedialActionAdder<T>> extends IdentifiableAdder<T> {

    T withOperator(String operator);

    FreeToUseAdder<T> newFreeToUseUsageRule();

    OnStateAdder<T> newOnStateUsageRule();
}
