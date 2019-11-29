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
public class RemedialAction {
    @NotNull(message = "id")
    private final String id;
    @NotNull(message = "name")
    private final String name;
    @NotNull(message = "remedialActionElements.empty")
    @Valid
    private final List<RemedialActionElement> remedialActionElements;
    @NotNull(message = "usageRules.empty")
    @Valid
    private final List<UsageRule> usageRules;

    @ConstructorProperties({"id", "name", "remedialActionElements", "usageRules"})
    public RemedialAction(final String id,
                          final String name,
                          final List<RemedialActionElement>  remedialActionElements,
                          final List<UsageRule> usageRules) {
        this.id = id;
        this.name = name;
        if (remedialActionElements == null) {
            this.remedialActionElements = Collections.emptyList();
        } else {
            this.remedialActionElements = remedialActionElements;
        }
        if (usageRules == null) {
            this.usageRules = Collections.emptyList();
        } else {
            this.usageRules = usageRules;
        }
    }

}
