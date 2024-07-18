/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import java.util.Map;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */
public interface RaUsageLimitsAdder {

    RaUsageLimitsAdder withMaxRa(int maxRa);

    RaUsageLimitsAdder withMaxTso(int maxTso);

    RaUsageLimitsAdder withMaxTopoPerTso(Map<String, Integer> maxTopoPerTso);

    RaUsageLimitsAdder withMaxPstPerTso(Map<String, Integer> maxPstPerTso);

    RaUsageLimitsAdder withMaxRaPerTso(Map<String, Integer> maxRaPerTso);

    RaUsageLimitsAdder withMaxElementaryActionPerTso(Map<String, Integer> maxRaPerTso);

    RaUsageLimits add();
}
