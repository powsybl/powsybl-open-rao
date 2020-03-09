/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecResultsExtension extends AbstractExtension<Cnec> {

    private Map<String, CnecResult> cnecResultMap;

    public CnecResultsExtension() {
        cnecResultMap = new HashMap<>();
    }

    CnecResultsExtension(String firstVariant) {
        cnecResultMap = new HashMap<>();
        cnecResultMap.put(firstVariant, new CnecResult());
    }

    public CnecResult getVariant(String variantId) {
        return cnecResultMap.get(variantId);
    }

    public Map<String, CnecResult> getCnecResultMap() {
        return cnecResultMap;
    }

    public CnecResult addVariant(String newVariantId) {
        return addVariant(newVariantId, new CnecResult());
    }

    public CnecResult addVariant(String newVariantId, CnecResult cnecResult) {
        if (cnecResultMap.containsKey(newVariantId)) {
            throw new FaraoException(String.format("Cannot create CnecResult variant with id [%s] for Cnec [%s] as it already exists", newVariantId, getExtendable().getId()));
        }
        cnecResultMap.put(newVariantId, cnecResult);
        return cnecResultMap.get(newVariantId);
    }

    void deleteVariant(String variantId) {
        cnecResultMap.remove(variantId);
    }

    @Override
    public String getName() {
        return "CnecResultsExtension";
    }
}
