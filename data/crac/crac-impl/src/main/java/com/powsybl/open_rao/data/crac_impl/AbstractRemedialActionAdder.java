/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.RemedialActionAdder;
import com.powsybl.open_rao.data.crac_api.cnec.Cnec;
import com.powsybl.open_rao.data.crac_api.usage_rule.*;

import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractRemedialActionAdder<T extends RemedialActionAdder<T>> extends AbstractIdentifiableAdder<T> implements RemedialActionAdder<T> {

    protected String operator;
    protected Integer speed;
    protected Set<UsageRule> usageRules = new HashSet<>();
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

    @Override
    public OnInstantAdder<T> newOnInstantUsageRule() {
        return new OnInstantAdderImpl(this);
    }

    @Override
    public OnContingencyStateAdder<T> newOnContingencyStateUsageRule() {
        return new OnContingencyStateAdderImpl(this);
    }

    @Override
    public OnFlowConstraintAdder<T> newOnFlowConstraintUsageRule() {
        return new OnFlowConstraintAdderImpl(this);
    }

    @Override
    public OnAngleConstraintAdder<T> newOnAngleConstraintUsageRule() {
        return new OnAngleConstraintAdderImpl(this);
    }

    @Override
    public OnVoltageConstraintAdder<T> newOnVoltageConstraintUsageRule() {
        return new OnVoltageConstraintAdderImpl(this);
    }

    @Override
    public OnFlowConstraintInCountryAdder<T> newOnFlowConstraintInCountryUsageRule() {
        return new OnFlowConstraintInCountryAdderImpl(this);
    }

    void addUsageRule(UsageRule usageRule) {
        this.usageRules.add(usageRule);
    }

    CracImpl getCrac() {
        return this.crac;
    }

    static void checkOnConstraintUsageRules(Instant instant, Cnec<?> cnec) {
        // Only allow PRAs with usage method OnFlowConstraint/OnAngleConstraint/OnVoltageConstraint, for CNECs of instants PREVENTIVE & OUTAGE & CURATIVE
        // Only allow ARAs with usage method OnFlowConstraint/OnAngleConstraint/OnVoltageConstraint, for CNECs of instant AUTO
        // Only allow CRAs with usage method OnFlowConstraint/OnAngleConstraint/OnVoltageConstraint, for CNECs of instant CURATIVE

        if (cnec.getState().getInstant().comesBefore(instant)) {
            throw new OpenRaoException(String.format("Remedial actions available at instant '%s' on a CNEC constraint at instant '%s' are not allowed.", instant, cnec.getState().getInstant()));
        }
    }
}
