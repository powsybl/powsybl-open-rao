/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flowbased_domain;

import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;

/**
 * Business Object of the FlowBased DataDomain
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class DataDomain {
    @NotNull(message = "id.empty")
    private final String id;
    @NotNull(message = "name.empty")
    private final String name;
    @NotNull(message = "sourceFormat.empty")
    private final String sourceFormat;
    @NotNull(message = "description.empty")
    private final String description;
    @NotNull(message = "dataPreContingency.empty")
    @Valid
    private final DataPreContingency dataPreContingency;

    @ConstructorProperties({"id", "name", "sourceFormat", "description", "dataPreContingency"})
    public DataDomain(final String id, final String name, final String sourceFormat, final String description, final DataPreContingency dataPreContingency) {
        this.id = id;
        this.name = name;
        this.sourceFormat = sourceFormat;
        this.description = description;
        this.dataPreContingency = dataPreContingency;
    }
}
