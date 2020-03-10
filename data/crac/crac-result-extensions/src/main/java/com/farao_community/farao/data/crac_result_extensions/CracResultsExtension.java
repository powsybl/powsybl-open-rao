/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracResultsExtension extends AbstractResultExtension<Crac, CracResult> {

    public CracResult addVariant(String newVariantId) {
        return addVariant(newVariantId, new CracResult());
    }

    @Override
    public String getName() {
        return "CracResultsExtension";
    }

}
