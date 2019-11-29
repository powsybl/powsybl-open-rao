/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file;

import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.List;

/**
 * Remedial Action object in the CRAC file
 *
 * @author Luc Di-Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
@Builder
@Data
public class UsageRule {

    public enum Instant {
        N,
        OUTAGE,
        AUTO,
        CURATIVE
    }

    public enum Usage {
        FREE_TO_USE,
        ON_OUTAGE,
        ON_CONSTRAINT
    }

    private final String id;
    @NotNull(message = "instants")
    private final Instant instants;
    @NotNull(message = "usage")
    private final Usage usage;
    @Valid
    private final List<String> contingenciesID;
    @Valid
    private final List<String> constraintsID;

    @ConstructorProperties({"id", "instants", "usage", "contingenciesID", "constraintsID"})
    public UsageRule(final String id,
                     final Instant instants,
                     final Usage usage,
                     final List<String> contingenciesID,
                     final List<String> constraintsID) {
        this.id = id;
        this.instants = instants;
        this.usage = usage;
        if (contingenciesID == null) {
            this.contingenciesID = Collections.emptyList();
        } else {
            this.contingenciesID = contingenciesID;
        }
        if (constraintsID == null) {
            this.constraintsID = Collections.emptyList();
        } else {
            this.constraintsID = constraintsID;
        }
    }

}
