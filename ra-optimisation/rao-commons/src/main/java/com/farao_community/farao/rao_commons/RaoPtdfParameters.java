/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains parameters for PTDF computation in RAO
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoPtdfParameters extends AbstractExtension<RaoParameters> {

    private static final String COUNTRY_CODES_FORMAT_EXCEPTION = "Country boundaries should be formatted 'XX-YY' where XX and YY are the 2-character country codes";
    public static final double DEFAULT_PTDF_SUM_LOWER_BOUND = 0.01;

    private List<Pair<Country, Country>> boundaries = new ArrayList<>();
    private double ptdfSumLowerBound = DEFAULT_PTDF_SUM_LOWER_BOUND; // prevents relative margins from diverging to +infinity

    @Override
    public String getName() {
        return "RaoPtdfParameters";
    }

    public List<Pair<Country, Country>> getBoundaries() {
        return boundaries;
    }

    public void setBoundaries(List<Pair<Country, Country>> boundaries) {
        this.boundaries = boundaries;
    }

    public List<String> getBoundariesAsString() {
        return boundaries.stream()
                .map(countryPair -> {
                    return countryPair.getLeft().toString() + "-" + countryPair.getRight().toString();
                })
                .collect(Collectors.toList());
    }

    public void setBoundariesFromCountryCodes(List<String> boundaries) {
        this.boundaries = boundaries.stream()
                .map(stringPair -> {
                    if (stringPair.length() != 5) {
                        throw new FaraoException(COUNTRY_CODES_FORMAT_EXCEPTION);
                    }
                    try {
                        Country left = Country.valueOf(stringPair.substring(0, 2));
                        Country right = Country.valueOf(stringPair.substring(3, 5));
                        return new ImmutablePair<>(left, right);
                    } catch (IllegalArgumentException e) {
                        throw new FaraoException(COUNTRY_CODES_FORMAT_EXCEPTION);
                    }
                })
                .collect(Collectors.toList());
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    public void setPtdfSumLowerBound(double ptdfSumLowerBound) {
        this.ptdfSumLowerBound = ptdfSumLowerBound;
    }

}
