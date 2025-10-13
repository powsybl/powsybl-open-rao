/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.usagerule;

import com.powsybl.openrao.data.crac.api.RemedialActionAdder;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface OnConstraintAdder<T extends RemedialActionAdder<T>, S extends Cnec<?>> {
    OnConstraintAdder<T, S> withInstant(String instantId);

    OnConstraintAdder<T, S> withCnec(String cnecId);

    T add();
}
