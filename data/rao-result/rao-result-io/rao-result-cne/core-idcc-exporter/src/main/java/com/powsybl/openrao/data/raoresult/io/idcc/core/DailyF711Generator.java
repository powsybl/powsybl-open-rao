/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.idcc.core;

import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import org.threeten.extra.Interval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class DailyF711Generator {

    private DailyF711Generator() {
        throw new AssertionError("Static class. Should not be constructed");
    }

    public static FlowBasedConstraintDocument generate(DailyF711GeneratorInputsProvider inputsProvider) {
        FlowBasedConstraintDocument flowBasedConstraintDocument = inputsProvider.referenceConstraintDocument();
        Map<Integer, Interval> positionMap = IntervalUtil.getPositionsMap(flowBasedConstraintDocument.getConstraintTimeInterval().getV());
        List<HourlyF711Info> hourlyF711Infos = new ArrayList<>();
        positionMap.values().forEach(interval -> {
            if (inputsProvider.shouldBeReported(interval)) {
                Optional<HourlyF711InfoGenerator.Inputs> optionalInputs = inputsProvider.hourlyF303InputsForInterval(interval);
                if (optionalInputs.isPresent()) {
                    hourlyF711Infos.add(HourlyF711InfoGenerator.getInfoForSuccessfulInterval(flowBasedConstraintDocument, interval, optionalInputs.get()));
                } else {
                    hourlyF711Infos.add(HourlyF711InfoGenerator.getInfoForNonRequestedOrFailedInterval(flowBasedConstraintDocument, interval));
                }
            }
        });

        // gather hourly info in one common document, cluster the elements that can be clusterized
        return new DailyF711Clusterizer(hourlyF711Infos, flowBasedConstraintDocument).generateClusterizedDocument();
    }
}
