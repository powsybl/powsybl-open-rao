/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;

import java.util.Comparator;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class RaoResultJsonConstants {

    private RaoResultJsonConstants() { }

    public static final String CONTINGENCY_ID = "contingency";

    // costs
    public static final String COST_RESULTS = "costResults";
    public static final String FUNCTIONAL_COST = "functionalCost";
    public static final String VIRTUAL_COSTS = "virtualCost";

    // flowCnecResults
    public static final String FLOWCNEC_RESULTS = "flowCnecResults";
    public static final String FLOWCNEC_ID = "flowCnecId";
    public static final String FLOW = "flow";
    public static final String MARGIN = "margin";
    public static final String RELATIVE_MARGIN = "relativeMargin";
    public static final String COMMERCIAL_FLOW = "commercialFlow";
    public static final String LOOP_FLOW = "loopFlow";
    public static final String ZONAL_PTDF_SUM = "zonalPtdfSum";

    // networkActionResults
    public static final String NETWORKACTION_RESULTS = "networkActionResults";
    public static final String NETWORKACTION_ID = "networkActionId";
    public static final String STATES_ACTIVATED_NETWORKACTION = "activatedStates";

    // rangeActionResults
    public static final String PSTRANGEACTION_RESULTS = "pstRangeActionResults";
    public static final String PSTRANGEACTION_ID = "pstRangeActionId";
    public static final String PST_NETWORKELEMENT_ID = "pstNetworkElementId";
    public static final String STATES_ACTIVATED_PSTRANGEACTION = "activatedStates";
    public static final String INITIAL_TAP = "initialTap";
    public static final String INITIAL_SETPOINT = "initialSetpoint";
    public static final String AFTER_PRA_TAP = "afterPraTap";
    public static final String AFTER_PRA_SETPOINT = "afterPraSetpoint";
    public static final String TAP = "tap";
    public static final String SETPOINT = "setpoint";

    // instants
    public static final String INSTANT = "instant";
    public static final String PREVENTIVE_INSTANT = "preventive";
    public static final String OUTAGE_INSTANT = "outage";
    public static final String AUTO_INSTANT = "auto";
    public static final String CURATIVE_INSTANT = "curative";

    // units
    public static final String AMPERE_UNIT = "ampere";
    public static final String MEGAWATT_UNIT = "megawatt";
    public static final String DEGREE_UNIT = "degree";
    public static final String KILOVOLT_UNIT = "kilovolt";
    public static final String PERCENT_IMAX_UNIT = "percent_imax";
    public static final String TAP_UNIT = "tap";

    // optimization states
    public static final String INITIAL_OPT_STATE = "initial";
    public static final String AFTER_PRA_OPT_STATE = "afterPRA";
    public static final String AFTER_CRA_OPT_STATE = "afterCRA";

    // computation statuses
    public static final String COMPUTATION_STATUS = "computationStatus";
    public static final String DEFAULT_STATUS = "default";
    public static final String FALLBACK_STATUS = "fallback";
    public static final String FAILURE_STATUS = "failure";

    public static String serializeUnit(Unit unit) {
        switch (unit) {
            case AMPERE:
                return AMPERE_UNIT;
            case DEGREE:
                return DEGREE_UNIT;
            case MEGAWATT:
                return MEGAWATT_UNIT;
            case KILOVOLT:
                return KILOVOLT_UNIT;
            case PERCENT_IMAX:
                return PERCENT_IMAX_UNIT;
            case TAP:
                return TAP_UNIT;
            default:
                throw new FaraoException(String.format("Unsupported unit %s", unit));
        }
    }

    public static Unit deserializeUnit(String stringValue) {
        switch (stringValue) {
            case AMPERE_UNIT:
                return Unit.AMPERE;
            case DEGREE_UNIT:
                return Unit.DEGREE;
            case MEGAWATT_UNIT:
                return Unit.MEGAWATT;
            case KILOVOLT_UNIT:
                return Unit.KILOVOLT;
            case PERCENT_IMAX_UNIT:
                return Unit.PERCENT_IMAX;
            case TAP_UNIT:
                return Unit.TAP;
            default:
                throw new FaraoException(String.format("Unrecognized unit %s", stringValue));
        }
    }

    public static String serializeInstant(Instant instant) {
        switch (instant) {
            case PREVENTIVE:
                return PREVENTIVE_INSTANT;
            case OUTAGE:
                return OUTAGE_INSTANT;
            case AUTO:
                return AUTO_INSTANT;
            case CURATIVE:
                return CURATIVE_INSTANT;
            default:
                throw new FaraoException(String.format("Unsupported instant %s", instant));
        }
    }

    public static Instant deserializeInstant(String stringValue) {
        switch (stringValue) {
            case PREVENTIVE_INSTANT:
                return Instant.PREVENTIVE;
            case OUTAGE_INSTANT:
                return Instant.OUTAGE;
            case AUTO_INSTANT:
                return Instant.AUTO;
            case CURATIVE_INSTANT:
                return Instant.CURATIVE;
            default:
                throw new FaraoException(String.format("Unrecognized instant %s", stringValue));
        }
    }

    public static String serializeOptimizationState(OptimizationState optimizationState) {
        switch (optimizationState) {
            case INITIAL:
                return INITIAL_OPT_STATE;
            case AFTER_PRA:
                return AFTER_PRA_OPT_STATE;
            case AFTER_CRA:
                return AFTER_CRA_OPT_STATE;
            default:
                throw new FaraoException(String.format("Unsupported optimization state %s", optimizationState));
        }
    }

    public static OptimizationState deserializeOptimizationState(String stringValue) {
        switch (stringValue) {
            case INITIAL_OPT_STATE:
                return OptimizationState.INITIAL;
            case AFTER_PRA_OPT_STATE:
                return OptimizationState.AFTER_PRA;
            case AFTER_CRA_OPT_STATE:
                return OptimizationState.AFTER_CRA;
            default:
                throw new FaraoException(String.format("Unrecognized optimization state %s", stringValue));
        }
    }

    public static String serializeStatus(ComputationStatus computationStatus) {
        switch (computationStatus) {
            case DEFAULT:
                return DEFAULT_STATUS;
            case FALLBACK:
                return FALLBACK_STATUS;
            case FAILURE:
                return FAILURE_STATUS;
            default:
                throw new FaraoException(String.format("Unsupported computation status %s", computationStatus));
        }
    }

    public static ComputationStatus deserializeStatus(String stringValue) {
        switch (stringValue) {
            case DEFAULT_STATUS:
                return ComputationStatus.DEFAULT;
            case FALLBACK_STATUS:
                return ComputationStatus.FALLBACK;
            case FAILURE_STATUS:
                return ComputationStatus.FAILURE;
            default:
                throw new FaraoException(String.format("Unrecognized computation status %s", stringValue));
        }
    }

    // state comparator
    public static final Comparator<State> STATE_COMPARATOR = (s1, s2) -> {
        if (s1.getInstant().getOrder() != s2.getInstant().getOrder()) {
            return s1.compareTo(s2);
        } else if (s1.getInstant().equals(Instant.PREVENTIVE)) {
            return 0;
        } else {
            return s1.getContingency().get().getId().compareTo(s2.getContingency().get().getId());
        }
    };

}
