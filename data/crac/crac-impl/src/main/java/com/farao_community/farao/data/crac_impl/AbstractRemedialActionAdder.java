/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialActionAdder;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.usage_rule.*;

import java.util.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractRemedialActionAdder<T extends RemedialActionAdder<T>>  extends AbstractIdentifiableAdder<T> implements RemedialActionAdder<T> {

    protected String operator;
    protected List<UsageRule> usageRules = new ArrayList<>();
    private CracImpl crac;

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
    public FreeToUseAdder<T> newFreeToUseUsageRule() {
        return new FreeToUseAdderImpl(this);
    }

    @Override
    public OnStateAdder<T> newOnStateUsageRule() {
        return new OnStateAdderImpl(this);
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
        // Only allow PRAs with usage method OnFlowConstraint/OnAngleConstraint, for CNECs of instants PREVENTIVE & OUTAGE & CURATIVE
        // Only allow ARAs with usage method OnFlowConstraint/OnAngleConstraint, for CNECs of instant AUTO
        //  Only allow CRAs with usage method OnFlowConstraint/OnAngleConstraint, for CNECs of instant CURATIVE

        Map<Instant, Set<Instant>> allowedCnecInstantPerRaInstant = Map.of(
            Instant.PREVENTIVE, Set.of(Instant.PREVENTIVE, Instant.OUTAGE, Instant.CURATIVE),
            Instant.AUTO, Set.of(Instant.AUTO),
            Instant.CURATIVE, Set.of(Instant.CURATIVE)
        );

        if (!allowedCnecInstantPerRaInstant.get(instant).contains(cnec.getState().getInstant())) {
            throw new FaraoException(String.format("Remedial actions available at instant %s on a CNEC constraint at instant %s are not allowed.", instant, cnec.getState().getInstant()));
        }
    }
}
