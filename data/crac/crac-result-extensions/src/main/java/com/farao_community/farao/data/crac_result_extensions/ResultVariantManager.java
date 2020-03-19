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
import java.util.stream.Collectors;

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

    /**
     * Constructor which take in input a list of already existing variantIds
     * Private-package, used only for the JSON import
     */
    ResultVariantManager(Set<String> variantIdSet) {
        variants = variantIdSet;
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
     * Create a new variant.
     * If they do not exist, add a {@link AbstractResultExtension} to all the Cnecs, RangeActions,
     * NetworkActions and the Crac itself.
     * Add a new {@link Result} variant, with default values, to all the ResultExtensions
     * of the Crac.
     */
    @SuppressWarnings("unchecked")
    public void createVariant(String variantId) {

        if (variants.contains(variantId)) {
            throw new FaraoException(String.format("Cannot create results variant with id [%s], as one with the same id already exists", variantId));
        }

        Set<String> stateIds = getExtendable().getStates().stream().map(State::getId).collect(Collectors.toSet());

        // add CRAC result variant
        if (getExtendable().getExtension(CracResultExtension.class) == null) {
            getExtendable().addExtension(CracResultExtension.class, new CracResultExtension());
        }
        getExtendable().getExtension(CracResultExtension.class).addVariant(variantId, new CracResult());

        // add CNEC result variant
        getExtendable().getCnecs().forEach(cnec -> {
            if (cnec.getExtension(CnecResultExtension.class) == null) {
                cnec.addExtension(CnecResultExtension.class, new CnecResultExtension());
            }
            cnec.getExtension(CnecResultExtension.class).addVariant(variantId, new CnecResult());
        });

        // add Network Action result variant
        for (NetworkAction<?> networkAction: getExtendable().getNetworkActions()) {
            if (networkAction.getExtension(NetworkActionResultExtension.class) == null) {
                networkAction.addExtension(NetworkActionResultExtension.class, new NetworkActionResultExtension());
            }
            networkAction.getExtension(NetworkActionResultExtension.class).addVariant(variantId, new NetworkActionResult(stateIds));
        }

        // add Range Action result variant
        for (RangeAction<?> rangeAction: getExtendable().getRangeActions()) {
            if (rangeAction instanceof PstRange) {
                if (rangeAction.getExtension(PstRangeResultExtension.class) == null) {
                    rangeAction.addExtension(PstRangeResultExtension.class, new PstRangeResultExtension());
                }
                rangeAction.getExtension(PstRangeResultExtension.class).addVariant(variantId, new PstRangeResult(stateIds));
            } else {
                if (rangeAction.getExtension(RangeActionResultExtension.class) == null) {
                    rangeAction.addExtension(RangeActionResultExtension.class, new RangeActionResultExtension());
                }
                rangeAction.getExtension(RangeActionResultExtension.class).addVariant(variantId, new RangeActionResult(stateIds));
            }
        }

        // add variant in variant map
        variants.add(variantId);
    }

    /**
     * Delete the variant with id variantId
     * Remove the {@link Result} associated to the variant to be deleted of all the
     * {@link AbstractResultExtension} of the Crac.
     */
    @SuppressWarnings("unchecked")
    public void deleteVariant(String variantId) {

        if (!variants.contains(variantId)) {
            throw new FaraoException(String.format("Cannot delete variant with id [%s], as it does not exist", variantId));
        }

        if (variants.size() == 1) { // if the crac does not contains other variant than this one : delete all extension
            getExtendable().removeExtension(CracResultExtension.class);

            getExtendable().getCnecs().forEach(cnec -> cnec.removeExtension(CnecResultExtension.class));

            for (NetworkAction<?> networkAction: getExtendable().getNetworkActions()) {
                networkAction.removeExtension(NetworkActionResultExtension.class);
            }

            for (RangeAction<?> rangeAction: getExtendable().getRangeActions()) {
                if (rangeAction instanceof PstRange) {
                    rangeAction.removeExtension(PstRangeResultExtension.class);
                } else {
                    rangeAction.removeExtension(RangeActionResultExtension.class);
                }
            }

        } else { // else, delete the variants

            getExtendable().getExtension(CracResultExtension.class).deleteVariant(variantId);

            getExtendable().getCnecs().forEach(cnec -> cnec.getExtension(CnecResultExtension.class).deleteVariant(variantId));

            for (NetworkAction<?> networkAction: getExtendable().getNetworkActions()) {
                networkAction.getExtension(NetworkActionResultExtension.class).deleteVariant(variantId);
            }

            for (RangeAction<?> rangeAction: getExtendable().getRangeActions()) {
                if (rangeAction instanceof PstRange) {
                    rangeAction.getExtension(PstRangeResultExtension.class).deleteVariant(variantId);
                } else {
                    rangeAction.getExtension(RangeActionResultExtension.class).deleteVariant(variantId);
                }
            }
        }

        variants.remove(variantId);
    }
}
