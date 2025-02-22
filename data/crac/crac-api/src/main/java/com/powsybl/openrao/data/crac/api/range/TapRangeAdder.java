package com.powsybl.openrao.data.crac.api.range;

import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface TapRangeAdder {

    TapRangeAdder withMinTap(int minTap);

    TapRangeAdder withMaxTap(int maxTap);

    TapRangeAdder withRangeType(RangeType rangeType);

    PstRangeActionAdder add();

}
