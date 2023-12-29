/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.swe_cne_exporter;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Contingency;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.cnec.AngleCnec;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.powsybl.open_rao.data.crac_creation.creator.cim.crac_creator.cnec.AngleCnecCreationContext;
import com.powsybl.open_rao.data.swe_cne_exporter.xsd.AdditionalConstraintSeries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.powsybl.open_rao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

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
        List<AdditionalConstraintSeries> additionalConstraintSeriesList = new ArrayList<>();
        if (Objects.isNull(sweCneHelper.getAngleMonitoringResult())) {
            return additionalConstraintSeriesList;
        }
        List<AngleCnecCreationContext> sortedAngleCnecs = cracCreationContext.getAngleCnecCreationContexts().stream()
                .filter(AngleCnecCreationContext::isImported)
                .sorted(Comparator.comparing(AngleCnecCreationContext::getCreatedCnecId))
                .toList();
        if (Objects.isNull(contingency)) {
            sortedAngleCnecs.stream().filter(angleCnecCreationContext -> Objects.isNull(angleCnecCreationContext.getContingencyId()))
                    .forEach(angleCnecCreationContext ->
                BUSINESS_WARNS.warn("Preventive angle cnec {} will not be added to CNE file", angleCnecCreationContext.getNativeId()));
        } else {
            sortedAngleCnecs.stream()
                    .filter(angleCnecCreationContext -> angleCnecCreationContext.getContingencyId().equals(contingency.getId()))
                            .map(this::generateAdditionalConstraintSeries)
                                    .filter(Objects::nonNull)
                                            .forEach(additionalConstraintSeriesList::add);
        }
        return additionalConstraintSeriesList;
    }

    private AdditionalConstraintSeries generateAdditionalConstraintSeries(AngleCnecCreationContext angleCnecCreationContext) {
        Crac crac = sweCneHelper.getCrac();
        AngleCnec angleCnec = crac.getAngleCnec(angleCnecCreationContext.getCreatedCnecId());
        if (!angleCnec.getState().getInstant().isCurative()) {
            BUSINESS_WARNS.warn("{} angle cnec {} will not be added to CNE file", angleCnec.getState().getInstant(), angleCnecCreationContext.getNativeId());
            return null;
        }
        AdditionalConstraintSeries additionalConstraintSeries = new AdditionalConstraintSeries();
        additionalConstraintSeries.setMRID(angleCnecCreationContext.getCreatedCnecId());
        additionalConstraintSeries.setBusinessType(ANGLE_CNEC_BUSINESS_TYPE);
        additionalConstraintSeries.setName(angleCnec.getName());
        if (!sweCneHelper.getAngleMonitoringResult().isDivergent()) {
            additionalConstraintSeries.setQuantityQuantity(BigDecimal.valueOf(sweCneHelper.getAngleMonitoringResult().getAngle(angleCnec, Unit.DEGREE)).setScale(1, RoundingMode.HALF_UP));
        }
        return additionalConstraintSeries;
    }
}
