package com.powsybl.openrao.data.crac.api.rangeaction;

import com.powsybl.openrao.data.crac.api.range.ConnectedAreaAdder;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public interface CounterTradeRangeActionAdder extends StandardRangeActionAdder<CounterTradeRangeActionAdder> {

    CounterTradeRangeActionAdder withInitialNetPosition(Double initialNetPosition);

    CounterTradeRangeActionAdder withArea(String area);

    ConnectedAreaAdder newConnectedArea();

    CounterTradeRangeAction add();
}
