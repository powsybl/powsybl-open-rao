/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FaraoException extends RuntimeException {

    public FaraoException() {
    }

    public FaraoException(final String msg) {
        super(msg);
    }

    public FaraoException(final Throwable throwable) {
        super(throwable);
    }

    public FaraoException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
