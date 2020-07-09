/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CostEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MnecViolationCostEvaluator.class);

    private Unit unit;
    private double mnecAcceptableMarginDiminution;
    private double mnecViolationCost;

    public MnecViolationCostEvaluator(Unit unit, double mnecAcceptableMarginDiminution, double mnecViolationCost) {
        if ((unit != MEGAWATT) && (unit != AMPERE)) {
            throw new NotImplementedException("MNEC violation cost is only implemented in MW and AMPERE units");
        }
        this.unit = unit;
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
        this.mnecViolationCost = mnecViolationCost;
    }

    @Override
    public double getCost(RaoData raoData) {
        if (Math.abs(mnecViolationCost) < 1e-10) {
            return 0;
        }
        double totalMnecMarginViolation = 0;
        boolean mnecsSkipped = false;
        String initialVariantId =  raoData.getCrac().getExtension(ResultVariantManager.class).getPreOptimVariantId();
        for (Cnec cnec : raoData.getCrac().getCnecs()) {
            if (cnec.isMonitored()) {
                double initialFlow = (unit == MEGAWATT) ? cnec.getExtension(CnecResultExtension.class).getVariant(initialVariantId).getFlowInMW()
                        : cnec.getExtension(CnecResultExtension.class).getVariant(initialVariantId).getFlowInA();
                if (Double.isNaN(initialFlow)) {
                    // Sensitivity results are not available, skip cnec
                    // (happens on search tree rao rootleaf evaluation)
                    mnecsSkipped = true;
                    continue;
                }
                double initialMargin = cnec.computeMargin(initialFlow, unit);
                double newFlow = (unit == MEGAWATT) ? raoData.getSystematicSensitivityAnalysisResult().getReferenceFlow(cnec) :
                        raoData.getSystematicSensitivityAnalysisResult().getReferenceIntensity(cnec);
                double newMargin = cnec.computeMargin(newFlow, unit);
                double convertedAcceptableMarginDiminution = mnecAcceptableMarginDiminution / getUnitConversionCoefficient(cnec, raoData);
                totalMnecMarginViolation += Math.max(0, Math.min(0, initialMargin - convertedAcceptableMarginDiminution) - newMargin);
            }
        }
        if (mnecsSkipped) {
            LOGGER.warn("Some MNECs were skipped during violation cost evaluation because their sensitivity results were not available.");
        }
        return mnecViolationCost * totalMnecMarginViolation;
    }

    @Override
    public Unit getUnit() {
        return this.unit;
    }

    /**
     * Get unit conversion coefficient between A and MW
     * The acceptable margin diminution parameter is defined in MW, so if the minimum margin is defined in ampere,
     * appropriate conversion coefficient should be used.
     */
    private double getUnitConversionCoefficient(Cnec cnec, RaoData linearRaoData) {
        if (unit.equals(MEGAWATT)) {
            return 1;
        } else {
            // Unom(cnec) * sqrt(3) / 1000
            return linearRaoData.getNetwork().getBranch(cnec.getNetworkElement().getId()).getTerminal1().getVoltageLevel().getNominalV() * Math.sqrt(3) / 1000;
        }
    }
}
