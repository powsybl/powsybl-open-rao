/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.RemedialActionAdder;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerConditionAdder;

import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractRemedialActionAdder<T extends RemedialActionAdder<T>> extends AbstractIdentifiableAdder<T> implements RemedialActionAdder<T> {

    protected String operator;
    protected Integer speed;
    protected Set<TriggerCondition> triggerConditions = new HashSet<>();
    private final CracImpl crac;

    AbstractRemedialActionAdder(CracImpl crac) {
        Objects.requireNonNull(crac);
        this.crac = crac;
    }

    @Override
    public T withOperator(String operator) {
        this.operator = operator;
        return (T) this;
    }

    @Override
    public T withSpeed(Integer speed) {
        this.speed = speed;
        return (T) this;
    }

    void addTriggerCondition(TriggerCondition triggerCondition) {
        this.triggerConditions.add(triggerCondition);
    }

    @Override
    public TriggerConditionAdder<T> newTriggerCondition() {
        return new TriggerConditionAdderImpl(this);
    }

    CracImpl getCrac() {
        return this.crac;
    }
}
