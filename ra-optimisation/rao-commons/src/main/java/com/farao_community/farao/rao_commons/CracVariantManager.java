/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.*;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CracVariantManager {
    private static final String NO_WORKING_VARIANT = "No working variant is defined.";
    private static final String UNKNOWN_VARIANT = "Unknown variant %s";

    private final List<String> variantIds;
    private String workingVariantId;
    private final Crac crac;
    private final Map<String, SystematicSensitivityResult> systematicSensitivityResultMap;

    /**
     * This constructor takes in input the id of an already existing variant. The created CracVariantManager
     * will rely on this given variant, which will be set as the pre-optim variant of the CracVariantManager.
     *
     * @param crac CRAC object.
     * @param cracVariantId given Crac Variant id, which will be used as pre-optim variant
     */
    public CracVariantManager(Crac crac, String cracVariantId) {

        Objects.requireNonNull(cracVariantId);

        this.crac = crac;
        this.variantIds = new ArrayList<>();
        this.systematicSensitivityResultMap = new HashMap<>();
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);

        if (resultVariantManager == null) {
            throw new FaraoException(format("Rao data is based on an existing variant %s but CRAC variant manager does not exist.", cracVariantId));
        }
        if (!resultVariantManager.getVariants().contains(cracVariantId)) {
            throw new FaraoException(format("Rao data is based on an existing variant %s but this variant does not exist.", cracVariantId));
        }

        variantIds.add(cracVariantId);
        systematicSensitivityResultMap.put(cracVariantId, null);
        setWorkingVariant(cracVariantId);
    }

    /**
     * This constructor creates a new data variant with a pre-optimisation prefix and set it as the working variant.
     * So accessing data after this constructor will lead directly to the newly created variant data. CRAC and
     * sensitivity data will be empty. It will create a CRAC ResultVariantManager if it does not exist yet.
     *
     * @param crac CRAC object.
     */
    public CracVariantManager(Crac crac) {
        this.crac = crac;
        this.variantIds = new ArrayList<>();
        this.systematicSensitivityResultMap = new HashMap<>();
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);

        String variantId;
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
            variantId = createVariantFromWorkingVariant(VariantType.INITIAL);
        } else {
            variantId = createVariantFromWorkingVariant(VariantType.PRE_OPTIM);
        }

        setWorkingVariant(variantId);
    }

    public List<String> getVariantIds() {
        return variantIds;
    }

    public String getWorkingVariantId() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return workingVariantId;
    }

    public String getPreOptimVariantId() {
        if (variantIds.isEmpty()) {
            throw new FaraoException("No variants are present in the data");
        }
        return variantIds.get(0);
    }

    public void setWorkingVariant(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(format(UNKNOWN_VARIANT, variantId));
        }
        workingVariantId = variantId;
    }

    public CracResult getCracResult(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(format(UNKNOWN_VARIANT, variantId));
        }
        return crac.getExtension(CracResultExtension.class).getVariant(variantId);
    }

    public CracResult getCracResult() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return getCracResult(workingVariantId);
    }

    public SystematicSensitivityResult getSystematicSensitivityResult() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return systematicSensitivityResultMap.get(workingVariantId);
    }

    public void setSystematicSensitivityResult(SystematicSensitivityResult systematicSensitivityResult) {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        systematicSensitivityResultMap.put(workingVariantId, systematicSensitivityResult);
    }

    // VARIANTS MANAGEMENT METHODS

    public enum VariantType {
        INITIAL,
        PRE_OPTIM,
        POST_OPTIM
    }

    private String createVariantFromWorkingVariant(VariantType variantType) {
        String prefix;
        SystematicSensitivityResult systematicSensitivityResult;
        if (variantType == VariantType.INITIAL) {
            prefix = "initialResults";
            systematicSensitivityResult = null;
        } else if (variantType == VariantType.PRE_OPTIM) {
            prefix = "preOptimisationResults";
            systematicSensitivityResult = null;
        } else {
            prefix = "postOptimisationResults";
            systematicSensitivityResult = getSystematicSensitivityResult();
        }
        String variantId = crac.getExtension(ResultVariantManager.class).createNewUniqueVariantId(prefix);
        //TODO: Copy crac result in the copy ?
        variantIds.add(variantId);
        systematicSensitivityResultMap.put(variantId, systematicSensitivityResult);
        return variantId;
    }

    /**
     * This methods creates a variant from an already existing data variant. CRAC result variant will be
     * created but empty and sensitivity computation results will be copied from the reference variant. The
     * variant ID will contain a post optimisation prefix.
     *
     * @return The data variant ID of the newly created variant.
     * @throws FaraoException if referenceVariantId is not an existing variant of the data.
     */
    public String cloneWorkingVariant() {
        return createVariantFromWorkingVariant(CracVariantManager.VariantType.POST_OPTIM);
    }

    /**
     * This method deletes a variant according to its ID. If the working variant is the variant to be deleted nothing
     * would be done. CRAC result can be kept.
     *
     * @param variantId:      Variant ID that is required to delete.
     * @param keepCracResult: If true it will delete the variant as data variant and the related network variant
     *                        but it will keep the crac variant.
     * @throws FaraoException if variantId is not an existing data variant.
     */
    public void deleteVariant(String variantId, boolean keepCracResult) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(format(UNKNOWN_VARIANT, variantId));
        }
        if (!variantId.equals(workingVariantId)) {
            systematicSensitivityResultMap.remove(variantId);
            if (!keepCracResult) {
                crac.getExtension(ResultVariantManager.class).deleteVariant(variantId);
            }
            variantIds.remove(variantId);
        }
    }

    /**
     * This method clears all the data variants with their related variants in the different data objects. It
     * enables to keep some CRAC result variants as it is results of computation.
     *
     * @param remainingCracResults IDs of the variants we want to keep the results in CRAC
     */
    public void clearWithKeepingCracResults(List<String> remainingCracResults) {
        workingVariantId = null;
        String[] copiedIds = new String[variantIds.size()];
        variantIds.toArray(copiedIds);
        for (String variantId: copiedIds) {
            deleteVariant(variantId, remainingCracResults.contains(variantId));
        }
        variantIds.clear();
    }

    /**
     * This method clear all the data variants with their related variants in the different data objects.
     */
    public void clear() {
        clearWithKeepingCracResults(Collections.emptyList());
    }
}
