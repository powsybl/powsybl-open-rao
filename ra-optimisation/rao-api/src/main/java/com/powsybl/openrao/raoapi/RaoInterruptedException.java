/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class RaoInterruptedException extends RuntimeException {
    private final RaoResult raoResult;

    public RaoInterruptedException() {
        this(null);
    }

    public RaoInterruptedException(RaoResult raoResult) {
        super("RAO was interrupted.");
        this.raoResult = raoResult;
    }

    public Optional<RaoResult> getRaoResult() {
        return Optional.ofNullable(raoResult);
    }
}
