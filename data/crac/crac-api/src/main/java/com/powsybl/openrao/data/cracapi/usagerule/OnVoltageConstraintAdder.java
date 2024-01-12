/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracapi.usagerule;

import com.powsybl.openrao.data.cracapi.RemedialActionAdder;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public interface OnVoltageConstraintAdder<T extends RemedialActionAdder<T>> {

    OnVoltageConstraintAdder<T> withInstant(String instantId);

    OnVoltageConstraintAdder<T> withVoltageCnec(String voltageCnecId);

    OnVoltageConstraintAdder<T> withUsageMethod(UsageMethod usageMethod);

    T add();
}
