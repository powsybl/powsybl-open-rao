/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creator_api;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public interface ElementaryCreationContext {

    /**
     * Get a unique identifier of the element in the native Crac.
     */
    String getNativeId();

    /**
     * Indicates if element has been imported.
     */
    boolean isImported();

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
