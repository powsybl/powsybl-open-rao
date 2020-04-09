/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;

/**
 * An AbstractSituation includes a set of information associated to a given
 * network situation (i.e. a given combination of RangeActions set-points).
 * An AbstractSituation also embeds some methods enabling to do some
 * computation on this network situation. The computation common to all
 * AbstractSituation is a systematic sensitivity analysis.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
abstract class AbstractSituation {

    /**
     * Results of the systematic sensitivity analysis performed on the situation
     */
    private SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult;

    /**
     * variant id in which some information about the situation are stored (including
     * the RangeActions' set-points)
     */
    private String resultVariantId;

    /**
     * Crac object
     */
    private Crac crac;

    /**
     * Network object
     */
    private Network network;

    /**
     * Network variant id
     */
    private String networkVariant;

    /**
     * cost, value of the objective function for this situation
     */
    private double cost;

    /**
     * constructor
     */
    AbstractSituation(Network network, Crac crac) {
        this.crac = crac;
        this.network = network;
        this.cost = Double.NaN;

        this.networkVariant = createAndSwitchToNewVariant(network, network.getVariantManager().getWorkingVariantId());

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }
        resultVariantId = resultVariantManager.createNewUniqueVariantId(this.getVariantPrefix());
    }

    Crac getCrac() {
        return crac;
    }

    Network getNetwork() {
        return network;
    }

    SystematicSensitivityAnalysisResult getSystematicSensitivityAnalysisResult() {
        return systematicSensitivityAnalysisResult;
    }

    String getResultVariant() {
        return resultVariantId;
    }

    double getCost() {
        return cost;
    }

    /**
     * get the variant prefix used in the Crac ResultVariantManager
     */
    protected abstract String getVariantPrefix();

    /**
     * Compare the network situations (i.e. the RangeActions set-points) of two
     * AbstractSituation. Returns true if the situations are identical, and false
     * if they are not.
     */
    boolean sameRaResults(AbstractSituation otherLinearRaoSituation) {
        String otherResultVariantId = otherLinearRaoSituation.getResultVariant();
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            double value1 = rangeActionResultMap.getVariant(resultVariantId).getSetPoint(preventiveState);
            double value2 = rangeActionResultMap.getVariant(otherResultVariantId).getSetPoint(preventiveState);
            if (value1 != value2 && (!Double.isNaN(value1) || !Double.isNaN(value2))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Delete the Crac result variant associated to this situation
     */
    void deleteResultVariant() {
        crac.getExtension(ResultVariantManager.class).deleteVariant(resultVariantId);
    }

    /**
     * Compute the objective function, the minimal margin.
     */
    private double getMinMargin() {
        double minMargin = Double.POSITIVE_INFINITY;
        for (Cnec cnec : crac.getCnecs()) {
            double flow = systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN);
            double margin = cnec.computeMargin(flow, Unit.MEGAWATT);
            if (Double.isNaN(margin)) {
                throw new FaraoException(format("Cnec %s is not present in the linear RAO result. Bad behaviour.", cnec.getId()));
            }
            minMargin = Math.min(minMargin, margin);
        }
        return minMargin;
    }


    /**
     * add results of the systematic analysis (flows and objective function value) in the
     * Crac result variant associated to this situation.
     */
    void setResults(SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        this.systematicSensitivityAnalysisResult = systematicSensitivityAnalysisResult;
        cost = -getMinMargin();
        updateCracExtension();
        updateCnecExtensions();
    }

    private void updateCracExtension() {
        CracResultExtension cracResultMap = crac.getExtension(CracResultExtension.class);
        CracResult cracResult = cracResultMap.getVariant(resultVariantId);
        cracResult.setCost(cost);
    }

    private void updateCnecExtensions() {
        crac.getCnecs().forEach(cnec -> {
            CnecResultExtension cnecResultMap = cnec.getExtension(CnecResultExtension.class);
            CnecResult cnecResult = cnecResultMap.getVariant(resultVariantId);
            cnecResult.setFlowInMW(systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setFlowInA(systematicSensitivityAnalysisResult.getCnecIntensityMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setThresholds(cnec);
        });
    }

    public void switchToNetworkVariant() {
        network.getVariantManager().setWorkingVariant(networkVariant);
    }

    private String getUniqueVariantId(Network network) {
        String uniqueId;
        do {
            uniqueId = UUID.randomUUID().toString();
        } while (network.getVariantManager().getVariantIds().contains(uniqueId));
        return uniqueId;
    }

    private String createAndSwitchToNewVariant(Network network, String referenceNetworkVariant) {
        Objects.requireNonNull(referenceNetworkVariant);
        if (!network.getVariantManager().getVariantIds().contains(referenceNetworkVariant)) {
            throw new FaraoException(String.format("Unknown network variant %s", referenceNetworkVariant));
        }
        String uniqueId = getUniqueVariantId(network);
        network.getVariantManager().cloneVariant(referenceNetworkVariant, uniqueId);
        network.getVariantManager().setWorkingVariant(uniqueId);
        return uniqueId;
    }
}
