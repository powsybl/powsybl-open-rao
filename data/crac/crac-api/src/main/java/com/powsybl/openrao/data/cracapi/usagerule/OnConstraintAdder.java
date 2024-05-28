/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracapi.usagerule;

import com.powsybl.openrao.data.cracapi.RemedialActionAdder;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
public interface OnConstraintAdder<T extends RemedialActionAdder<T>, J extends Cnec<?>> {
    OnConstraintAdder<T, J> withInstant(String instantId);

    OnConstraintAdder<T, J> withCnec(String cnecId);

    OnConstraintAdder<T, J> withUsageMethod(UsageMethod usageMethod);

    T add();
}
