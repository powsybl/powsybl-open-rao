/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * A LinearRaoData is an object that gathers Network, Crac and SystematicSensitivityAnalysisResult data. It manages
 * variants of these objects to ensure data consistency at any moment. Network will remain the same at any moment
 * with no variant management. It is a single point of entry to manipulate all data related to linear rao with
 * variant management.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoData {

    private static final String NO_WORKING_VARIANT = "No working variant is defined.";
    private static final String UNKNOWN_VARIANT = "Unknown variant %s";

    private List<String> variantIds;
    private String workingVariantId;
    private Network network;
    private Crac crac;
    private Map<String, SystematicSensitivityAnalysisResult> systematicSensitivityAnalysisResultMap;

    /**
     * This constructor creates a new data variant with a pre-optimisation prefix and set it as the working variant.
     * So accessing data after this constructor will lead directly to the newly created variant data. CRAC and
     * sensitivity data will be empty. It will create a CRAC ResultVariantManager if it does not exist yet.
     *
     * @param network: Network object.
     * @param crac: CRAC object.
     */
    public RaoData(Network network, Crac crac) {
        this.network = network;
        this.crac = crac;
        this.variantIds = new ArrayList<>();
        this.systematicSensitivityAnalysisResultMap = new HashMap<>();

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }

        String variantId = createVariantFromWorkingVariant(VariantType.PRE_OPTIM);
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

    public String getInitialVariantId() {
        if (variantIds.isEmpty()) {
            throw new FaraoException("No variants are present in the data");
        }
        return variantIds.get(0);
    }

    public void setWorkingVariant(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        workingVariantId = variantId;
    }

    public Network getNetwork() {
        return network;
    }

    public Crac getCrac() {
        return crac;
    }

    public CracResult getCracResult(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        return crac.getExtension(CracResultExtension.class).getVariant(variantId);
    }

    public CracResult getCracResult() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return getCracResult(workingVariantId);
    }

    public SystematicSensitivityAnalysisResult getSystematicSensitivityAnalysisResult() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return systematicSensitivityAnalysisResultMap.get(workingVariantId);
    }

    public void setSystematicSensitivityAnalysisResult(SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        systematicSensitivityAnalysisResultMap.put(workingVariantId, systematicSensitivityAnalysisResult);
    }

    public boolean hasSensitivityValues() {
        return getSystematicSensitivityAnalysisResult() != null;
    }

    public double getReferenceFlow(Cnec cnec) {
        return getSystematicSensitivityAnalysisResult().getReferenceFlow(cnec);
    }

    public double getSensitivity(Cnec cnec, RangeAction rangeAction) {
        return getSystematicSensitivityAnalysisResult().getSensitivityOnFlow(rangeAction, cnec);
    }

    /**
     * This method works from the working variant. It is filling CRAC result extension of the working variant
     * with values in network of the working variant.
     */
    public void fillRangeActionResultsWithNetworkValues() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        String preventiveState = getCrac().getPreventiveState().getId();
        for (RangeAction rangeAction : getCrac().getRangeActions()) {
            double valueInNetwork = rangeAction.getCurrentValue(getNetwork());
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(workingVariantId);
            rangeActionResult.setSetPoint(preventiveState, valueInNetwork);
            if (rangeAction instanceof PstRange) {
                ((PstRangeResult) rangeActionResult).setTap(preventiveState, ((PstRange) rangeAction).computeTapPosition(valueInNetwork));
            }
        }
    }

    /**
     * This method works from the working variant. It is applying on the network working variant
     * according to the values present in the CRAC result extension of the working variant.
     */
    public void applyRangeActionResultsOnNetwork() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        String preventiveState = getCrac().getPreventiveState().getId();
        for (RangeAction rangeAction : getCrac().getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            rangeAction.apply(getNetwork(), rangeActionResultMap.getVariant(workingVariantId).getSetPoint(preventiveState));
        }
    }

    /**
     * This method compares CRAC result extension of two different variants. It compares the set point values
     * of all the range actions.
     *
     * @param variantId1: First variant to compare.
     * @param variantId2: Second variant to compare.
     * @return True if all the range actions are set at the same values and false otherwise.
     */
    public boolean sameRemedialActions(String variantId1, String variantId2) {
        String preventiveState = getCrac().getPreventiveState().getId();
        for (RangeAction rangeAction : getCrac().getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            double value1 = rangeActionResultMap.getVariant(variantId1).getSetPoint(preventiveState);
            double value2 = rangeActionResultMap.getVariant(variantId2).getSetPoint(preventiveState);
            if (value1 != value2 && (!Double.isNaN(value1) || !Double.isNaN(value2))) {
                return false;
            }
        }
        return true;
    }

    // VARIANTS MANAGEMENT METHODS

    public enum VariantType {
        PRE_OPTIM,
        POST_OPTIM
    }

    private String createVariantFromWorkingVariant(VariantType variantType) {
        String prefix;
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult;
        if (variantType == VariantType.PRE_OPTIM) {
            prefix = "preOptimisationResults";
            systematicSensitivityAnalysisResult = null;
        } else {
            prefix = "postOptimisationResults";
            systematicSensitivityAnalysisResult = getSystematicSensitivityAnalysisResult();
        }
        String variantId = crac.getExtension(ResultVariantManager.class).createNewUniqueVariantId(prefix);
        //TODO: Copy crac result in the copy ?
        variantIds.add(variantId);
        systematicSensitivityAnalysisResultMap.put(variantId, systematicSensitivityAnalysisResult);
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
        return createVariantFromWorkingVariant(VariantType.POST_OPTIM);
    }

    /**
     * This method deletes a variant according to its ID. If the working variant is the variant to be deleted nothing
     * would be done. CRAC result can be kept.
     *
     * @param variantId: Variant ID that is required to delete.
     * @param keepCracResult: If true it will delete the variant as data variant and the related network variant
     *                      but it will keep the crac variant.
     * @throws FaraoException if variantId is not an existing data variant.
     */
    public void deleteVariant(String variantId, boolean keepCracResult) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        if (!variantId.equals(workingVariantId)) {
            systematicSensitivityAnalysisResultMap.remove(variantId);
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
    void clear() {
        clearWithKeepingCracResults(Collections.emptyList());
    }
}
