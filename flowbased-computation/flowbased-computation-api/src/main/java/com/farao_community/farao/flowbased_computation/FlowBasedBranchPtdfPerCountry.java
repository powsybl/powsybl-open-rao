/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

/**
 * FlowBasedBranchPtdfPerCountry
 */
public class FlowBasedBranchPtdfPerCountry {
    private final String country;
    private final double ptdf;

    public FlowBasedBranchPtdfPerCountry(final String country, final double ptdf) {
        this.country = country;
        this.ptdf = ptdf;
    }

    public String getCountry() {
        return country;
    }

    public double getPtdf() {
        return ptdf;
    }
}
