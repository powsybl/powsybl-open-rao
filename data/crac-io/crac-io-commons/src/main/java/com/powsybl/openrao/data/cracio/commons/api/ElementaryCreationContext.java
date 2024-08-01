/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.api;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public interface ElementaryCreationContext {

    /**
     * Get a unique identifier of the element in the native Crac.
     */
    String getNativeObjectId();

    /**
     * Get a unique identifier of the element in the created Crac.
     */
    String getCreatedObjectId();

    /**
     * Indicates if element has been imported.
     */
    default boolean isImported() {
        return ImportStatus.IMPORTED.equals(getImportStatus());
    }

    /**
     * Indicates if element has been altered.
     */
    boolean isAltered();

    /**
     * Returns status detailing import situation.
     */
    ImportStatus getImportStatus();

    /**
     * Returns additional information on import context.
     */
    String getImportStatusDetail();
}
