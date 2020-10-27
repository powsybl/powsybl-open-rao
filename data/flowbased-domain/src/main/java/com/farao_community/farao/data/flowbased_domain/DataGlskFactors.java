/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flowbased_domain;

import lombok.Builder;
import lombok.Data;

import javax.validation.Valid;
import java.beans.ConstructorProperties;
import java.util.Collections;
import java.util.Map;

/**
 * Business Object of the FlowBased DataGlskFactors
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@Builder
@Data
public class DataGlskFactors {
    private String areaId;
    @Valid
    private final Map<String, Float> glskFactors;

    @ConstructorProperties({"areaId", "glskFactors"})
    public DataGlskFactors(final String areaId, final Map<String, Float> glskFactors) {
        this.areaId = areaId;
        this.glskFactors = Collections.unmodifiableMap(glskFactors);
    }
}
