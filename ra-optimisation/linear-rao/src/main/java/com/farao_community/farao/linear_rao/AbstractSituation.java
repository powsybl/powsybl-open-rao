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
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;

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

    enum ComputationStatus {
        NOT_RUN,
        RUN_OK,
        RUN_NOK
    }

    /**
     * Computation status of the systematic sensitivity analysis
     */
    protected ComputationStatus sensiStatus;

    /**
     * Results of the systematic sensitivity analysis performed on the situation
     */
    protected SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult;

    /**
     * variant id in which some information about the situation are stored (including
     * the RangeActions' set-points)
     */
    protected String resultVariantId;

    /**
     * Crac object
     */
    protected Crac crac;

    /**
     * cost, value of the objective function for this situation
     */
    protected double cost;

    /**
     * constructor
     */
    AbstractSituation(Crac crac) {
        sensiStatus = ComputationStatus.NOT_RUN;
        this.crac = crac;
        this.cost = Double.NaN;

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }

        resultVariantId = resultVariantManager.createNewUniqueVariantId(this.getVariantPrefix());
    }

    SystematicSensitivityAnalysisResult getSystematicSensitivityAnalysisResult() {
        return systematicSensitivityAnalysisResult;
    }

    ComputationStatus getSensiStatus() {
        return sensiStatus;
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
     * evaluate the sensitivity coefficients and the objective function value of the
     * AbstractSituation. The results are written in the attributes
     * systematicSensitivityAnalysisResult, cost and in the Crac variant with id
     * resultVariantId.
     */
    void evaluateSensiAndCost(Network network, ComputationManager computationManager, SensitivityComputationParameters sensitivityComputationParameters) {

        systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
            .runAnalysis(network, crac, computationManager, sensitivityComputationParameters);

        // Failure if some sensitivities are not computed
        if (systematicSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || systematicSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {
            // delete()
            sensiStatus = ComputationStatus.RUN_NOK;
        } else {
            cost = -getMinMargin();
            sensiStatus = ComputationStatus.RUN_OK;
            addSystematicSensitivityAnalysisResultsToCracVariant(network);
        }
    }

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
    protected void addSystematicSensitivityAnalysisResultsToCracVariant(Network network) {
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
}
