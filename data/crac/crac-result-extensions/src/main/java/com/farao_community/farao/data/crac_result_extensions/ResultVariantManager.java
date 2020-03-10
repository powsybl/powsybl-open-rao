/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.State;
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

    private Set<String> variants;

    /**
     * Default constructor
     */
    public ResultVariantManager() {
        variants = new HashSet<>();
    }

    @Override
    public String getName() {
        return "ResultVariantManager";
    }

    /**
     * Get the ids of all the variants present in the Crac
     */
    public Set<String> getVariants() {
        return variants;
    }

    /**
     * Create a new variant. For all extendable object
     */
    public void createVariant(String variantId) {

        if (variants.contains(variantId)) {
            throw new FaraoException(String.format("Cannot create results variant with id [%s], as one with the same id already exists", variantId));
        }

        Set<State> states = getExtendable().getStates();

        // add CRAC result variant
        if(getExtendable().getExtension(CracResult.class) == null) {
            getExtendable().addExtension(CracResultsExtension.class, new CracResultsExtension());
        }
        getExtendable().getExtension(CracResultsExtension.class).addVariant(variantId);


        // add CNEC result variant
        getExtendable().getCnecs().forEach(cnec -> {
            if(cnec.getExtension(CnecResultsExtension.class) == null) {
                cnec.addExtension(CnecResultsExtension.class, new CnecResultsExtension());
            }
            cnec.getExtension(CnecResultsExtension.class).addVariant(variantId);
        });

        // add Network Action result variant
        getExtendable().getNetworkActions().forEach(na -> {
            if(na.getExtension(NetworkActionResultsExtension.class) == null) {
                na.addExtension(NetworkActionResultsExtension.class, new NetworkActionResultsExtension());
            }
            na.getExtension(NetworkActionResultsExtension.class).addVariant(variantId, states);
        });

        // add Range Action result variant
        getExtendable().getRangeActions().forEach(ra -> {
            if(ra instanceof PstRange) {
                PstRange pstRa = (PstRange) ra;
                if (pstRa.getExtension(PstRangeResultsExtension.class) == null) {
                    pstRa.addExtension(PstRangeResultsExtension.class, new PstRangeResultsExtension());
                }
                pstRa.getExtension(PstRangeResultsExtension.class).addVariant(variantId, states);
            }
            // other RangeActions than PstRange are not handled for now
        });

        // add variant in variant map
        variants.add(variantId);
    }

    /**
     * Delete an existing variant.
     */
    public void deleteVariant(String variantId) {

        if (!variants.contains(variantId)) {
            throw new FaraoException(String.format("Cannot delete variant with id [%s], as it does not exist", variantId));
        }

        // todo : in the Result extensions of the Cnec, Crac and RemedialActions, delete the variant with id
        //        {variantId}

        variants.remove(variantId);
    }
}
