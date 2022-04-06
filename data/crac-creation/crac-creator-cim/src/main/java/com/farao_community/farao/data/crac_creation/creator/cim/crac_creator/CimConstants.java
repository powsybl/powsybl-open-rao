/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import java.util.List;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class CimConstants {
    private CimConstants() { }

    // --- GENERAL
    public static final String MEGAWATT_UNIT_SYMBOL = "MAW";
    public static final String AMPERES_UNIT_SYMBOL = "AMP";

    // --- Contingencies
    public static final String CONTINGENCY_SERIES_BUSINESS_TYPE = "B55";

    // --- Cnecs
    public static final String CNECS_SERIES_BUSINESS_TYPE = "B57";
    public static final String CNECS_MNEC_MARKET_OBJECT_STATUS = "A49";
    public static final String CNECS_OPTIMIZED_MARKET_OBJECT_STATUS = "A52";
    public static final String CNECS_DIRECT_DIRECTION_FLOW = "A01";
    public static final String CNECS_OPPOSITE_DIRECTION_FLOW = "A02";
    public static final String CNECS_N_STATE_MEASUREMENT_TYPE = "A02";
    public static final String CNECS_OUTAGE_STATE_MEASUREMENT_TYPE = "A07";
    public static final String CNECS_AUTO_STATE_MEASUREMENT_TYPE = "A12";
    public static final String CNECS_CURATIVE_STATE_MEASUREMENT_TYPE = "A13";
    public static final String CNECS_PATL_UNIT_SYMBOL = "P1";

    // --- Remedial Actions
    public static final String REMEDIAL_ACTIONS_SERIES_BUSINESS_TYPE = "B56";
    public static final List<String> REMEDIAL_ACTION_AUTHORIZED_OPTIMIZATION_STATUS = List.of("Z01", "A01", "A52", "A49");
    public static final String NETWORK_ELEMENT_BUSINESS_TYPE = "B59";

    public enum AuthorizedRemedialActionApplicationModeMarketObjectStatus {
        PRA("A18"),
        CRA("A19"),
        PRA_AND_CRA("A27"),
        AUTO("A20");

        private String status;

        AuthorizedRemedialActionApplicationModeMarketObjectStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status; }
    }

    public enum AvailabilityMarketObjectStatus {
        MIGHT_BE_USED("A39"),
        SHALL_BE_USED("A38");

        private String status;

        AvailabilityMarketObjectStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status; }
    }

    public enum MarketObjectStatus {
        ABSOLUTE("A26"),
        RELATIVE_TO_INITIAL_NETWORK("A25"),
        RELATIVE_TO_PREVIOUS_INSTANT1("A51"),
        RELATIVE_TO_PREVIOUS_INSTANT2("Z01"),
        STOP("A23"),
        OPEN("A21"),
        CLOSE("A22");

        private String status;

        MarketObjectStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status; }
    }

    // ------ PST
    public static final String PST_CAPACITY_UNIT_SYMBOL = "C62";

    // ------ Network Actions
    public enum NetworkActionPsrType {
        TIE_LINE("A01"),
        LINE("A02"),
        GENERATION("A04"),
        LOAD("A05"),
        PST("A06"),
        CIRCUIT_BREAKER("A07"),
        TRANSFORMER("B24"),
        DEPRECATED_LINE("A12");

        private String status;

        NetworkActionPsrType(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status; }
    }
}
