/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Cnec;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecResultsExtension extends ResultExtension<Cnec, CnecResult> {

    public CnecResult addVariant(String newVariantId) {
        return addVariant(newVariantId, new CnecResult());
    }

    @Override
    public String getName() {
        return "CnecResultsExtension";
    }
}
