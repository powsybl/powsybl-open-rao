/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * It enables to evaluate the absolute or relative minimal margin as a cost
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MinMarginEvaluator implements CostEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinMarginEvaluator.class);

    private Unit unit;
    private boolean relativePositiveMargins;
    private double ptdfSumLowerBound;
    Set<String> operatorsNotToOptimize;
    LinearOptimizerInput linearOptimizerInput;

    public MinMarginEvaluator(LinearOptimizerInput linearOptimizerInput, Unit unit, Set<String> operatorsNotToOptimize, boolean relativePositiveMargins) {
        this(linearOptimizerInput, unit, operatorsNotToOptimize, relativePositiveMargins, 0);
    }

    public MinMarginEvaluator(LinearOptimizerInput linearOptimizerInput, Unit unit, Set<String> operatorsNotToOptimize, boolean relativePositiveMargins, double ptdfSumLowerBound) {
        if (relativePositiveMargins && ptdfSumLowerBound <= 0) {
            throw new FaraoException("Please provide a (strictly positive) PTDF sum lower bound for relative margins.");
        }
        this.unit = unit;
        this.relativePositiveMargins = relativePositiveMargins;
        this.ptdfSumLowerBound = ptdfSumLowerBound;
        if (!Objects.isNull(operatorsNotToOptimize)) {
            this.operatorsNotToOptimize = operatorsNotToOptimize;
        } else {
            this.operatorsNotToOptimize = new HashSet<>();
        }
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public double getCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        if (unit.equals(MEGAWATT)) {
            return -getMinMarginInMegawatt(sensitivityAndLoopflowResults.getSystematicSensitivityResult());
        } else {
            return -getMinMarginInAmpere(sensitivityAndLoopflowResults.getSystematicSensitivityResult());
        }
    }

    private double getRelativeCoef(BranchCnec cnec) {
        return relativePositiveMargins ? 1 / Math.max(linearOptimizerInput.getInitialAbsolutePtdfSum(cnec), ptdfSumLowerBound) : 1;
    }

    private double getCnecMargin(SystematicSensitivityResult sensitivityResult, BranchCnec cnec, Unit unit) {
        if (operatorsNotToOptimize.contains(cnec.getOperator())) {
            // do not consider this kind of cnecs if they have a better margin than before optimization
            double prePerimeterMarginInAbsoluteMW = linearOptimizerInput.getPrePerimeterMarginsInAbsoluteMW(cnec);
            double newMarginInAbsoluteMW = cnec.computeMargin(sensitivityResult.getReferenceFlow(cnec), Side.LEFT, MEGAWATT);
            if (newMarginInAbsoluteMW > prePerimeterMarginInAbsoluteMW - .0001 * Math.abs(prePerimeterMarginInAbsoluteMW)) {
                return Double.MAX_VALUE;
            }
        }
        double newMargin = unit.equals(MEGAWATT) ?
                cnec.computeMargin(sensitivityResult.getReferenceFlow(cnec), Side.LEFT, MEGAWATT) :
                cnec.computeMargin(sensitivityResult.getReferenceIntensity(cnec), Side.LEFT, Unit.AMPERE);
        newMargin = (newMargin > 0) ? newMargin * getRelativeCoef(cnec) : newMargin;
        return newMargin;
    }

    private double getMinMarginInMegawatt(SystematicSensitivityResult sensitivityResult) {
        if (linearOptimizerInput.getCnecs().stream().noneMatch(BranchCnec::isOptimized)) {
            // There are only pure MNECs
            return 0;
        }
        return linearOptimizerInput.getCnecs().stream().filter(BranchCnec::isOptimized).
            map(cnec -> getCnecMargin(sensitivityResult, cnec, MEGAWATT)).min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    private double getMinMarginInAmpere(SystematicSensitivityResult sensitivityResult) {
        if (linearOptimizerInput.getCnecs().stream().noneMatch(BranchCnec::isOptimized)) {
            // There are only pure MNECs
            return 0;
        }
        List<Double> marginsInAmpere = linearOptimizerInput.getCnecs().stream().filter(BranchCnec::isOptimized).
            map(cnec -> getCnecMargin(sensitivityResult, cnec, AMPERE)).collect(Collectors.toList());

        if (marginsInAmpere.contains(Double.NaN) && sensitivityResult.getStatus() == SystematicSensitivityResult.SensitivityComputationStatus.FALLBACK) {
            // in fallback, intensities can be missing as the fallback configuration does not necessarily
            // compute them (example : default in AC, fallback in DC). In that case a fallback computation
            // of the intensity is made, based on the MEGAWATT values and the nominal voltage
            LOGGER.warn("No intensities available in fallback mode, the margins are assessed by converting the flows from MW to A with the nominal voltage of each Cnec.");
            marginsInAmpere = getMarginsInAmpereFromMegawattConversion(sensitivityResult);
        }

        return marginsInAmpere.stream().min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    List<Double> getMarginsInAmpereFromMegawattConversion(SystematicSensitivityResult sensitivityResult) {
        return linearOptimizerInput.getCnecs().stream().filter(BranchCnec::isOptimized).map(cnec -> {
                double leftFlowInMW = sensitivityResult.getReferenceFlow(cnec);
                return cnec.computeMargin(leftFlowInMW * RaoUtil.getBranchFlowUnitMultiplier(cnec, Side.LEFT, MEGAWATT, AMPERE), Side.LEFT, AMPERE) * getRelativeCoef(cnec);
            }
        ).collect(Collectors.toList());
    }
}
