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

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CostEvaluator {

    private Unit unit;
    private double mnecAcceptableMarginDiminution;
    private double mnecViolationCostPerMW;

    public MnecViolationCostEvaluator(Unit unit, double mnecAcceptableMarginDiminution, double mnecViolationCostPerMW) {
        if (unit != MEGAWATT) {
            throw new NotImplementedException("MNEC violation cost is only implemented in MW unit");
        }
        this.unit = unit;
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
        this.mnecViolationCostPerMW = mnecViolationCostPerMW;
    }

    @Override
    public double getCost(RaoData raoData) {
        return getViolationCostInMegawatt(raoData);
    }

    private double getViolationCostInMegawatt(RaoData raoData) {
        if (Math.abs(mnecViolationCostPerMW) < 1e-10) {
            return 0;
        }
        double totalMnecMarginViolation = 0;
        String initialVariantId =  raoData.getCrac().getExtension(ResultVariantManager.class).getPreOptimVariantId();
        for (Cnec cnec : raoData.getCrac().getCnecs()) {
            if (cnec.isMonitored()) {
                double initialFlow = cnec.getExtension(CnecResultExtension.class).getVariant(initialVariantId).getFlowInMW();
                double initialMargin = cnec.computeMargin(initialFlow, Unit.MEGAWATT);
                double newMargin = cnec.computeMargin(raoData.getSystematicSensitivityAnalysisResult().getReferenceFlow(cnec), Unit.MEGAWATT);
                totalMnecMarginViolation += Math.max(0, Math.min(0, initialMargin - mnecAcceptableMarginDiminution) - newMargin);
            }
        }
        return mnecViolationCostPerMW * totalMnecMarginViolation;
    }

    @Override
    public Unit getUnit() {
        return this.unit;
    }
}
