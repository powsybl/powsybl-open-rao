/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreator;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.VoltageCnecsCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.TimeSeries;
import com.farao_community.farao.data.crac_util.CracValidator;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CimCracCreator implements CracCreator<CimCrac, CimCracCreationContext> {
    private List<TimeSeries> cimTimeSeries;
    private Crac crac;
    private Network network;
    CimCracCreationContext creationContext;

    @Override
    public String getNativeCracFormat() {
        return "CimCrac";
    }

    @Override
    public CimCracCreationContext createCrac(CimCrac cimCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters parameters) {
        // Set attributes
        this.crac = parameters.getCracFactory().create(cimCrac.getCracDocument().getMRID());
        this.network = network;
        this.cimTimeSeries = new ArrayList<>(cimCrac.getCracDocument().getTimeSeries());
        this.creationContext = new CimCracCreationContext(crac, offsetDateTime);

        // Get warning messages from parameters parsing
        CimCracCreationParameters cimCracCreationParameters = parameters.getExtension(CimCracCreationParameters.class);
        if (cimCracCreationParameters != null) {
            cimCracCreationParameters.getFailedParseWarnings().forEach(message -> creationContext.getCreationReport().warn(message));
            if (!cimCracCreationParameters.getTimeseriesMrids().isEmpty()) {
                this.cimTimeSeries.removeIf(ts -> !cimCracCreationParameters.getTimeseriesMrids().contains(ts.getMRID()));
                cimCracCreationParameters.getTimeseriesMrids().stream()
                    .filter(mrid -> this.cimTimeSeries.stream().map(TimeSeries::getMRID).noneMatch(id -> id.equals(mrid)))
                    .forEach(mrid -> creationContext.getCreationReport().warn(String.format("Requested TimeSeries mRID \"%s\" in CimCracCreationParameters was not found in the CRAC file.", mrid)));
            }
        }

        if (offsetDateTime == null) {
            creationContext.getCreationReport().error("Timestamp is null for cim crac creator.");
            return creationContext.creationFailure();
        } else {
            String cracTimePeriodStart = cimCrac.getCracDocument().getTimePeriodTimeInterval().getStart();
            String cracTimePeriodEnd = cimCrac.getCracDocument().getTimePeriodTimeInterval().getEnd();
            if (!isInTimeInterval(offsetDateTime, cracTimePeriodStart, cracTimePeriodEnd)) {
                creationContext.getCreationReport().error(String.format("Timestamp %s is not in time interval [%s %s].", offsetDateTime, cracTimePeriodStart, cracTimePeriodEnd));
                return creationContext.creationFailure();
            }
        }

        createContingencies();
        createCnecs(parameters.getDefaultMonitoredSides());
        createRemedialActions(cimCracCreationParameters);
        createVoltageCnecs(cimCracCreationParameters);
        creationContext.buildCreationReport();
        CracValidator.validateCrac(crac, network).forEach(creationContext.getCreationReport()::added);
        return creationContext.creationSuccess(crac);
    }

    private void createContingencies() {
        new CimContingencyCreator(cimTimeSeries, crac, network, creationContext).createAndAddContingencies();
    }

    private void createCnecs(Set<Side> defaultMonitoredSides) {
        new MonitoredSeriesCreator(cimTimeSeries, network, creationContext, defaultMonitoredSides).createAndAddMonitoredSeries();
    }

    private void createRemedialActions(CimCracCreationParameters cimCracCreationParameters) {
        new RemedialActionSeriesCreator(cimTimeSeries, crac, network, creationContext, cimCracCreationParameters).createAndAddRemedialActionSeries();
    }

    private void createVoltageCnecs(CimCracCreationParameters cimCracCreationParameters) {
        if (cimCracCreationParameters != null && cimCracCreationParameters.getVoltageCnecsCreationParameters() != null) {
            new VoltageCnecsCreator(cimCracCreationParameters.getVoltageCnecsCreationParameters(), creationContext, network).createAndAddCnecs();
        }
    }

    private boolean isInTimeInterval(OffsetDateTime offsetDateTime, String startTime, String endTime) {
        OffsetDateTime startTimeBranch = OffsetDateTime.parse(startTime);
        OffsetDateTime endTimeBranch = OffsetDateTime.parse(endTime);
        // Select valid critical branches
        return !offsetDateTime.isBefore(startTimeBranch) && offsetDateTime.isBefore(endTimeBranch);
    }
}
