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
 * Business object of the CRAC file
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class CracFile {
    @NotNull(message = "id.empty")
    private final String id;
    @NotNull(message = "name.empty")
    private final String name;
    private final String sourceFormat;
    private final String description;
    @Valid
    private final PreContingency preContingency;
    @Valid
    private final List<Contingency> contingencies;
    @Valid
    private final List<RemedialAction> remedialActions;

    @ConstructorProperties({"id", "name", "sourceFormat", "description", "preContingency", "contingencies", "remedialActions"})
    public CracFile(final String id,
                    final String name,
                    final String sourceFormat,
                    final String description,
                    final PreContingency preContingency,
                    final List<Contingency> contingencies,
                    final List<RemedialAction> remedialActions) {
        this.id = id;
        this.name = name;
        this.sourceFormat = sourceFormat;
        this.description = description;
        this.preContingency = preContingency;
        if (contingencies == null) {
            this.contingencies = Collections.emptyList();
        } else {
            this.contingencies = contingencies;
        }
        if (remedialActions == null) {
            this.remedialActions = Collections.emptyList();
        } else {
            this.remedialActions = remedialActions;
        }
    }
}
