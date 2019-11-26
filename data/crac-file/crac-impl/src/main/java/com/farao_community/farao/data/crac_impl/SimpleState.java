/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

/**
 * Business object for a state (instant and contingency) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class SimpleState implements State {

    private Contingency contingency;
    private Instant instant;

    public SimpleState(Optional<Contingency> contingency, Instant instant) {
        this.contingency = contingency.orElse(null);
        this.instant = instant;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public Optional<Contingency> getContingency() {
        return Optional.ofNullable(contingency);
    }

    @Override
    public void setContingency(Optional<Contingency> contingency) {
        this.contingency = contingency.orElse(null);
    }
}
