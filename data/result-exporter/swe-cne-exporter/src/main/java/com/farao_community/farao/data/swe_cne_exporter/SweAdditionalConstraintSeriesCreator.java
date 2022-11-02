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
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.AngleCnecCreationContext;
import com.farao_community.farao.data.swe_cne_exporter.xsd.AdditionalConstraintSeries;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
        List<AngleCnecCreationContext> sortedAngleCnecs = cracCreationContext.getAngleCnecCreationContexts().stream()
                .filter(AngleCnecCreationContext::isImported)
                .sorted(Comparator.comparing(AngleCnecCreationContext::getNativeId))
                .collect(Collectors.toList());
        if (Objects.isNull(contingency)) {
            List<AngleCnecCreationContext> prevAngleCnecs = sortedAngleCnecs.stream().filter(angleCnecCreationContext -> Objects.isNull(angleCnecCreationContext.getContingencyId()))
                    .collect(Collectors.toList());
            prevAngleCnecs.forEach(angleCnecCreationContext -> {
                AdditionalConstraintSeries additionalConstraintSeries = generateAdditionalConstraintSeries(angleCnecCreationContext);
                additionalConstraintSeriesList.add(additionalConstraintSeries);
            });
        } else {
            List<AngleCnecCreationContext> contingencyAngleCnecs = sortedAngleCnecs.stream()
                    .filter(angleCnecCreationContext -> angleCnecCreationContext.getContingencyId().equals(contingency.getId()))
                    .collect(Collectors.toList());
            contingencyAngleCnecs.forEach(angleCnecCreationContext -> {
                AdditionalConstraintSeries additionalConstraintSeries = generateAdditionalConstraintSeries(angleCnecCreationContext);
                additionalConstraintSeriesList.add(additionalConstraintSeries);
            });
        }
        return additionalConstraintSeriesList;
    }

    private AdditionalConstraintSeries generateAdditionalConstraintSeries(AngleCnecCreationContext angleCnecCreationContext) {
        Crac crac = sweCneHelper.getCrac();
        AngleCnec angleCnec = crac.getAngleCnec(angleCnecCreationContext.getNativeId());
        AdditionalConstraintSeries additionalConstraintSeries = new AdditionalConstraintSeries();
        additionalConstraintSeries.setMRID(angleCnecCreationContext.getNativeId());
        additionalConstraintSeries.setBusinessType("B87");
        additionalConstraintSeries.setName(angleCnec.getName());
        additionalConstraintSeries.setQuantityQuantity(BigDecimal.valueOf(sweCneHelper.getAngleMonitoringResult().getAngle(angleCnec, Unit.DEGREE)));
        return additionalConstraintSeries;
    }
}
