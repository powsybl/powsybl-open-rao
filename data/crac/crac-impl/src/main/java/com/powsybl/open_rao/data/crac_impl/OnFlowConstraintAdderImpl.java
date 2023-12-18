/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnFlowConstraint;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnFlowConstraintAdder;

import java.util.Objects;

import static com.powsybl.open_rao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnFlowConstraintAdder<T> {

    private T owner;
    private String instantId;
    private String flowCnecId;

    OnFlowConstraintAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnFlowConstraintAdder<T> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public OnFlowConstraintAdder<T> withFlowCnec(String flowCnecId) {
        this.flowCnecId = flowCnecId;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, "OnInstant", "instant", "withInstant()");
        assertAttributeNotNull(flowCnecId, "OnFlowConstraint", "flow cnec", "withFlowCnec()");

        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isOutage()) {
            throw new OpenRaoException("OnFlowConstraint usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        FlowCnec flowCnec = owner.getCrac().getFlowCnec(flowCnecId);
        if (Objects.isNull(flowCnec)) {
            throw new OpenRaoException(String.format("FlowCnec %s does not exist in crac. Consider adding it first.", flowCnecId));
        }

        AbstractRemedialActionAdder.checkOnConstraintUsageRules(instant, flowCnec);

        //TODO : you'll need the order to get the correct instant once we have more than one curative/auto instant

        OnFlowConstraint onFlowConstraint = new OnFlowConstraintImpl(instant, flowCnec);
        owner.addUsageRule(onFlowConstraint);
        return owner;
    }
}
