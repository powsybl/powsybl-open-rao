/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.cim.craccreator;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.cnec.MonitoredSeriesCreator;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.cnec.VoltageCnecsCreator;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.contingency.CimContingencyCreator;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.remedialaction.RemedialActionSeriesCreator;
import com.powsybl.openrao.data.craccreation.creator.cim.parameters.CimCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.CRACMarketDocument;
import com.powsybl.openrao.data.craccreation.creator.cim.xsd.TimeSeries;
import com.powsybl.openrao.data.craccreation.util.RaUsageLimitsAdder;
import com.powsybl.openrao.data.cracutil.CracValidator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class CimCracCreator {
    private List<TimeSeries> cimTimeSeries;
    private Crac crac;
    private Network network;
    CimCracCreationContext creationContext;

    CimCracCreationContext createCrac(CRACMarketDocument cimCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters parameters) {
        // Set attributes
        this.crac = parameters.getCracFactory().create(cimCrac.getMRID());
        addCimInstants();
        RaUsageLimitsAdder.addRaUsageLimits(crac, parameters);
        this.network = network;
        this.cimTimeSeries = new ArrayList<>(cimCrac.getTimeSeries());
        this.creationContext = new CimCracCreationContext(crac, offsetDateTime, network.getNameOrId());

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
            String cracTimePeriodStart = cimCrac.getTimePeriodTimeInterval().getStart();
            String cracTimePeriodEnd = cimCrac.getTimePeriodTimeInterval().getEnd();
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

    private void addCimInstants() {
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
    }

    private void createContingencies() {
        new CimContingencyCreator(cimTimeSeries, crac, network, creationContext).createAndAddContingencies();
    }

    private void createCnecs(Set<TwoSides> defaultMonitoredSides) {
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
