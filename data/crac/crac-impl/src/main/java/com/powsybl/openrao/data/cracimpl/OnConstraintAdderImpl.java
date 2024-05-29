/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.usagerule.OnConstraint;
import com.powsybl.openrao.data.cracapi.usagerule.OnConstraintAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;

import java.util.Objects;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
public class OnConstraintAdderImpl<T extends AbstractRemedialActionAdder<T>, J extends Cnec<?>> implements OnConstraintAdder<T, J> {
    public static final String ON_CONSTRAINT = "OnConstraint";
    private final T owner;
    private String instantId;
    private String cnecId;
    private UsageMethod usageMethod;

    OnConstraintAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnConstraintAdderImpl<T, J> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public OnConstraintAdderImpl<T, J> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public OnConstraintAdderImpl<T, J> withCnec(String cnecId) {
        this.cnecId = cnecId;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, ON_CONSTRAINT, "instant", "withInstant()");
        assertAttributeNotNull(cnecId, ON_CONSTRAINT, "cnec", "withCnec()");
        assertAttributeNotNull(usageMethod, ON_CONSTRAINT, "usage method", "withUsageMethod()");

        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isOutage()) {
            throw new OpenRaoException("OnConstraint usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        J cnec = (J) owner.getCrac().getCnec(cnecId);
        if (Objects.isNull(cnec)) {
            throw new OpenRaoException(String.format("Cnec %s does not exist in crac. Consider adding it first.", cnecId));
        }

        AbstractRemedialActionAdder.checkOnConstraintUsageRules(instant, cnec);

        OnConstraint<J> onConstraint = new OnConstraintImpl<>(usageMethod, instant, cnec);
        owner.addUsageRule(onConstraint);
        return owner;
    }
}
