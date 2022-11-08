/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.AngleCnecCreationContext;
import com.farao_community.farao.data.swe_cne_exporter.xsd.AdditionalConstraintSeries;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

/**
 * Generates AdditionalConstraintSeries for SWE CNE format
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class SweAdditionalConstraintSeriesCreator {
    private final SweCneHelper sweCneHelper;
    private final CimCracCreationContext cracCreationContext;

    public SweAdditionalConstraintSeriesCreator(SweCneHelper sweCneHelper, CimCracCreationContext cracCreationContext) {
        this.sweCneHelper = sweCneHelper;
        this.cracCreationContext = cracCreationContext;
    }

    public List<AdditionalConstraintSeries> generateAdditionalConstraintSeries(Contingency contingency) {
        List<AdditionalConstraintSeries> additionalConstraintSeriesList = new ArrayList<>();
        if (Objects.isNull(sweCneHelper.getAngleMonitoringResult())) {
            return additionalConstraintSeriesList;
        }
        List<AngleCnecCreationContext> sortedAngleCnecs = cracCreationContext.getAngleCnecCreationContexts().stream()
                .filter(AngleCnecCreationContext::isImported)
                .sorted(Comparator.comparing(AngleCnecCreationContext::getNativeId))
                .collect(Collectors.toList());
        if (Objects.isNull(contingency)) {
            List<AngleCnecCreationContext> prevAngleCnecs = sortedAngleCnecs.stream().filter(angleCnecCreationContext -> Objects.isNull(angleCnecCreationContext.getContingencyId()))
                    .collect(Collectors.toList());
            prevAngleCnecs.forEach(angleCnecCreationContext ->
                BUSINESS_WARNS.warn("Preventive angle cnec {} will not be added to CNE file", angleCnecCreationContext.getNativeId()));
        } else {
            List<AngleCnecCreationContext> contingencyAngleCnecs = sortedAngleCnecs.stream()
                    .filter(angleCnecCreationContext -> angleCnecCreationContext.getContingencyId().equals(contingency.getId()))
                    .collect(Collectors.toList());
            contingencyAngleCnecs.forEach(angleCnecCreationContext -> {
                AdditionalConstraintSeries additionalConstraintSeries = generateAdditionalConstraintSeries(angleCnecCreationContext);
                if (Objects.nonNull(additionalConstraintSeries)) {
                    additionalConstraintSeriesList.add(additionalConstraintSeries);
                }
            });
        }
        return additionalConstraintSeriesList;
    }

    private AdditionalConstraintSeries generateAdditionalConstraintSeries(AngleCnecCreationContext angleCnecCreationContext) {
        Crac crac = sweCneHelper.getCrac();
        AngleCnec angleCnec = crac.getAngleCnec(angleCnecCreationContext.getNativeId());
        if (!angleCnec.getState().getInstant().equals(Instant.CURATIVE)) {
            BUSINESS_WARNS.warn("{} angle cnec {} will not be added to CNE file", angleCnec.getState().getInstant(), angleCnecCreationContext.getNativeId());
            return null;
        }
        AdditionalConstraintSeries additionalConstraintSeries = new AdditionalConstraintSeries();
        additionalConstraintSeries.setMRID(angleCnecCreationContext.getNativeId());
        additionalConstraintSeries.setBusinessType("B87");
        additionalConstraintSeries.setName(angleCnec.getName());
        if (!sweCneHelper.getAngleMonitoringResult().isDivergent()) {
            additionalConstraintSeries.setQuantityQuantity(BigDecimal.valueOf(sweCneHelper.getAngleMonitoringResult().getAngle(angleCnec, Unit.DEGREE)));
        }
        return additionalConstraintSeries;
    }
}
