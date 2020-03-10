/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
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
    @SuppressWarnings("unchecked")
    public void createVariant(String variantId) {

        if (variants.contains(variantId)) {
            throw new FaraoException(String.format("Cannot create results variant with id [%s], as one with the same id already exists", variantId));
        }

        Set<State> states = getExtendable().getStates();

        // add CRAC result variant
        if(getExtendable().getExtension(ResultExtension.class) == null) {
            getExtendable().addExtension(ResultExtension.class, new ResultExtension<Crac, CracResult>());
        }
        getExtendable().getExtension(ResultExtension.class).addVariant(variantId, new CracResult());

        // add CNEC result variant
        getExtendable().getCnecs().forEach(cnec -> {
            if(cnec.getExtension(ResultExtension.class) == null) {
                cnec.addExtension(ResultExtension.class, new ResultExtension<Cnec, CnecResult>());
            }
            cnec.getExtension(ResultExtension.class).addVariant(variantId, new CnecResult());
        });

        // add Network Action result variant
        getExtendable().getNetworkActions().forEach(na -> {
            if(na.getExtension(ResultExtension.class) == null) {
                na.addExtension(ResultExtension.class, new ResultExtension<NetworkAction, NetworkActionResult>());
            }
            na.getExtension(ResultExtension.class).addVariant(variantId, new NetworkActionResult(states));
        });

        // add Range Action result variant
        getExtendable().getRangeActions().forEach(ra -> {
            if(ra instanceof PstRange) {
                PstRange pstRa = (PstRange) ra;
                if (pstRa.getExtension(ResultExtension.class) == null) {
                    pstRa.addExtension(ResultExtension.class, new ResultExtension<PstRange, PstRangeResult>());
                }
                pstRa.getExtension(ResultExtension.class).addVariant(variantId, new PstRangeResult(states)
                );
            }
            // other RangeActions than PstRange are not handled
        });

        // add variant in variant map
        variants.add(variantId);
    }

    /**
     * Delete an existing variant.
     */
    @SuppressWarnings("unchecked")
    public void deleteVariant(String variantId) {

        if (!variants.contains(variantId)) {
            throw new FaraoException(String.format("Cannot delete variant with id [%s], as it does not exist", variantId));
        }

        if(variants.size() == 1) { // if the crac does not contains other variant than this one : delete all extension
            getExtendable().removeExtension(ResultExtension.class);

            getExtendable().getCnecs().forEach(cnec -> {
                cnec.removeExtension(ResultExtension.class);
            });

            getExtendable().getNetworkActions().forEach(na -> {
                na.removeExtension(ResultExtension.class);
            });

            getExtendable().getRangeActions().forEach(ra -> {
                if (ra instanceof PstRange) {
                    PstRange pstRa = (PstRange) ra;
                    pstRa.removeExtension(ResultExtension.class);
                }
            });

        } else { // else, delete the variants

            getExtendable().getExtension(ResultExtension.class).deleteVariant(variantId);

            getExtendable().getCnecs().forEach(cnec -> {
                cnec.getExtension(ResultExtension.class).deleteVariant(variantId);
            });

            getExtendable().getNetworkActions().forEach(na -> {
                na.getExtension(ResultExtension.class).deleteVariant(variantId);
            });

            getExtendable().getRangeActions().forEach(ra -> {
                if (ra instanceof PstRange) {
                    PstRange pstRa = (PstRange) ra;
                    pstRa.getExtension(ResultExtension.class).deleteVariant(variantId);
                }
            });
        }

        variants.remove(variantId);
    }
}


