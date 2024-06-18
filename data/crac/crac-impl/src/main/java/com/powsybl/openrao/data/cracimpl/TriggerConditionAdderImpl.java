/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerConditionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;

import java.util.Optional;

import static com.powsybl.openrao.data.cracimpl.AdderUtils.assertAttributeNotNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TriggerConditionAdderImpl<T extends AbstractRemedialActionAdder<T>> implements TriggerConditionAdder<T> {
    private final T owner;
    private String instantId;
    private String contingencyId;
    private String cnecId;
    private String country;
    private UsageMethod usageMethod;
    private static final String TRIGGER_CONDITION = "TriggerCondition";

    TriggerConditionAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public TriggerConditionAdder<T> withCnec(String cnecId) {
        this.cnecId = cnecId;
        return this;
    }

    @Override
    public TriggerConditionAdder<T> withContingency(String contingencyId) {
        this.contingencyId = contingencyId;
        return this;
    }

    @Override
    public TriggerConditionAdder<T> withCountry(String country) {
        this.country = country;
        return this;
    }

    @Override
    public TriggerConditionAdder<T> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public TriggerConditionAdder<T> withUsageMethod(UsageMethod usageMethod) {
        this.usageMethod = usageMethod;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, TRIGGER_CONDITION, "instant", "withInstant()");
        assertAttributeNotNull(usageMethod, TRIGGER_CONDITION, "usage method", "withUsageMethod()");

        Instant instant = owner.getCrac().getInstant(instantId);
        Contingency contingency = getContingencyInCrac();
        Cnec<?> cnec = getCnecInCrac();

        checkInstantCoherence(instant, contingency);
        checkCnecCoherence(cnec, contingency, country);

        TriggerCondition triggerCondition = new TriggerConditionImpl(instant, contingency, cnec, country, usageMethod);
        owner.addTriggerCondition(triggerCondition);
        return owner;
    }

    private Contingency getContingencyInCrac() {
        if (contingencyId == null) {
            return null;
        }
        Contingency contingency = owner.getCrac().getContingency(contingencyId);
        if (contingency == null) {
            throw new OpenRaoException(String.format("Contingency %s does not exist in crac. Consider adding it first.", contingencyId));
        }
        return contingency;
    }

    private Cnec<?> getCnecInCrac() {
        if (cnecId == null) {
            return null;
        }
        Cnec<?> cnec = owner.getCrac().getCnec(cnecId);
        if (cnec == null) {
            throw new OpenRaoException(String.format("Cnec %s does not exist in crac. Consider adding it first.", cnecId));
        }
        return cnec;
    }

    private void checkInstantCoherence(Instant instant, Contingency contingency) {
        if (instant.isOutage()) {
            throw new OpenRaoException("TriggerConditions are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            if (contingency != null) {
                throw new OpenRaoException("Preventive TriggerConditions are not allowed after a contingency.");
            }
            owner.getCrac().addPreventiveState();
        }
    }

    private void checkCnecCoherence(Cnec<?> cnec, Contingency contingency, String country) {
        if (cnec == null) {
            return;
        }
        if (contingency != null && !cnec.getState().getContingency().equals(Optional.of(contingency))) {
            throw new OpenRaoException("The provided cnec is not monitored after the provided contingency, this is not supported.");
        }
        if (country != null) {
            throw new OpenRaoException("A country and a cnec cannot be provided simultaneously.");
        }
    }
}
