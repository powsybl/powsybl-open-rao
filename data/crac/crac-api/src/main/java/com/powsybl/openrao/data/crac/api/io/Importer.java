/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.io;

import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public interface Importer {

    /**
     * Get a unique identifier of the format.
     */
    String getFormat();

    // @Deprecated
    //TODO Lui deprecated
    // boolean depre_exists(String filename, InputStream inputStream);

    boolean exists(SafeFileReader inputFile);

    /**
     * Create a model.
     *
     * @param inputStream data input stream
     * @param cracCreationParameters extra CRAC creation parameters
     * @param network     network upon which the CRAC is based
     * @return the model
     */
    // @Deprecated
    //TODO Lui deprecated
    // CracCreationContext depre_importData(InputStream inputStream, CracCreationParameters cracCreationParameters, Network network);

    CracCreationContext importData(SafeFileReader inputFile, CracCreationParameters cracCreationParameters, Network network);

    static Importer findImporter(SafeFileReader inputFile) {
        return new ServiceLoaderCache<>(Importer.class).getServices().stream()
            .filter(importer -> importer.exists(inputFile))
            .findAny()
            .orElseThrow(() -> new OpenRaoException("No suitable importer found."));
    }



}
