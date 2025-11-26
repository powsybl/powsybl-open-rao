/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ZoneToZonePtdfDefinition {
    private static final String WRONG_SYNTAX_MSG = "ZoneToZonePtdfDefinition should have the following syntax: {Code_1}-{Code_2}+{Code_3}... where Code_i are 16-characters EI codes or 2-characters country codes.";

    private final List<WeightedZoneToSlackPtdf> zoneToSlackPtdfs;
    private String zoneToZonePtdfAsString;

    public static class WeightedZoneToSlackPtdf {
        private final EICode ptdfZoneToSlack;
        private final double weight;

        WeightedZoneToSlackPtdf(EICode ptdfZoneToSlack, double weight) {
            this.ptdfZoneToSlack = ptdfZoneToSlack;
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }

        public EICode getEiCode() {
            return ptdfZoneToSlack;
        }

        @Override
        public String toString() {
            if (!(weight > 1 || weight < 1)) {
                return String.format("+{%s}", ptdfZoneToSlack.toString());
            } else if (!(weight > -1 || weight < -1)) {
                return String.format("-{%s}", ptdfZoneToSlack.toString());
            } else {
                throw new NotImplementedException("Method toString() has not been implemented for weight different from 1 or -1");
            }
        }
    }

    public ZoneToZonePtdfDefinition(List<WeightedZoneToSlackPtdf> zoneToSlackPtdfs) {
        this.zoneToSlackPtdfs = zoneToSlackPtdfs;
    }

    public ZoneToZonePtdfDefinition(String zoneToZonePtdf) {
        /*
        Examples of valid strings :
        "{FR}-{ES}
        "{EICODEWITH16CHAR}-{EICODEWITH16CHAR}
        "{FR}+{DE}-{BE}+{ES}

        More generally: a concatenation of "+{Code}" or "{-Code}" where :
        - Code are 2-characters country Code or 16-characters EI-Code
        - the concatenation contains at least one element
        - the +/- of the first element of the concatenation can be omitted (if so, it will be considered as a +)
        */

        this.zoneToSlackPtdfs = parsePtdfBoundaryString(zoneToZonePtdf);
        this.zoneToZonePtdfAsString = zoneToZonePtdf;
    }

    public List<WeightedZoneToSlackPtdf> getZoneToSlackPtdfs() {
        return zoneToSlackPtdfs;
    }

    public List<EICode> getEiCodes() {
        return zoneToSlackPtdfs.stream().map(WeightedZoneToSlackPtdf::getEiCode).toList();
    }

    public double getWeight(EICode eiCode) {
        return zoneToSlackPtdfs.stream()
            .filter(zToSPtdf -> zToSPtdf.getEiCode().equals(eiCode))
            .mapToDouble(WeightedZoneToSlackPtdf::getWeight)
            .sum();
    }

    @Override
    public String toString() {
        if (Objects.isNull(zoneToZonePtdfAsString)) {
            this.zoneToZonePtdfAsString = getZoneToSlackPtdfs().stream().map(WeightedZoneToSlackPtdf::toString).collect(Collectors.joining(""));
        }
        return zoneToZonePtdfAsString;
    }

    private List<WeightedZoneToSlackPtdf> parsePtdfBoundaryString(String ptdfBoundaryAsString) {

        List<WeightedZoneToSlackPtdf> zoneToSlackList = new ArrayList<>();

        Pattern beginningInCurlyBrackets = Pattern.compile("^\\{(.*?)\\}");
        Pattern positiveInCurlyBrackets = Pattern.compile("\\+\\{(.*?)\\}");
        Pattern negativeInCurlyBrackets = Pattern.compile("\\-\\{(.*?)\\}");

        beginningInCurlyBrackets.matcher(ptdfBoundaryAsString).results().forEach(re -> zoneToSlackList.add(new WeightedZoneToSlackPtdf(convertBracketIntoEiCode(re.group()), 1.)));
        positiveInCurlyBrackets.matcher(ptdfBoundaryAsString).results().forEach(re -> zoneToSlackList.add(new WeightedZoneToSlackPtdf(convertBracketIntoEiCode(re.group()), 1.)));
        negativeInCurlyBrackets.matcher(ptdfBoundaryAsString).results().forEach(re -> zoneToSlackList.add(new WeightedZoneToSlackPtdf(convertBracketIntoEiCode(re.group()), -1.)));

        if (zoneToSlackList.size() != StringUtils.countMatches(ptdfBoundaryAsString, "{") || zoneToSlackList.isEmpty()) {
            throw new OpenRaoException(WRONG_SYNTAX_MSG);
        }

        return zoneToSlackList;
    }

    private EICode convertBracketIntoEiCode(String eiCodeInBrackets) {
        String eiCodeAsString = StringUtils.substringBetween(eiCodeInBrackets, "{", "}");
        if (StringUtils.isEmpty(eiCodeAsString)) {
            throw new OpenRaoException(WRONG_SYNTAX_MSG);
        }
        if (eiCodeAsString.length() == 16) {
            return new EICode(eiCodeAsString);
        } else if (eiCodeAsString.length() == 2) {
            return new EICode(Country.valueOf(eiCodeAsString));
        } else {
            throw new OpenRaoException(WRONG_SYNTAX_MSG);
        }
    }
}
