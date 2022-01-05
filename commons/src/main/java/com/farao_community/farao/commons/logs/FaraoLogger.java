/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.commons.logs;

/**
 * An interface for custom logging
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public interface FaraoLogger {
    public void trace(String format, Object... arguments);

    public void info(String format, Object... arguments);

    public void warn(String format, Object... arguments);

    public void error(String format, Object... arguments);

    public void debug(String format, Object... arguments);
}
