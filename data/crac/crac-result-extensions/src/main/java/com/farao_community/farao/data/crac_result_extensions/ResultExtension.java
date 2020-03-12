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
 * ResultExtension is a standard Extension of any {@link Identifiable} object
 * which can contain results of a RAO computation.
 *
 * ResultExtension contains a map of results, with one {@link Result} object for
 * each registered variant. It also contains utility methods to manage the result
 * variants of an Identifiable object.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ResultExtension<T extends Identifiable<T>, S extends Result<T>> extends AbstractExtension<T> {

    /**
     * Map of results, with one result "S" of each variantId "String"
     */
    private Map<String, S> resultMap;

    /**
     * Default constructor.
     * Private-package as the extensions can only be created by the ResultVariantManager
     * to ensure the consistency of ResultExtensions within all the Crac.
     */
    ResultExtension() {
        resultMap = new HashMap<>();
    }

    /**
     * Get the complete map of results
     */
    Map<String, S> getResultMap() {
        return resultMap;
    }

    /**
     * Set the complete map of results
     */
    void setResultMap(Map<String, S> resultMap) {
        this.resultMap = resultMap;
    }

    /**
     * Get the result for the variant with id variantId. Return null if the variant
     * does not exist.
     */
    public S getVariant(String variantId) {
        return resultMap.get(variantId);
    }

    /**
     * Add the result resultElement for the new variant with id newVariantId.
     * Private-package as the variants can only be created by the ResultVariantManager
     * to ensure the consistency of variants within all the Crac.
     */
    void addVariant(String newVariantId, S resultElement) {
        if (resultMap.containsKey(newVariantId)) {
            throw new FaraoException(String.format("Cannot create result variant with id [%s] for [%s] as it already exists", newVariantId, getExtendable().getId()));
        }
        resultMap.put(newVariantId, resultElement);
    }

    /**
     * Remove the results for the variant with id variantId.
     * Private-package as the variants can only be deleted by the ResultVariantManager
     * to ensure the consistency of variants within all the Crac.
     */
    void deleteVariant(String variantId) {
        resultMap.remove(variantId);
    }

    /**
     * Extension name
     */
    @Override
    public String getName() {
        return "ResultExtension";
    }
}
