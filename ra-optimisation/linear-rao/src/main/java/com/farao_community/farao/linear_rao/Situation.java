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
 * An AbstractSituation includes a set of information associated to a given
 * network situation.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class Situation {

    private static final String NO_WORKING_VARIANT = "No working variant is defined.";
    private static final String UNKNOWN_VARIANT = "Unknown situation variant %s";

    private List<String> variantIds;

    private String workingVariantId;

    /**
     * Network object
     */
    private Network network;

    private String initialNetworkVariantId;

    /**
     * Crac object
     */
    private Crac crac;

    private Map<String, SystematicSensitivityAnalysisResult> systematicSensitivityAnalysisResultMap;

    /**
     * constructor
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
        init();
    }

    /**
     * Add in the Crac extension the initial RangeActions set-points
     */
    private void init() {
        String preventiveState = getCrac().getPreventiveState().getId();
        for (RangeAction rangeAction : getCrac().getRangeActions()) {
            double valueInNetwork = rangeAction.getCurrentValue(network);
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(workingVariantId);
            rangeActionResult.setSetPoint(preventiveState, valueInNetwork);
            if (rangeAction instanceof PstRange) {
                ((PstRangeResult) rangeActionResult).setTap(preventiveState, ((PstRange) rangeAction).computeTapPosition(valueInNetwork));
            }
        }
    }

    public SystematicSensitivityAnalysisResult getSystematicSensitivityAnalysisResult() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return systematicSensitivityAnalysisResultMap.get(workingVariantId);
    }

    public SystematicSensitivityAnalysisResult getSystematicSensitivityAnalysisResult(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        return systematicSensitivityAnalysisResultMap.get(variantId);
    }

    public void setSystematicSensitivityAnalysisResult(SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        systematicSensitivityAnalysisResultMap.put(workingVariantId, systematicSensitivityAnalysisResult);
    }

    /**
     * get a unique network variant ID
     */
    private String getUniqueNetworkVariantId() {
        String uniqueId;
        do {
            uniqueId = UUID.randomUUID().toString();
        } while (network.getVariantManager().getVariantIds().contains(uniqueId));
        return uniqueId;
    }

    private String getUniqueSituationId() {
        String networkVariantId = getUniqueNetworkVariantId();
        String cracVariantId = crac.getExtension(ResultVariantManager.class).getUniqueVariantId();
        return networkVariantId.concat(cracVariantId);
    }

    /**
     * Compare the network situations (i.e. the RangeActions set-points) of two
     * AbstractSituation. Returns true if the situations are identical, and false
     * if they are not.
     */
    boolean sameRemedialActions(String id1, String id2) {
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            double value1 = rangeActionResultMap.getVariant(id1).getSetPoint(preventiveState);
            double value2 = rangeActionResultMap.getVariant(id2).getSetPoint(preventiveState);
            if (value1 != value2 && (!Double.isNaN(value1) || !Double.isNaN(value2))) {
                return false;
            }
        }
        return true;
    }

    public List<String> getVariantIds() {
        return variantIds;
    }

    String createVariant() {
        String situationVariantId = getUniqueSituationId();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), situationVariantId);
        crac.getExtension(ResultVariantManager.class).createVariant(situationVariantId);
        variantIds.add(situationVariantId);
        systematicSensitivityAnalysisResultMap.put(situationVariantId, null);
        return situationVariantId;
    }

    String cloneVariant(String referenceVariantId) {
        if (!variantIds.contains(referenceVariantId)) {
            throw new FaraoException(String.format("Situation does not contain %s  as reference variant", referenceVariantId));
        }
        String situationVariantId = getUniqueSituationId();
        network.getVariantManager().cloneVariant(referenceVariantId, situationVariantId);
        crac.getExtension(ResultVariantManager.class).createVariant(situationVariantId);
        variantIds.add(situationVariantId);
        systematicSensitivityAnalysisResultMap.put(situationVariantId, null);
        return situationVariantId;
    }

    public String getWorkingVariantId() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return workingVariantId;
    }

    Situation setWorkingVariant(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        workingVariantId = variantId;
        return this;
    }

    /**
     * This method deletes a variant according to its ID. If the variant ID is not present in the situation
     * it will throw an error. If the working variant is the variant to be deleted nothing would be done.
     * @param variantId: Variant ID that is required to delete.
     * @param keepCracResult: If true it will delete the variant as situation variant and the related network variant
     *                      but it will keep the crac variant.
     */
    void deleteVariant(String variantId, boolean keepCracResult) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format(UNKNOWN_VARIANT, variantId));
        }
        if (!variantId.equals(workingVariantId)) {
            network.getVariantManager().removeVariant(variantId);
            if (!keepCracResult) {
                crac.getExtension(ResultVariantManager.class).deleteVariant(variantId);
            }
            variantIds.remove(variantId);
        }
    }

    void clear() {
        clear(Collections.emptyList());
    }

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
     * Getters and setters
     */
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

    double getCost() {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        return getCost(workingVariantId);
    }

    double getCost(String variantId) {
        return crac.getExtension(CracResultExtension.class).getVariant(variantId).getCost();
    }

    void setCost(double cost) {
        if (workingVariantId == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        crac.getExtension(CracResultExtension.class).getVariant(workingVariantId).setCost(cost);
    }
}
