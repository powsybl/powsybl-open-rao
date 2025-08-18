/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.idcc.core;

import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import org.threeten.extra.Interval;

import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public interface DailyF711GeneratorInputsProvider {
    FlowBasedConstraintDocument referenceConstraintDocument();

    Optional<HourlyF711InfoGenerator.Inputs> hourlyF303InputsForInterval(Interval interval);

    boolean shouldBeReported(Interval interval);

}