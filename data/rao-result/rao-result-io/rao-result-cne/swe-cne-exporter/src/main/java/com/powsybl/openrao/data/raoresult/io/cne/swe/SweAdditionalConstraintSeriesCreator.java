/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.swe;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.crac.io.cim.craccreator.AngleCnecCreationContext;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.AdditionalConstraintSeries;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneUtil.DEFAULT_DECIMALS_FOR_ROUNDING_ANGLES;

/**
 * Generates AdditionalConstraintSeries for SWE CNE format
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SweAdditionalConstraintSeriesCreator {
    public static final String ANGLE_CNEC_BUSINESS_TYPE = "B87";
    private final SweCneHelper sweCneHelper;
    private final CimCracCreationContext cracCreationContext;

    public SweAdditionalConstraintSeriesCreator(SweCneHelper sweCneHelper, CimCracCreationContext cracCreationContext) {
        this.sweCneHelper = sweCneHelper;
        this.cracCreationContext = cracCreationContext;
    }

    public List<AdditionalConstraintSeries> generateAdditionalConstraintSeries(Contingency contingency) {
        List<AngleCnecCreationContext> sortedAngleCnecs = cracCreationContext.getAngleCnecCreationContexts().stream()
            .filter(AngleCnecCreationContext::isImported)
            .sorted(Comparator.comparing(AngleCnecCreationContext::getCreatedObjectId))
            .toList();
        if (contingency == null) {
            sortedAngleCnecs.stream().filter(angleCnecCreationContext -> Objects.isNull(angleCnecCreationContext.getContingencyId()))
                .forEach(angleCnecCreationContext ->
                    BUSINESS_WARNS.warn("Preventive angle cnec {} will not be added to CNE file", angleCnecCreationContext.getNativeObjectId()));
            return Collections.emptyList();
        } else {
            return sortedAngleCnecs.stream()
                .filter(angleCnecCreationContext -> angleCnecCreationContext.getContingencyId().equals(contingency.getId()))
                .map(this::generateAdditionalConstraintSeries)
                .filter(Objects::nonNull)
                .toList();
        }
    }

    private AdditionalConstraintSeries generateAdditionalConstraintSeries(AngleCnecCreationContext angleCnecCreationContext) {
        Crac crac = sweCneHelper.getCrac();
        AngleCnec angleCnec = crac.getAngleCnec(angleCnecCreationContext.getCreatedObjectId());
        if (!angleCnec.getState().getInstant().isCurative()) {
            BUSINESS_WARNS.warn("{} angle cnec {} will not be added to CNE file", angleCnec.getState().getInstant(), angleCnecCreationContext.getNativeObjectId());
            return null;
        }
        RaoResult raoResult = sweCneHelper.getRaoResult();
        // only export if angle check ran
        if (!raoResult.getComputationStatus().equals(ComputationStatus.FAILURE) && !Double.isNaN(raoResult.getAngle(crac.getInstant(InstantKind.CURATIVE), angleCnec, Unit.DEGREE))) {
            AdditionalConstraintSeries additionalConstraintSeries = new AdditionalConstraintSeries();
            additionalConstraintSeries.setMRID(angleCnecCreationContext.getCreatedObjectId());
            additionalConstraintSeries.setBusinessType(ANGLE_CNEC_BUSINESS_TYPE);
            additionalConstraintSeries.setName(angleCnec.getName());
            additionalConstraintSeries.setQuantityQuantity(roundAngleValue(angleCnec, crac, raoResult));
            return additionalConstraintSeries;
        }
        return null;
    }

    /**
     * Round the angle value of an AngleCNEC with a relevant number decimals.
     * Generally, only one decimal suffices but in case of very small violations,
     * the number of decimals must be increased so the violation can be
     * read directly in the results.
     */
    static BigDecimal roundAngleValue(AngleCnec angleCnec, Crac crac, RaoResult raoResult) {
        double angle = raoResult.getAngle(crac.getInstant(InstantKind.CURATIVE), angleCnec, Unit.DEGREE);
        double marginOnLowerBound = angle - angleCnec.getLowerBound(Unit.DEGREE).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = angleCnec.getUpperBound(Unit.DEGREE).orElse(Double.POSITIVE_INFINITY) - angle;
        double margin = Math.min(marginOnLowerBound, marginOnUpperBound);
        return roundValueBasedOnMargin(angle, margin, DEFAULT_DECIMALS_FOR_ROUNDING_ANGLES);
    }
}
