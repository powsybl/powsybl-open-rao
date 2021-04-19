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
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerParameters;
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

    Set<BranchCnec> cnecs;
    Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW;
    Map<BranchCnec, Double> initialAbsolutePtdfSums;
    private final Unit unit;
    private final boolean relativePositiveMargins;
    private double ptdfSumLowerBound;
    private Set<String> operatorsNotToOptimize = Collections.emptySet();

    public MinMarginEvaluator(Set<BranchCnec> cnecs, Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW, Map<BranchCnec, Double> initialAbsolutePtdfSums, LinearOptimizerParameters linearOptimizerParameters) {
        this.cnecs = cnecs;
        this.prePerimeterMarginsInAbsoluteMW = prePerimeterMarginsInAbsoluteMW;
        this.initialAbsolutePtdfSums = initialAbsolutePtdfSums;
        this.unit = linearOptimizerParameters.getUnit();
        this.relativePositiveMargins = linearOptimizerParameters.hasRelativeMargins();
        if (relativePositiveMargins) {
            this.ptdfSumLowerBound = linearOptimizerParameters.getMaxMinRelativeMarginParameters().getPtdfSumLowerBound();
        }
        if (linearOptimizerParameters.hasOperatorsNotToOptimize()) {
            this.operatorsNotToOptimize = linearOptimizerParameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize();
        }
        if (relativePositiveMargins && ptdfSumLowerBound <= 0) {
            throw new FaraoException("Please provide a (strictly positive) PTDF sum lower bound for relative margins.");
        }
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public List<BranchCnec> getMostLimitingElements(SensitivityAndLoopflowResults sensitivityAndLoopflowResults, int numberOfElements) {
        List<BranchCnec> mostLimitingElements = getMargins(sensitivityAndLoopflowResults.getSystematicSensitivityResult(), unit)
                .entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return mostLimitingElements.subList(0, Math.min(numberOfElements, mostLimitingElements.size()));
    }

    @Override
    public double computeCost(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        if (unit.equals(MEGAWATT)) {
            return -getMinMarginInMegawatt(sensitivityAndLoopflowResults.getSystematicSensitivityResult());
        } else {
            return -getMinMarginInAmpere(sensitivityAndLoopflowResults.getSystematicSensitivityResult());
        }
    }

    private double getRelativeCoef(BranchCnec cnec) {
        return relativePositiveMargins ? 1 / Math.max(initialAbsolutePtdfSums.get(cnec), ptdfSumLowerBound) : 1;
    }

    private double getCnecMargin(SystematicSensitivityResult sensitivityResult, BranchCnec cnec, Unit unit) {
        if (operatorsNotToOptimize.contains(cnec.getOperator())) {
            // do not consider this kind of cnecs if they have a better margin than before optimization
            double prePerimeterMarginInAbsoluteMW = prePerimeterMarginsInAbsoluteMW.get(cnec);
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
        if (cnecs.stream().noneMatch(BranchCnec::isOptimized)) {
            // There are only pure MNECs
            return 0;
        }
        return getMargins(sensitivityResult, MEGAWATT).values().stream().min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    private double getMinMarginInAmpere(SystematicSensitivityResult sensitivityResult) {
        if (cnecs.stream().noneMatch(BranchCnec::isOptimized)) {
            // There are only pure MNECs
            return 0;
        }
        Map<BranchCnec, Double> marginsInAmpere = getMargins(sensitivityResult, AMPERE);

        if (marginsInAmpere.containsValue(Double.NaN) && sensitivityResult.getStatus() == SystematicSensitivityResult.SensitivityComputationStatus.FALLBACK) {
            // in fallback, intensities can be missing as the fallback configuration does not necessarily
            // compute them (example : default in AC, fallback in DC). In that case a fallback computation
            // of the intensity is made, based on the MEGAWATT values and the nominal voltage
            LOGGER.warn("No intensities available in fallback mode, the margins are assessed by converting the flows from MW to A with the nominal voltage of each Cnec.");
            marginsInAmpere = getMarginsInAmpereFromMegawattConversion(sensitivityResult);
        }

        return marginsInAmpere.values().stream().min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    private Map<BranchCnec, Double> getMargins(SystematicSensitivityResult sensitivityResult, Unit unit) {
        Map<BranchCnec, Double> marginsInAmpere = new HashMap<>();
        cnecs.stream().filter(BranchCnec::isOptimized).
                forEach(cnec -> marginsInAmpere.put(cnec, getCnecMargin(sensitivityResult, cnec, unit)));
        return marginsInAmpere;
    }

    Map<BranchCnec, Double> getMarginsInAmpereFromMegawattConversion(SystematicSensitivityResult sensitivityResult) {
        Map<BranchCnec, Double> margins = new HashMap<>();
        cnecs.stream().filter(BranchCnec::isOptimized).forEach(cnec -> {
                double leftFlowInMW = sensitivityResult.getReferenceFlow(cnec);
                margins.put(cnec, cnec.computeMargin(leftFlowInMW * RaoUtil.getBranchFlowUnitMultiplier(cnec, Side.LEFT, MEGAWATT, AMPERE), Side.LEFT, AMPERE) * getRelativeCoef(cnec));
            }
        );
        return margins;
    }
}
