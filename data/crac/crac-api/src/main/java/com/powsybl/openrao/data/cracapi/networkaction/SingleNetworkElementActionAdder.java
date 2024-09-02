/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.cracapi.networkaction;

public interface SingleNetworkElementActionAdder<T extends SingleNetworkElementActionAdder<T>> {
    T withNetworkElement(String networkElementId);

    T withNetworkElement(String networkElementId, String networkElementName);

    NetworkActionAdder add();
}
