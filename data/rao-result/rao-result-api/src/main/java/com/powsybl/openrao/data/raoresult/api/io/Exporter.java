/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.io;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;

import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface Exporter {
    /**
     * Get a unique identifier of the format.
     */
    String getFormat();

    Set<String> getRequiredProperties();

    Class<? extends CracCreationContext> getCracCreationContextClass();

    default void validateDataToExport(CracCreationContext cracCreationContext, Properties properties) {
        if (!getCracCreationContextClass().isInstance(cracCreationContext)) {
            throw new OpenRaoException("%s exporter expects a %s.".formatted(getFormat(), getCracCreationContextClass().getSimpleName()));
        }
        if (!getRequiredProperties().isEmpty() && properties == null) {
            throw new OpenRaoException("The export properties cannot be null for %s export.".formatted(getFormat()));
        }
        for (String requiredProperty : getRequiredProperties()) {
            if (!properties.containsKey(requiredProperty)) {
                throw new OpenRaoException("The mandatory %s property is missing for %s export.".formatted(requiredProperty, getFormat()));
            }
        }
    }

    void exportData(RaoResult raoResult, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream);

    void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream);
}
