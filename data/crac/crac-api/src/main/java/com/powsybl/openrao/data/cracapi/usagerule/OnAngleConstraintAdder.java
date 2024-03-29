/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracapi.usagerule;

import com.powsybl.openrao.data.cracapi.RemedialActionAdder;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface OnAngleConstraintAdder<T extends RemedialActionAdder<T>> {
    OnAngleConstraintAdder<T> withInstant(String instantId);

    OnAngleConstraintAdder<T> withAngleCnec(String angleCnecId);

    OnAngleConstraintAdder<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
