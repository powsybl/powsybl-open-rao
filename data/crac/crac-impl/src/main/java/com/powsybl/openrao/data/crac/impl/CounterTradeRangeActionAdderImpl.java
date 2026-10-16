/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.CountryGraph;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.ConnectedArea;
import com.powsybl.openrao.data.crac.api.ConnectedAreaAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotEmpty;
import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
class CounterTradeRangeActionAdderImpl extends AbstractStandardRangeActionAdder<CounterTradeRangeActionAdder> implements CounterTradeRangeActionAdder {

    public static final String COUNTER_TRADE_RANGE_ACTION = "CounterTradeRangeAction";
    private Double initialNetPosition;
    private String area;
    private CountryGraph countryGraph;
    private final List<ConnectedArea> connectedAreas = new ArrayList<>();

    @Override
    protected String getTypeDescription() {
        return COUNTER_TRADE_RANGE_ACTION;
    }

    CounterTradeRangeActionAdderImpl(CracImpl owner) {
        super(owner);
    }

    @Override
    public CounterTradeRangeActionAdder withInitialNetPosition(Double initialNetPosition) {
        this.initialNetPosition = initialNetPosition;
        return this;
    }

    @Override
    public CounterTradeRangeActionAdder withArea(String area) {
        this.area = area;
        return this;
    }

    @Override
    public CounterTradeRangeActionAdder withConnectedAreas(Network network) {
        this.countryGraph = new CountryGraph(network);
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

        // connected areas defined with newConnectedArea() are kept as they are, with their border ranges
        List<ConnectedArea> allConnectedAreas = new ArrayList<>(connectedAreas);
        if (countryGraph != null) {
            // Calculate connected areas: the areas sharing a border with the area in the network
            Set<String> neighbors = countryGraph.getNeighbors(Country.valueOf(area)).stream().map(Country::toString).collect(Collectors.toSet());

            // check that each connected area defined with newConnectedArea() shares a border with the area
            for (ConnectedArea connectedArea : connectedAreas) {
                if (!neighbors.contains(connectedArea.getArea())) {
                    throw new OpenRaoException(String.format("Connected area %s of CounterTradeRangeAction %s does not share a border with area %s", connectedArea.getArea(), id, area));
                }
            }

            // if no connected area was defined with newConnectedArea(), all the neighbors from the network are used,
            // without border ranges, sorted to get a deterministic order
            if (connectedAreas.isEmpty()) {
                neighbors.stream()
                    .sorted()
                    .forEach(neighbor -> allConnectedAreas.add(new ConnectedAreaImpl(neighbor, new ArrayList<>())));
            }
        } else if (!connectedAreas.isEmpty()) {
            // without a network, the connected areas cannot be checked to share a border with the area
            throw new OpenRaoException(String.format(
                "Cannot check that the connected areas of CounterTradeRangeAction %s share a border with area %s without a network. Please use withConnectedAreas()",
                id, area));
        }

        // check initialNetPosition
        assertAttributeNotNull(initialNetPosition, COUNTER_TRADE_RANGE_ACTION, "initialNetPosition", "withInitialNetPosition()");

        // check ranges
        assertAttributeNotEmpty(ranges, COUNTER_TRADE_RANGE_ACTION, "range", "newRange()");

        // check usage rules
        if (usageRules.isEmpty()) {
            BUSINESS_WARNS.warn("CounterTradeRangeAction {} does not contain any usage rule, by default it will never be available", id);
        }

        CounterTradeRangeAction counterTradeRangeAction = new CounterTradeRangeActionImpl(
            this.id, this.name, this.operator, this.groupId, this.usageRules, this.ranges, this.initialNetPosition, this.initialSetpoint,
            speed, activationCost, variationCosts, this.area, allConnectedAreas
        );
        getCrac().addCounterTradeRangeAction(counterTradeRangeAction);
        return counterTradeRangeAction;

    }

}
