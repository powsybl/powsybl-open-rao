/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PreventiveState implements State {

    private final Instant instant;

    PreventiveState(Instant instant) {
        if (instant.getInstantKind() != InstantKind.PREVENTIVE) { // TODO test this
            throw new FaraoException("Instant must be preventive");
        }
        this.instant = instant;
    }

    @Override
    public String getId() {
        return instant.getId();
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public Optional<Contingency> getContingency() {
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }
}
