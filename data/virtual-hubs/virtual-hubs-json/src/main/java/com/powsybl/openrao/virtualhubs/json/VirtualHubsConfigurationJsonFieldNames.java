/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs.json;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class VirtualHubsConfigurationJsonFieldNames {
    private VirtualHubsConfigurationJsonFieldNames() {
    }

    static final String REGION = "region";
    static final String SYNCHRONOUS_AREAS = "synchronousAreas";
    static final String CONTROL_AREAS = "controlAreas";
    static final String MARKET_AREAS = "marketAreas";
    static final String VIRTUAL_HUBS = "virtualHubs";
    static final String BORDER_DIRECTIONS = "borderDirections";
    static final String INTERNAL_HVDCS = "internalHvdcs";
    static final String CODE = "code";
    static final String EIC = "eic";
    static final String IS_LIVE = "isLive";
    static final String IS_MAIN_AREA = "isMainArea";
    static final String LOCAL_LOT_3_PARAMETERS = "localLOT3Parameters";
    static final String INITIAL_VOLTAGES = "initialVoltages";
    static final String NOMINAL = "nominal";
    static final String INITIAL = "initial";
    static final String SLACK_SEARCH_PRIORITY = "slackSearchPriority";
    static final String IS_MC_PARTICIPANT = "isMcParticipant";
    static final String IS_TECH_COUNTERPARTY = "isTechCounterparty";
    static final String IS_SDAC = "isSdac";
    static final String IS_AHC = "isAhc";
    static final String SYNCHRONOUS_AREA = "synchronousArea";
    static final String RELATED_CA = "relatedCa";
    static final String IS_ITCP = "isItcp";
    static final String IS_IGNORE_ECS = "isIgnoreECs";
    static final String NODE_NAME = "nodeName";
    static final String RELATED_MA = "relatedMa";
    static final String OPPOSITE_HUB = "oppositeHub";
    static final String SPLITTING_FACTOR = "splittingFactor";
    static final String LF = "lf";
    static final String RLF = "rlf";
    static final String FROM = "from";
    static final String TO = "to";
    static final String POLES = "poles";
    static final String IS_MTHVDC = "isMthvdc";
    static final String IS_BIPOLAR = "isBipolar";
    static final String ID = "id";
    static final String CONVERTERS = "converters";
    static final String LINES = "lines";
    static final String NODE = "node";
    static final String STATION = "station";
}
