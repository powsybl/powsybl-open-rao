/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.BorderRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.ConnectedArea;
import com.powsybl.openrao.data.crac.api.rangeaction.ConnectedAreaAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;

import java.util.ArrayList;
import java.util.List;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class ConnectedAreaAdderImpl implements ConnectedAreaAdder {

    private static final String CLASS_NAME = "ConnectedArea";
    private final CounterTradeRangeActionAdderImpl ownerAdder;

    private String area;
    private final List<StandardRange> borderRanges;

    ConnectedAreaAdderImpl(CounterTradeRangeActionAdderImpl ownerAdder) {
        this.ownerAdder = ownerAdder;
        this.borderRanges = new ArrayList<>();
    }

    @Override
    public ConnectedAreaAdder withArea(String area) {
        this.area = area;
        return this;
    }

    @Override
    public BorderRangeAdder newBorderRange() {
        return new BorderRangeAdderImpl(this);
    }

    void addBorderRange(StandardRange borderRange) {
        borderRanges.add(borderRange);
    }

    @Override
    public CounterTradeRangeActionAdder add() {
        assertAttributeNotNull(area, CLASS_NAME, "area", "withArea()");

        ConnectedArea connectedArea = new ConnectedAreaImpl(area, borderRanges);
        ownerAdder.addConnectedArea(connectedArea);
        return ownerAdder;
    }
}
