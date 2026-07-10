/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.sensitivity.SensitivityVariableSet;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
public final class CounterTradingUtils {

    private CounterTradingUtils() {
    }

    public static void updateCounterTradeRangeActionGlsks(Crac crac, ZonalData<SensitivityVariableSet> glsk) {
        crac.getCounterTradeRangeActions().forEach(action -> action.setGlsk(glsk));

    }

}
