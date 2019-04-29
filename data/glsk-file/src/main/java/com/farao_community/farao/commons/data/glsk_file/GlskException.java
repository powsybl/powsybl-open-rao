/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons.data.glsk_file;

/**
 * Glsk Exception
 * @author RTE International {@literal <contact@rte-international.com>}
 */
public class GlskException extends RuntimeException {
    /**
     * @param message Exception's message
     */
    public GlskException(String message) {
        super(message);
    }

}
