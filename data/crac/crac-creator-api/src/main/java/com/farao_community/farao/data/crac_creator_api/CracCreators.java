/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creator_api.parameters.CracCreatorParameters;
import com.farao_community.farao.data.native_crac_api.NativeCrac;
import com.farao_community.farao.data.native_crac_io_api.NativeCracImporters;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.iidm.network.Network;

import java.io.*;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A utility class to work with CRAC creators
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class CracCreators {

    private static final Supplier<List<CracCreator>> CRAC_CREATORS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracCreator.class).getServices())::get;

    private CracCreators() {
    }

    /**
     * Flexible method to create a Crac from a native CRAC, a network and a OffsetDateTime, whatever the format of the
     * native CRAC.
     * @param nativeCrac native CRAC object
     * @param network network object required for the conversion of the NativeCrac into a Crac
     * @param offsetDateTime timestamp for which the Crac is creator (null values might be accepted by some creators)
     * @param cracCreatorParameters the configuration of the CRAC creation
     * @return the created {@link CracCreationContext} object
     */
    public static CracCreationContext createCrac(NativeCrac nativeCrac, Network network, OffsetDateTime offsetDateTime, CracCreatorParameters cracCreatorParameters) {
        CracCreator creator = findCreator(nativeCrac.getFormat());

        if (Objects.isNull(creator)) {
            throw new FaraoException(String.format("No CracCreator found for format %s", nativeCrac.getFormat()));
        }

        return creator.createCrac(nativeCrac, network, offsetDateTime, cracCreatorParameters);
    }

    /**
     * Flexible method to create a Crac from a native CRAC, a network and a OffsetDateTime, whatever the format of the
     * native CRAC.
     * @param nativeCrac native CRAC object
     * @param network network object required for the conversion of the NativeCrac into a Crac
     * @param offsetDateTime timestamp for which the Crac is creator (null values might be accepted by some creators)
     * @return the created {@link CracCreationContext} object
     */
    public static CracCreationContext createCrac(NativeCrac nativeCrac, Network network, OffsetDateTime offsetDateTime) {
        return createCrac(nativeCrac, network, offsetDateTime, new CracCreatorParameters());
    }

    /**
     * Flexible method to import a Crac from a native CRAC file, a network and a OffsetDateTime, whatever the format of the
     * native CRAC file.
     * @param nativeCracPath {@link Path} of the native CRAC file
     * @param network network object required for the conversion of the NativeCrac into a Crac
     * @param offsetDateTime timestamp for which the Crac is creator (null values might be accepted by some creators)
     * @return the created {@link NativeCrac} object
     */
    public static CracCreationContext importAndCreateCrac(Path nativeCracPath, Network network, OffsetDateTime offsetDateTime) {
        NativeCrac nativeCrac = NativeCracImporters.importData(nativeCracPath);
        return createCrac(nativeCrac, network, offsetDateTime);
    }

    /**
     * Flexible method to import a Crac from a native CRAC file, a network and a OffsetDateTime, whatever the format of the
     * native CRAC file.
     * @param fileName name of the native CRAC file
     * @param inputStream input stream of the native CRAC file
     * @param network network object required for the conversion of the NativeCrac into a Crac
     * @param offsetDateTime timestamp for which the Crac is creator (null values might be accepted by some creators)
     * @return the created {@link NativeCrac} object
     */
    public static CracCreationContext importAndCreateCrac(String fileName, InputStream inputStream, Network network, OffsetDateTime offsetDateTime) {
        NativeCrac nativeCrac = NativeCracImporters.importData(fileName, inputStream);
        return createCrac(nativeCrac, network, offsetDateTime);
    }

    /**
     * Find a CracCreator for the specified native CRAC format name.
     * @param nativeCracFormat unique identifier of a native CRAC file format
     * @return the importer if one exists for the given format or <code>null</code> otherwise.
     */
    public static CracCreator findCreator(String nativeCracFormat) {
        List<CracCreator> validCracCreators = new ArrayList<>();
        for (CracCreator creator : CRAC_CREATORS.get()) {
            if (creator.getNativeCracFormat().equals(nativeCracFormat)) {
                validCracCreators.add(creator);
            }
        }
        if (validCracCreators.size() == 1) {
            return validCracCreators.get(0);
        } else if (validCracCreators.isEmpty()) {
            return null;
        } else {
            throw new FaraoException(String.format("Several CracCreators found for format %s", nativeCracFormat));
        }
    }
}
