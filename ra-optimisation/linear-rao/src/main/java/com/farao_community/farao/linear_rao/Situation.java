/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * A Situation is an object that gathers Network, Crac and SystematicSensitivityAnalysisResult data. It manages
 * variants of these objects to ensure data consistency at any moment. It is a single point of entry to manipulate
 * all network situation data with variant management.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class Situation {

    private static final String NO_WORKING_VARIANT = "No working variant is defined.";
    private static final String UNKNOWN_VARIANT = "Unknown situation variant %s";

    private List<String> variantIds;
    private String initialNetworkVariantId;
    private String workingVariantId;
    private Network network;
    private Crac crac;
    private Map<String, SystematicSensitivityAnalysisResult> systematicSensitivityAnalysisResultMap;

    /**
     * This constructor creates a new situation variant and set it as the working variant. So accessing data
     * after this constructor will lead directly to the newly created variant data. CRAC and sensitivity data will
     * be empty and network data will be copied from the initial network variant. It will create a CRAC
     * ResultVariantManager if it does not exist yet.
     *
     * @param network: Network object.
     * @param crac: CRAC object.
     */
    public Situation(Network network, Crac crac) {
        this.network = network;
        this.initialNetworkVariantId = network.getVariantManager().getWorkingVariantId();
        this.crac = crac;
        this.variantIds = new ArrayList<>();
        this.systematicSensitivityAnalysisResultMap = new HashMap<>();

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }

        String situationVariantId = createVariant();
        setWorkingVariant(situationVariantId);
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
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
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
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        network.getVariantManager().setWorkingVariant(workingVariantId);
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

    /**
     * This method generates a unique variant ID of a network according to the IDs already present in the network.
     *
     * @return A unique ID of the network as a string.
     */
    private String getUniqueNetworkVariantId() {
        String uniqueId;
        do {
            uniqueId = UUID.randomUUID().toString();
        } while (network.getVariantManager().getVariantIds().contains(uniqueId));
        return uniqueId;
    }

    /**
     * This method generates a unique variant ID of a situation. This ID has to be unique for the network
     * and for the CRAC.
     *
     * @return A unique ID a the situation as a string.
     */
    private String getUniqueSituationId() {
        String networkVariantId = getUniqueNetworkVariantId();
        String cracVariantId = crac.getExtension(ResultVariantManager.class).getUniqueVariantId();
        return networkVariantId.concat(cracVariantId);
    }

    private String createVariantFromNetworkVariant(String networkVariantId) {
        String situationVariantId = getUniqueSituationId();
        network.getVariantManager().cloneVariant(networkVariantId, situationVariantId);
        crac.getExtension(ResultVariantManager.class).createVariant(situationVariantId);
        variantIds.add(situationVariantId);
        systematicSensitivityAnalysisResultMap.put(situationVariantId, null);
        return situationVariantId;
    }

    /**
     * This methods creates a variant from the network working variant. It is useful for the constructor or
     * after a clear method that will erase all the situation variants. Except these two cases it is advised
     * to use cloneVariant method instead. Network variant will be copied from its working variant,
     * CRAC result variant will be created but empty as well as sensitivity computation results variant.
     *
     * @return The situation variant ID of the newly created variant.
     */
    String createVariant() {
        return createVariantFromNetworkVariant(network.getVariantManager().getWorkingVariantId());
    }

    /**
     * This methods creates a variant from an already existing situation variant. It is advised to use this method
     * to create a new variant. Network variant will be copied from the reference variant, CRAC result variant will be
     * created but empty and sensitivity computation results will be copied from the reference variant.
     *
     * @param referenceVariantId: Already existing situation variant ID.
     * @return The situation variant ID of the newly created variant.
     * @throws FaraoException if referenceVariantId is not an existing variant of the situation.
     */
    String cloneVariant(String referenceVariantId) {
        if (!variantIds.contains(referenceVariantId)) {
            throw new FaraoException(String.format("Situation does not contain %s  as reference variant", referenceVariantId));
        }
        String situationVariantId = createVariantFromNetworkVariant(referenceVariantId);
        systematicSensitivityAnalysisResultMap.put(situationVariantId, getSystematicSensitivityAnalysisResult());
        return situationVariantId;
    }

    /**
     * This method deletes a variant according to its ID. If the working variant is the variant to be deleted nothing
     * would be done.
     *
     * @param variantId: Variant ID that is required to delete.
     * @param keepCracResult: If true it will delete the variant as situation variant and the related network variant
     *                      but it will keep the crac variant.
     * @throws FaraoException if variantId is not an existing variant of the situation.
     */
    void deleteVariant(String variantId, boolean keepCracResult) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        if (!variantId.equals(workingVariantId)) {
            network.getVariantManager().removeVariant(variantId);
            systematicSensitivityAnalysisResultMap.remove(variantId);
            if (!keepCracResult) {
                crac.getExtension(ResultVariantManager.class).deleteVariant(variantId);
            }
            variantIds.remove(variantId);
        }
    }

    /**
     * This method clear all the situation variants with their related variants in the different data objects. It
     * enables to keep some CRAC result variants as it is results.
     *
     * @param remainingVariantsForCrac IDs of the variants we want to keep the results in CRAC
     */
    void clear(List<String> remainingVariantsForCrac) {
        network.getVariantManager().setWorkingVariant(initialNetworkVariantId);
        workingVariantId = null;
        String[] copiedIds = new String[variantIds.size()];
        variantIds.toArray(copiedIds);
        for (String variantId: copiedIds) {
            deleteVariant(variantId, remainingVariantsForCrac.contains(variantId));
        }
        variantIds.clear();
    }

    /**
     * This method clear all the situation variants with their related variants in the different data objects.
     */
    void clear() {
        clear(Collections.emptyList());
    }
}
