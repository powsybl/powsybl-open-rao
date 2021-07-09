/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraint;
import com.farao_community.farao.data.crac_api.usage_rule.OnFlowConstraintAdder;

import java.util.Objects;

import static com.farao_community.farao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnFlowConstraintAdder<T> {

    private T owner;
    private Instant instant;
    private String flowCnecId;

    OnFlowConstraintAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnFlowConstraintAdder<T> withInstant(Instant instant) {
        this.instant = instant;
        return this;
    }

    @Override
    public OnFlowConstraintAdder<T> withFlowCnec(String flowCnecId) {
        this.flowCnecId = flowCnecId;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instant, "FreeToUse", "instant", "withInstant()");
        assertAttributeNotNull(flowCnecId, "OnFlowConstraint", "flow cnec", "withFlowCnec()");

        if (instant.equals(Instant.OUTAGE)) {
            throw new FaraoException("OnFlowConstraint usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.equals(Instant.PREVENTIVE)) {
            owner.getCrac().addPreventiveState();
        }
        // TODO: when Instant.AUTO will be handled by FARAO, consider adding some states in the CRAC here.
        // not required as as soon as there is no RA on AUTO instant

        FlowCnec flowCnec = owner.getCrac().getFlowCnec(flowCnecId);
        if (Objects.isNull(flowCnec)) {
            throw new FaraoException(String.format("FlowCnec %s does not exist in crac. Consider adding it first.", flowCnecId));
        }

        OnFlowConstraint onFlowConstraint = new OnFlowConstraintImpl(instant, flowCnec);
        owner.addUsageRule(onFlowConstraint);
        return owner;
    }
}
