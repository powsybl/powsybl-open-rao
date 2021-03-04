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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class CracCreators {

    private static final Supplier<List<CracCreator>> CRAC_CREATORS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracCreator.class).getServices())::get;

    private CracCreators() {
    }

    public static CracCreationResult createCrac(RawCrac rawCrac, Network network, OffsetDateTime offsetDateTime) {
        CracCreator creator = findCreator(rawCrac);
        return creator.createCrac(rawCrac, network, offsetDateTime);
    }

    public static CracCreator findCreator(RawCrac rawCrac) {
        List<CracCreator> validCracCreators = new ArrayList<>();
        for (CracCreator creator : CRAC_CREATORS.get()) {
            if (creator.getRawCracFormat().equals(rawCrac.getFormat())) {
                validCracCreators.add(creator);
            }
        }
        if (validCracCreators.size() == 1) {
            return validCracCreators.get(0);
        } else if (validCracCreators.size() == 0) {
            throw new FaraoException(String.format("No CracCreator found for format %s", rawCrac.getFormat()));
        } else {
            throw new FaraoException(String.format("Several CracCreators found for format %s", rawCrac.getFormat()));
        }
    }
}
