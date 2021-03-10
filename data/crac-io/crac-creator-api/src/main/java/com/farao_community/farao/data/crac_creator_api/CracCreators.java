/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creator_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.raw_crac_api.RawCrac;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.iidm.network.Network;

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
     * Flexible method to create a Crac from a RawCrac, whatever its format
     */
    public static CracCreationResult createCrac(RawCrac rawCrac, Network network, OffsetDateTime offsetDateTime) {
        CracCreator creator = findCreator(rawCrac.getFormat());

        if (Objects.isNull(creator)) {
            throw new FaraoException(String.format("No CracCreator found for format %s", rawCrac.getFormat()));
        }

        return creator.createCrac(rawCrac, network, offsetDateTime);
    }

    /**
     * Find a CracCreator for the specified RawCrac format name.
     * @param rawCracFormat unique identifier of a raw CRAC file format
     * @return the importer if one exists for the given format or <code>null</code> otherwise.
     */
    public static CracCreator findCreator(String rawCracFormat) {
        List<CracCreator> validCracCreators = new ArrayList<>();
        for (CracCreator creator : CRAC_CREATORS.get()) {
            if (creator.getRawCracFormat().equals(rawCracFormat)) {
                validCracCreators.add(creator);
            }
        }
        if (validCracCreators.size() == 1) {
            return validCracCreators.get(0);
        } else if (validCracCreators.size() == 0) {
            return null;
        } else {
            throw new FaraoException(String.format("Several CracCreators found for format %s", rawCracFormat));
        }
    }
}
