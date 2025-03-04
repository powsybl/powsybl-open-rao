/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.ucte;

/**
 * Properties to customize UcteNetworkHelper behavior
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkAnalyzerProperties {

    // For Bus IDs with 7 characters, either complete them with white spaces or wildcards
    public enum BusIdMatchPolicy {
        COMPLETE_WITH_WHITESPACES,
        COMPLETE_WITH_WILDCARDS,
        REPLACE_8TH_CHARACTER_WITH_WILDCARD
    }

    public enum SuffixMatchPriority {
        ORDERCODE_BEFORE_NAME,
        NAME_BEFORE_ORDERCODE,
        ALL
    }

    private static final SuffixMatchPriority DEFAULT_SUFFIX_MATCH_PRIORITY = SuffixMatchPriority.ALL;

    private final BusIdMatchPolicy busIdMatchPolicy;
    private final SuffixMatchPriority suffixMatchPriority;

    public UcteNetworkAnalyzerProperties(BusIdMatchPolicy busIdMatchPolicy) {
        this(busIdMatchPolicy, DEFAULT_SUFFIX_MATCH_PRIORITY);
    }

    public UcteNetworkAnalyzerProperties(BusIdMatchPolicy busIdMatchPolicy, SuffixMatchPriority suffixMatchPriority) {
        this.busIdMatchPolicy = busIdMatchPolicy;
        this.suffixMatchPriority = suffixMatchPriority;
    }

    public BusIdMatchPolicy getBusIdMatchPolicy() {
        return busIdMatchPolicy;
    }

    public SuffixMatchPriority getSuffixMatchPriority() {
        return suffixMatchPriority;
    }

}
