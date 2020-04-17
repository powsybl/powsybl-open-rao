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

    private List<String> variantIds;

    private String workingVariantId;

    /**
     * Network object
     */
    private Network network;

    /**
     * Crac object
     */
    private Crac crac;

    /**
     * Results of the systematic sensitivity analysis performed on the situation
     */
    private Map<String, SystematicSensitivityAnalysisResult> systematicSensitivityAnalysisResultMap;

    /**
     * constructor
     */
    public Situation(Network network, Crac crac) {
        this.network = network;
        this.crac = crac;
        this.variantIds = new ArrayList<>();

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }

        String situationVariantId = getUniqueSituationId();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), situationVariantId);
        crac.getExtension(ResultVariantManager.class).createVariant(situationVariantId);
        variantIds.add(situationVariantId);
        setWorkingVariant(situationVariantId);
        init();

        this.systematicSensitivityAnalysisResultMap = new HashMap<>();
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
        return String.valueOf(networkVariantId.concat(cracVariantId).hashCode());
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

    String cloneVariant(String referenceVariantId) {
        if (!variantIds.contains(referenceVariantId)) {
            throw new FaraoException(String.format("Situation does not contain %s  as reference variant", referenceVariantId));
        }
        String situationVariantId = getUniqueSituationId();
        network.getVariantManager().cloneVariant(referenceVariantId, situationVariantId);
        crac.getExtension(ResultVariantManager.class).createVariant(situationVariantId);
        variantIds.add(situationVariantId);
        return situationVariantId;
    }

    public String getWorkingVariantId() {
        return workingVariantId;
    }

    Situation setWorkingVariant(String variantId) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format("Unknown situation variant %s", variantId));
        }
        workingVariantId = variantId;
        return this;
    }

    void deleteVariant(String variantId, boolean keepCracResult) {
        if (!variantIds.contains(variantId)) {
            throw new FaraoException(String.format("Unknown situation variant %s", variantId));
        }
        network.getVariantManager().removeVariant(variantId);
        if (!keepCracResult) {
            crac.getExtension(ResultVariantManager.class).deleteVariant(variantId);
        }
    }

    void clear(List<String> remainingVariantsForCrac) {
        for (String variantId: variantIds) {
            deleteVariant(variantId, remainingVariantsForCrac.contains(variantId));
        }
    }

    /**
     * Getters and setters
     */
    public Network getNetwork() {
        network.getVariantManager().setWorkingVariant(workingVariantId);
        return network;
    }

    public Crac getCrac() {
        return crac;
    }

    SystematicSensitivityAnalysisResult getSystematicSensitivityAnalysisResult() {
        return systematicSensitivityAnalysisResultMap.get(workingVariantId);
    }

    void setSystematicSensitivityAnalysisResultMap(SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        this.systematicSensitivityAnalysisResultMap.put(workingVariantId, systematicSensitivityAnalysisResult);
    }

    double getCost() {
        return crac.getExtension(CracResultExtension.class).getVariant(workingVariantId).getCost();
    }

    void setCost(double cost) {
        crac.getExtension(CracResultExtension.class).getVariant(workingVariantId).setCost(cost);
    }
}
