/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class OpenRaoException extends RuntimeException {

    public OpenRaoException() {
    }

    public OpenRaoException(final String msg) {
        super(msg);
    }

    public OpenRaoException(final Throwable throwable) {
        super(throwable);
    }

    public OpenRaoException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
