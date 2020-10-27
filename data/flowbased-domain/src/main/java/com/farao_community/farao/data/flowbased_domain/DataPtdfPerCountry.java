/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.flowbased_domain;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.beans.ConstructorProperties;

/**
 * Business Object of the FlowBased DataPtdfPerCountry
 *
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
@Builder
@Data
public class DataPtdfPerCountry {
    @NotNull(message = "dataPtdfPerCountry.country.empty")
    private final String country;
    @NotNull(message = "dataPtdfPerCountry.ptdf.empty")
    private final double ptdf;

    @ConstructorProperties({"country", "ptdf"})
    public DataPtdfPerCountry(final String country, final double ptdf) {
        this.country = country;
        this.ptdf = ptdf;
    }

}
