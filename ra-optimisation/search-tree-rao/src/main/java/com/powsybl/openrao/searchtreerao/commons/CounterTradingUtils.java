package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.sensitivity.SensitivityVariableSet;

public class CounterTradingUtils {

    private CounterTradingUtils() {
    }

    public static void updateCounterTradeRangeActionGlsks(Crac crac, ZonalData<SensitivityVariableSet> glsk) {
        crac.getCounterTradeRangeActions().forEach(action -> action.setGlsk(glsk));

    }

}
