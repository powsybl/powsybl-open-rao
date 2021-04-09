package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.Unit;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface PstRangeAdder {

    PstRangeAdder withMin(double minValue);

    PstRangeAdder withMax(double maxValue);

    PstRangeAdder withRangeType(RangeType rangeType);

    PstRangeAdder withRangeDefinition(RangeDefinition rangeDefinition);

    PstRangeAdder withUnit(Unit unit);

    PstRangeActionAdder add();

}
