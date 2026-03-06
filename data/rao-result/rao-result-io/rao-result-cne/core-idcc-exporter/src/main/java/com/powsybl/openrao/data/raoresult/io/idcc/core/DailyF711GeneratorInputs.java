/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.idcc.core;

import static com.powsybl.openrao.data.raoresult.io.idcc.core.CracUtil.importNativeCrac;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.xsd.FlowBasedConstraintDocument;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.threeten.extra.Interval;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class DailyF711GeneratorInputs implements DailyF711GeneratorInputsProvider {
    private final F711GeneratorInput input;

    public DailyF711GeneratorInputs(TemporalData<RaoResult> raoResults, TemporalData<FbConstraintCreationContext> cracCreationContexts, String cracPath) {
        input = new F711GeneratorInput(raoResults, cracCreationContexts, cracPath);
    }

    @Override
    public FlowBasedConstraintDocument referenceConstraintDocument() {
        try {
            return importNativeCrac(new File(input.cracPath()));
        } catch (Exception e) {
            throw new OpenRaoException("Exception occurred during F303 file creation", e);
        }
    }

    @Override
    public Optional<HourlyF711InfoGenerator.Inputs> hourlyF303InputsForInterval(Interval interval) {
        OffsetDateTime startDate = interval.getStart().atOffset(ZoneOffset.UTC);
        OffsetDateTime endDate = interval.getEnd().atOffset(ZoneOffset.UTC);
        for (OffsetDateTime timestamp : input.cracCreationContexts().getTimestamps()) {
            if (startDate.isBefore(timestamp) && timestamp.isBefore(endDate)) {
                return Optional.of(new HourlyF711InfoGenerator.Inputs(
                    input.cracCreationContexts().getData(timestamp).orElseThrow(),
                    input.raoResults().getData(timestamp).orElseThrow(),
                    timestamp
                ));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean shouldBeReported(Interval interval) {
        return true;
    }
}
