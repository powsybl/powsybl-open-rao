/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_commons.linear_optimisation.core.LinearProblemParameters;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.farao_community.farao.rao_commons.RaoData.NO_WORKING_VARIANT;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaoDataManager.class);

    private RaoData raoData;

    RaoDataManager(RaoData raoData) {
        this.raoData = raoData;
    }

    /**
     * This method works from the working variant. It is filling CRAC result extension of the working variant
     * with values in network of the working variant.
     */
    public void fillRangeActionResultsWithNetworkValues() {
        if (raoData.getWorkingVariantId() == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        for (RangeAction rangeAction : raoData.getCrac().getRangeActions()) {
            double valueInNetwork = rangeAction.getCurrentValue(raoData.getNetwork());
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(raoData.getWorkingVariantId());
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
        if (raoData.getWorkingVariantId() == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        for (RangeAction rangeAction : raoData.getCrac().getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            rangeAction.apply(raoData.getNetwork(), rangeActionResultMap.getVariant(raoData.getWorkingVariantId()).getSetPoint(preventiveState));
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
        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        for (RangeAction rangeAction : raoData.getCrac().getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            double value1 = rangeActionResultMap.getVariant(variantId1).getSetPoint(preventiveState);
            double value2 = rangeActionResultMap.getVariant(variantId2).getSetPoint(preventiveState);
            if (value1 != value2 && (!Double.isNaN(value1) || !Double.isNaN(value2))) {
                return false;
            }
        }
        return true;
    }

    /**
     * add results of the systematic analysis (flows and objective function value) in the
     * Crac result variant of the situation.
     */
    public void fillCracResultsWithSensis(LinearProblemParameters.ObjectiveFunction objectiveFunction, SystematicSensitivityComputation systematicSensitivityComputation) {
        double minMargin;
        minMargin = getMinMargin(objectiveFunction);
        raoData.getCracResult().setFunctionalCost(-minMargin);
        raoData.getCracResult().setVirtualCost(systematicSensitivityComputation.isFallback() ?
            systematicSensitivityComputation.getParameters().getFallbackOvercost() : 0);
        raoData.getCracResult().setNetworkSecurityStatus(minMargin < 0 ?
            CracResult.NetworkSecurityStatus.UNSECURED : CracResult.NetworkSecurityStatus.SECURED);
        updateCnecExtensions();
    }

    /**
     * Compute the objective function, the minimal margin.
     */
    private double getMinMargin(LinearProblemParameters.ObjectiveFunction objectiveFunction) {
        if (objectiveFunction == LinearProblemParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT) {
            return getMinMarginInMegawatt();
        } else {
            return getMinMarginInAmpere();
        }
    }

    private double getMinMarginInMegawatt() {
        return raoData.getCrac().getCnecs().stream().
            map(cnec -> cnec.computeMargin(raoData.getSystematicSensitivityAnalysisResult().getReferenceFlow(cnec), Unit.MEGAWATT)).
            min(Double::compareTo).orElseThrow(NoSuchElementException::new);

    }

    private double getMinMarginInAmpere() {
        List<Double> marginsInAmpere = raoData.getCrac().getCnecs().stream().map(cnec ->
            cnec.computeMargin(raoData.getSystematicSensitivityAnalysisResult().getReferenceIntensity(cnec), Unit.AMPERE)
        ).collect(Collectors.toList());

        if (marginsInAmpere.contains(Double.NaN)) {
            LOGGER.warn("No intensities available in fallback mode, the margins are assessed by converting the flows from MW to A with the nominal voltage of each Cnec.");
            marginsInAmpere = getMarginsInAmpereFromMegawattConversion();
            /*if (!fallbackMode) {
                // in default mode, this means that there is an error in the sensitivity computation, or an
                // incompatibility with the sensitivity computation mode (i.e. the sensitivity computation is
                // made in DC mode and no intensity are computed).
                throw new SensitivityComputationException("Intensity values are missing from the output of the sensitivity analysis. Min margin cannot be calculated in AMPERE.");
            } else {

                // in fallback, intensities can be missing as the fallback configuration does not necessarily
                // compute them (example : default in AC, fallback in DC). In that case a fallback computation
                // of the intensity is made, based on the MEGAWATT values and the nominal voltage

            }*/
        }

        return marginsInAmpere.stream().min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    private List<Double> getMarginsInAmpereFromMegawattConversion() {
        return raoData.getCrac().getCnecs().stream().map(cnec -> {
                double flowInMW = raoData.getSystematicSensitivityAnalysisResult().getReferenceFlow(cnec);
                double uNom = raoData.getNetwork().getBranch(cnec.getNetworkElement().getId()).getTerminal1().getVoltageLevel().getNominalV();
                return cnec.computeMargin(flowInMW * 1000 / (Math.sqrt(3) * uNom), Unit.AMPERE);
            }
        ).collect(Collectors.toList());
    }

    public void updateCnecExtensions() {
        if (raoData.getWorkingVariantId() == null) {
            throw new FaraoException(NO_WORKING_VARIANT);
        }
        raoData.getCrac().getCnecs().forEach(cnec -> {
            CnecResult cnecResult = cnec.getExtension(CnecResultExtension.class).getVariant(raoData.getWorkingVariantId());
            cnecResult.setFlowInMW(raoData.getSystematicSensitivityAnalysisResult().getReferenceFlow(cnec));
            cnecResult.setFlowInA(raoData.getSystematicSensitivityAnalysisResult().getReferenceIntensity(cnec));
            cnecResult.setThresholds(cnec);
        });
    }
}
