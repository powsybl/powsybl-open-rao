/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.HashSet;
import java.util.Set;

/**
 * The Crac can contain several variants of results.
 *
 * For instance, one variant for the initial newtork situation, with flows and margins
 * of the Cnec without any remedial actions application. And one variant for the optimal
 * application of the remedial actions, with the remedial actions which have been applied
 * and the flows and margin after the optimisation of remedial actions.
 *
 * The ResultVariantManager references all the variants present in the Crac. And offers
 * some utility methods to handle those variants.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ResultVariantManager extends AbstractExtension<Crac> {

    private Set<String> variantsId;

    @Override
    public String getName() {
        return "ResultVariantManager";
    }

    public ResultVariantManager() {
        variantsId = new HashSet<>();
    }

    /**
     * Get the ids of all the variants present in the Crac
     */
    Set<String> getVariants() {
        return variantsId;
    }

    /**
     * Create a new variant. For all extendable object
     */
    void createVariant(String variantId) {

        if (variantsId.contains(variantId)) {
            throw new FaraoException(String.format("Cannot create results variant with id [%s], as one with the same id already exists", variantId));
        }

        // todo : if no Result extensions exists for the Cnecs, the Crac and the RemedialActions -> create one
        // todo : in the Result extensions of the Cnec, Crac and RemedialActions, create a variant with id
        //        {variantId} and default result values

        variantsId.add(variantId);
    }

    /**
     * Delete an existing variant.
     */
    void deleteVariant(String variantId) {

        if (!variantsId.contains(variantId)) {
            throw new FaraoException(String.format("Cannot delete variant with id [%s], as it does not exist", variantId));
        }

        // todo : in the Result extensions of the Cnec, Crac and RemedialActions, delete the variant with id
        //        {variantId}

        variantsId.remove(variantId);
    }
}
