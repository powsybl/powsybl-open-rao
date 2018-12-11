/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_file;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;

/**
 * Business object of a contingency element in the CRAC file
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class ContingencyElement {
    @NotNull(message = "ContingencyElement.elementId.empty")
    private final String elementId;
    @NotNull(message = "ContingencyElement.name.empty")
    private final String name;

    @ConstructorProperties({"elementId", "name"})
    public ContingencyElement(final String elementId, final String name) {
        this.elementId = elementId;
        this.name = name;
    }

}
