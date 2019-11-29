/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import java.util.List;

/**
 * Business object for a contingency in the CRAC file
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class Contingency {
    @NotNull(message = "Contingency.id.empty")
    private final String id;
    @NotNull(message = "Contingency.name.empty")
    private final String name;
    @NotNull(message = "Contingency.monitoredBranches.empty")
    @Valid
    private final List<MonitoredBranch> monitoredBranches;
    @NotNull(message = "Contingency.contingencyElements.empty")
    @Valid
    private final List<ContingencyElement> contingencyElements;

    @ConstructorProperties({"id", "name", "monitoredBranches", "contingencyElements"})
    public Contingency(final String id, final String name, final List<MonitoredBranch> monitoredBranches,
                       final List<ContingencyElement> contingencyElements) {
        this.id = id;
        this.name = name;
        this.monitoredBranches = monitoredBranches;
        this.contingencyElements = contingencyElements;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        Contingency other = (Contingency) o;

        // The contingencies are equal if the id, name, and all contingencyElements are the same.
        return this.id.equals(other.id) &&
                this.name.equals(other.name) &&
                this.contingencyElements.equals(other.getContingencyElements());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
            append(id).
            append(name).
            append(contingencyElements).
            toHashCode();
    }
}
