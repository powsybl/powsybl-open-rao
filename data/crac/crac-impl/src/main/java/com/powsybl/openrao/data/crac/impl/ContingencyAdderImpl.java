/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyBuilder;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.ContingencyAdder;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static java.lang.String.format;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ContingencyAdderImpl extends AbstractIdentifiableAdder<ContingencyAdder> implements ContingencyAdder {
    final CracImpl owner;
    private final Map<String, ContingencyElementType> elementsTypeById = new TreeMap<>();

    ContingencyAdderImpl(CracImpl owner) {
        Objects.requireNonNull(owner);
        this.owner = owner;
    }

    @Override
    protected String getTypeDescription() {
        return "Contingency";
    }

    @Override
    public ContingencyAdder withContingencyElement(String contingencyElementId, ContingencyElementType contingencyElementType) {
        elementsTypeById.put(contingencyElementId, contingencyElementType);
        return this;
    }

    private void addElementsInContingency(ContingencyBuilder builder, String contingencyElementId, ContingencyElementType contingencyElementType) {
        switch (contingencyElementType) {
            case BRANCH -> builder.addBranch(contingencyElementId);
            case GENERATOR -> builder.addGenerator(contingencyElementId);
            case STATIC_VAR_COMPENSATOR -> builder.addStaticVarCompensator(contingencyElementId);
            case SHUNT_COMPENSATOR -> builder.addShuntCompensator(contingencyElementId);
            case HVDC_LINE -> builder.addHvdcLine(contingencyElementId);
            case BUSBAR_SECTION -> builder.addBusbarSection(contingencyElementId);
            case DANGLING_LINE -> builder.addDanglingLine(contingencyElementId);
            case LINE -> builder.addLine(contingencyElementId);
            case TWO_WINDINGS_TRANSFORMER -> builder.addTwoWindingsTransformer(contingencyElementId);
            case THREE_WINDINGS_TRANSFORMER -> builder.addThreeWindingsTransformer(contingencyElementId);
            case LOAD -> builder.addLoad(contingencyElementId);
            case SWITCH -> builder.addSwitch(contingencyElementId);
            case BATTERY -> builder.addBattery(contingencyElementId);
            case BUS -> builder.addBus(contingencyElementId);
            case TIE_LINE -> builder.addTieLine(contingencyElementId);
        }
    }

    @Override
    public Contingency add() {
        checkId();
        ContingencyBuilder builder = Contingency.builder(id);
        // Elements are in TreeMap which will not allow duplication and elements will be added in their id natural order,
        // then even if elements are stored in a list, same set of elements will be equals if build with this adder
        elementsTypeById.forEach((id, type) -> addElementsInContingency(builder, id, type));
        if (name != null) {
            builder.addName(name);
        }
        Contingency contingency = builder.build();
        if (owner.getContingency(id) != null) {  // contingency with same id already exist in the Crac
            if (owner.getContingency(id).equals(contingency)) {
                return owner.getContingency(id);
            } else {
                throw new OpenRaoException(format("A contingency with the same ID (%s) but not equals (same fields) already exists.", this.id));
            }
        } else {
            owner.addContingency(contingency);
            return owner.getContingency(id);
        }
    }
}
