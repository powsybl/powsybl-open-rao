/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraintAdder;

import java.util.Objects;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class OnConstraintAdderImpl<T extends AbstractRemedialActionAdder<T>, S extends Cnec<?>> implements OnConstraintAdder<T, S> {
    public static final String ON_CONSTRAINT = "OnConstraint";
    private final T owner;
    private String instantId;
    private String cnecId;

    OnConstraintAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnConstraintAdderImpl<T, S> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public OnConstraintAdderImpl<T, S> withCnec(String cnecId) {
        this.cnecId = cnecId;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, ON_CONSTRAINT, "instant", "withInstant()");
        assertAttributeNotNull(cnecId, ON_CONSTRAINT, "cnec", "withCnec()");

        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isOutage()) {
            throw new OpenRaoException("OnConstraint usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        S cnec = (S) owner.getCrac().getCnec(cnecId);
        if (Objects.isNull(cnec)) {
            throw new OpenRaoException(String.format("Cnec %s does not exist in crac. Consider adding it first.", cnecId));
        }

        AbstractRemedialActionAdder.checkOnConstraintUsageRules(instant, cnec);

        OnConstraint<S> onConstraint = new OnConstraintImpl<>(instant, cnec);
        owner.addUsageRule(onConstraint);
        return owner;
    }
}
