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

    public MinMarginEvaluator(Unit unit, Set<String> operatorsNotToOptimize, boolean relativePositiveMargins) {
        this(unit, operatorsNotToOptimize, relativePositiveMargins, 0);
    }

    public MinMarginEvaluator(Unit unit, Set<String> operatorsNotToOptimize, boolean relativePositiveMargins, double ptdfSumLowerBound) {
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
    public double getCost(RaoData raoData) {
        if (unit.equals(MEGAWATT)) {
            return -getMinMarginInMegawatt(raoData);
        } else {
            return -getMinMarginInAmpere(raoData);
        }
    }

    private double getRelativeCoef(BranchCnec cnec, String initialVariantId) {
        return relativePositiveMargins ? 1 / Math.max(cnec.getExtension(CnecResultExtension.class).getVariant(initialVariantId).getAbsolutePtdfSum(), ptdfSumLowerBound) : 1;
    }

    private double getCnecMargin(RaoData raoData, BranchCnec cnec, Unit unit) {
        String initialVariantId = raoData.getCrac().getExtension(ResultVariantManager.class).getInitialVariantId();
        String prePerimeterVariantId = raoData.getCrac().getExtension(ResultVariantManager.class).getPrePerimeterVariantId();
        double newMargin = unit.equals(MEGAWATT) ?
                cnec.computeMargin(raoData.getSystematicSensitivityResult().getReferenceFlow(cnec), Side.LEFT, MEGAWATT) :
                cnec.computeMargin(raoData.getSystematicSensitivityResult().getReferenceIntensity(cnec), Side.LEFT, Unit.AMPERE);
        newMargin = (newMargin > 0) ? newMargin * getRelativeCoef(cnec, initialVariantId) : newMargin;
        if (operatorsNotToOptimize.contains(cnec.getOperator())) {
            // do not consider this kind of cnecs if they have a better margin than before optimization
            double prePerimeterMargin = RaoUtil.computeCnecMargin(cnec, prePerimeterVariantId, unit, relativePositiveMargins);
            if (newMargin > prePerimeterMargin - .0001 * Math.abs(prePerimeterMargin)) {
                return Double.MAX_VALUE;
            }
        }
        return newMargin;
    }

    private double getMinMarginInMegawatt(RaoData raoData) {
        if (raoData.getCnecs().stream().noneMatch(BranchCnec::isOptimized)) {
            // There are only pure MNECs
            return 0;
        }
        return raoData.getCnecs().stream().filter(BranchCnec::isOptimized).
            map(cnec -> getCnecMargin(raoData, cnec, MEGAWATT)).min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    private double getMinMarginInAmpere(RaoData raoData) {
        if (raoData.getCnecs().stream().noneMatch(BranchCnec::isOptimized)) {
            // There are only pure MNECs
            return 0;
        }
        List<Double> marginsInAmpere = raoData.getCnecs().stream().filter(BranchCnec::isOptimized).
            map(cnec -> getCnecMargin(raoData, cnec, AMPERE)).collect(Collectors.toList());

        if (marginsInAmpere.contains(Double.NaN) && raoData.getSystematicSensitivityResult().getStatus() == SystematicSensitivityResult.SensitivityComputationStatus.FALLBACK) {
            // in fallback, intensities can be missing as the fallback configuration does not necessarily
            // compute them (example : default in AC, fallback in DC). In that case a fallback computation
            // of the intensity is made, based on the MEGAWATT values and the nominal voltage
            LOGGER.warn("No intensities available in fallback mode, the margins are assessed by converting the flows from MW to A with the nominal voltage of each Cnec.");
            marginsInAmpere = getMarginsInAmpereFromMegawattConversion(raoData);
        }

        return marginsInAmpere.stream().min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    List<Double> getMarginsInAmpereFromMegawattConversion(RaoData raoData) {
        String initialVariantId = raoData.getCrac().getExtension(ResultVariantManager.class).getInitialVariantId();
        return raoData.getCnecs().stream().filter(BranchCnec::isOptimized).map(cnec -> {
                double leftFlowInMW = raoData.getSystematicSensitivityResult().getReferenceFlow(cnec);
                double leftNominalVoltage = cnec.getNominalVoltage(Side.LEFT);
                return cnec.computeMargin(leftFlowInMW * 1000 / (Math.sqrt(3) * leftNominalVoltage), Side.LEFT, Unit.AMPERE) * getRelativeCoef(cnec, initialVariantId);
            }
        ).collect(Collectors.toList());
    }
}
