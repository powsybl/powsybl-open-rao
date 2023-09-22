/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraintAdder;

import java.util.Objects;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnAngleConstraintAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnAngleConstraintAdder<T> {

    private T owner;
    private Instant instant;
    private String angleCnecId;

    OnAngleConstraintAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnAngleConstraintAdder<T> withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public OnAngleConstraintAdder<T> withAngleCnec(String angleCnecId) {
        this.angleCnecId = angleCnecId;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instant, "OnInstant", "instant", "withInstant()");
        assertAttributeNotNull(angleCnecId, "OnAngleConstraint", "angle cnec", "withAngleCnec()");

        if (instant.isAuto()) {
            throw new FaraoException("OnAngleConstraint usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        AngleCnec angleCnec = owner.getCrac().getAngleCnec(angleCnecId);
        if (Objects.isNull(angleCnec)) {
            throw new FaraoException(String.format("AngleCnec %s does not exist in crac. Consider adding it first.", angleCnecId));
        }

        AbstractRemedialActionAdder.checkOnConstraintUsageRules(instant, angleCnec);

        OnAngleConstraint onAngleConstraint = new OnAngleConstraintImpl(instant, angleCnec);
        owner.addUsageRule(onAngleConstraint);
        return owner;
    }
}
