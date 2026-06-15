/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;

import java.util.Objects;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotEmpty;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
class CounterTradeRangeActionAdderImpl extends AbstractStandardRangeActionAdder<CounterTradeRangeActionAdder> implements CounterTradeRangeActionAdder {

    public static final String COUNTER_TRADE_RANGE_ACTION = "CounterTradeRangeAction";
    private String exportingArea;
    private String importingArea;

    @Override
    protected String getTypeDescription() {
        return COUNTER_TRADE_RANGE_ACTION;
    }

    CounterTradeRangeActionAdderImpl(CracImpl owner) {
        super(owner);
    }

    @Override
    public CounterTradeRangeActionAdder withExportingArea(String exportingArea) {
        this.exportingArea = exportingArea;
        return this;
    }

    @Override
    public CounterTradeRangeActionAdder withImportingArea(String importingArea) {
        this.importingArea = importingArea;
        return this;
    }

    @Override
    public CounterTradeRangeAction add() {
        checkId();
        checkAutoUsageRules();
        if (!Objects.isNull(getCrac().getRemedialAction(id))) {
            throw new OpenRaoException(String.format("A remedial action with id %s already exists", id));
        }

        // check exporting and importing country
        assertAttributeNotNull(exportingArea, COUNTER_TRADE_RANGE_ACTION, "exporting country", "withExportingArea()");
        assertAttributeNotNull(importingArea, COUNTER_TRADE_RANGE_ACTION, "importing country", "withImportingArea()");

        // check ranges
        assertAttributeNotEmpty(ranges, COUNTER_TRADE_RANGE_ACTION, "range", "newRange()");

        // check usage rules
        if (usageRules.isEmpty()) {
            BUSINESS_WARNS.warn("CounterTradeRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        CounterTradeRangeAction counterTradeRangeAction = new CounterTradeRangeActionImpl(
            this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, this.initialSetpoint, speed, activationCost, variationCosts, this.exportingArea, this.importingArea
        );
        getCrac().addCounterTradeRangeAction(counterTradeRangeAction);
        return counterTradeRangeAction;

    }

}
