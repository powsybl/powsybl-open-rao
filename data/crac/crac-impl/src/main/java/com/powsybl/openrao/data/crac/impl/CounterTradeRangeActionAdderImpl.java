/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.rangeaction.ConnectedArea;
import com.powsybl.openrao.data.crac.api.rangeaction.ConnectedAreaAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotEmpty;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
class CounterTradeRangeActionAdderImpl extends AbstractStandardRangeActionAdder<CounterTradeRangeActionAdder> implements CounterTradeRangeActionAdder {

    public static final String COUNTER_TRADE_RANGE_ACTION = "CounterTradeRangeAction";
    private String area;
    private Double initialNetPosition;
    private final List<ConnectedArea> connectedAreas = new ArrayList<>();

    @Override
    protected String getTypeDescription() {
        return COUNTER_TRADE_RANGE_ACTION;
    }

    CounterTradeRangeActionAdderImpl(CracImpl owner) {
        super(owner);
    }

    @Override
    public CounterTradeRangeActionAdder withArea(String area) {
        this.area = area;
        return this;
    }

    @Override
    public CounterTradeRangeActionAdder withInitialNetPosition(Double initialNetPosition) {
        this.initialNetPosition = initialNetPosition;
        return this;
    }

    @Override
    public ConnectedAreaAdder newConnectedArea() {
        return new ConnectedAreaAdderImpl(this);
    }

    void addConnectedArea(ConnectedArea connectedArea) {
        connectedAreas.add(connectedArea);
    }

    @Override
    public CounterTradeRangeAction add() {
        checkId();
        checkAutoUsageRules();
        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new OpenRaoException(String.format("A remedial action with id %s already exists", id));
        }

        // check area
        assertAttributeNotNull(area, COUNTER_TRADE_RANGE_ACTION, "area", "withArea()");

        // check ranges
        assertAttributeNotEmpty(ranges, COUNTER_TRADE_RANGE_ACTION, "range", "newRange()");

        // check usage rules
        if (usageRules.isEmpty()) {
            BUSINESS_WARNS.warn("CounterTradeRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        CounterTradeRangeAction counterTradeRangeAction = new CounterTradeRangeActionImpl(
            this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, this.initialSetpoint, speed, activationCost, variationCosts, this.area, this.initialNetPosition, this.connectedAreas
        );
        getCrac().addCounterTradeRangeAction(counterTradeRangeAction);
        return counterTradeRangeAction;

    }

}
