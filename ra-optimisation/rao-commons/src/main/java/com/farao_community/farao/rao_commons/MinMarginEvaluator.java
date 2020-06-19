/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * It enables to evaluate the absolute minimal margin as a cost
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinMarginEvaluator implements CostEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinMarginEvaluator.class);

    private Unit unit;

    public MinMarginEvaluator(Unit unit) {
        this.unit = unit;
    }

    @Override
    public double getCost(RaoData raoData) {
        if (unit.equals(MEGAWATT)) {
            return getMinMarginInMegawatt(raoData);
        } else {
            return getMinMarginInAmpere(raoData);
        }
    }

    private double getMinMarginInMegawatt(RaoData raoData) {
        return raoData.getCrac().getCnecs().stream().
            map(cnec -> cnec.computeMargin(raoData.getSystematicSensitivityAnalysisResult().getReferenceFlow(cnec), MEGAWATT)).
            min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    private double getMinMarginInAmpere(RaoData raoData) {
        List<Double> marginsInAmpere = raoData.getCrac().getCnecs().stream().map(cnec ->
            cnec.computeMargin(raoData.getSystematicSensitivityAnalysisResult().getReferenceIntensity(cnec), Unit.AMPERE)
        ).collect(Collectors.toList());

        if (marginsInAmpere.contains(Double.NaN)) { // It means that computation has been performed in DC mode
            // in fallback, intensities can be missing as the fallback configuration does not necessarily
            // compute them (example : default in AC, fallback in DC). In that case a fallback computation
            // of the intensity is made, based on the MEGAWATT values and the nominal voltage
            LOGGER.warn("No intensities available in fallback mode, the margins are assessed by converting the flows from MW to A with the nominal voltage of each Cnec.");
            marginsInAmpere = getMarginsInAmpereFromMegawattConversion(raoData);
        }

        return marginsInAmpere.stream().min(Double::compareTo).orElseThrow(NoSuchElementException::new);
    }

    private List<Double> getMarginsInAmpereFromMegawattConversion(RaoData raoData) {
        return raoData.getCrac().getCnecs().stream().map(cnec -> {
                double flowInMW = raoData.getSystematicSensitivityAnalysisResult().getReferenceFlow(cnec);
                double uNom = raoData.getNetwork().getBranch(cnec.getNetworkElement().getId()).getTerminal1().getVoltageLevel().getNominalV();
                return cnec.computeMargin(flowInMW * 1000 / (Math.sqrt(3) * uNom), Unit.AMPERE);
            }
        ).collect(Collectors.toList());
    }
}
