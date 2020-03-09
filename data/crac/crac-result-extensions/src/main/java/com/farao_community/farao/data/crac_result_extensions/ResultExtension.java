/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class ResultExtension<T extends Identifiable, S> extends AbstractExtension<T> {

    private Map<String, S> resultMap;

    public ResultExtension() {
        resultMap = new HashMap<>();
    }

    public S getVariant(String variantId) {
        return resultMap.get(variantId);
    }

    public Map<String, S> getCnecResultMap() {
        return resultMap;
    }

    public S addVariant(String newVariantId, S resultElement) {
        if (resultMap.containsKey(newVariantId)) {
            throw new FaraoException(String.format("Cannot create result variant with id [%s] for [%s] as it already exists", newVariantId, getExtendable().getId()));
        }
        resultMap.put(newVariantId, resultElement);
        return resultMap.get(newVariantId);
    }

    void deleteVariant(String variantId) {
        resultMap.remove(variantId);
    }
}
