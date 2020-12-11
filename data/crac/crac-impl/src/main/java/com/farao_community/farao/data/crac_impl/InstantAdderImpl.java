/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantAdder;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class InstantAdderImpl extends AbstractIdentifiableAdder<InstantAdder> implements InstantAdder {

    private SimpleCrac parent;
    private Integer seconds;

    public InstantAdderImpl(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    public InstantAdder setSeconds(Integer seconds) {
        this.seconds = seconds;
        return this;
    }

    @Override
    public Instant add() {
        checkId();
        if (this.seconds == null) {
            throw new FaraoException("Cannot add an instant without a number of seconds. Please use setSeconds.");
        }
        return parent.addInstant(id, seconds);
    }
}
