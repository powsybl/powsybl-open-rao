/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.crac_impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnec;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnVoltageConstraint;
import com.powsybl.open_rao.data.crac_api.usage_rule.OnVoltageConstraintAdder;

import java.util.Objects;

import static com.powsybl.open_rao.data.crac_impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Fabrice Buscaylet {@literal <fabrice.buscaylet at artelys.com>}
 */
public class OnVoltageConstraintAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnVoltageConstraintAdder<T> {

    private final T owner;
    private String instantId;
    private String voltageCnecId;

    OnVoltageConstraintAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnVoltageConstraintAdder<T> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public OnVoltageConstraintAdder<T> withVoltageCnec(String voltageCnecId) {
        this.voltageCnecId = voltageCnecId;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, "OnInstant", "instant", "withInstant()");
        assertAttributeNotNull(voltageCnecId, "OnVoltageConstraint", "voltage cnec", "withVoltageCnec()");

        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isOutage()) {
            throw new FaraoException("OnVoltageConstraint usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        VoltageCnec voltageCnec = owner.getCrac().getVoltageCnec(voltageCnecId);
        if (Objects.isNull(voltageCnec)) {
            throw new FaraoException(String.format("VoltageCnec %s does not exist in crac. Consider adding it first.", voltageCnecId));
        }

        AbstractRemedialActionAdder.checkOnConstraintUsageRules(instant, voltageCnec);

        OnVoltageConstraint onVoltageConstraint = new OnVoltageConstraintImpl(instant, voltageCnec);
        owner.addUsageRule(onVoltageConstraint);
        return owner;
    }
}
