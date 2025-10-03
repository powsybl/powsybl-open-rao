/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstant;
import com.powsybl.openrao.data.crac.api.usagerule.OnInstantAdder;

import static com.powsybl.openrao.data.crac.impl.AdderUtils.assertAttributeNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class OnInstantAdderImpl<T extends AbstractRemedialActionAdder<T>> implements OnInstantAdder<T> {

    private T owner;
    private String instantId;

    OnInstantAdderImpl(AbstractRemedialActionAdder<T> owner) {
        this.owner = (T) owner;
    }

    @Override
    public OnInstantAdder<T> withInstant(String instantId) {
        this.instantId = instantId;
        return this;
    }

    @Override
    public T add() {
        assertAttributeNotNull(instantId, "OnInstant", "instant", "withInstant()");

        Instant instant = owner.getCrac().getInstant(instantId);
        if (instant.isOutage()) {
            throw new OpenRaoException("OnInstant usage rules are not allowed for OUTAGE instant.");
        }
        if (instant.isPreventive()) {
            owner.getCrac().addPreventiveState();
        }

        OnInstant onInstant = new OnInstantImpl(instant);
        owner.addUsageRule(onInstant);
        return owner;
    }
}
