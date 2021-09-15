/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.data.crac_creator_api;

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
     * Returns status detailing import situation.
     */
    ImportStatus getImportStatus();


    /**
     * Returns additional information on import context.
     */
    String getImportStatusDetail();
}
