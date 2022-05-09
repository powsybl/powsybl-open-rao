/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.creator.cim.crac_creator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreator;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.cnec.MonitoredSeriesCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.contingency.CimContingencyCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.remedial_action.RemedialActionSeriesCreator;
import com.farao_community.farao.data.crac_creation.creator.cim.parameters.CimCracCreationParameters;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.crac_creation.creator.cim.CimCrac;
import com.farao_community.farao.data.crac_creation.creator.cim.xsd.TimeSeries;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(CracCreator.class)
public class CimCracCreator implements CracCreator<CimCrac, CimCracCreationContext> {
    private List<TimeSeries> cimTimeSeries;
    private Crac crac;
    private Network network;
    CimCracCreationContext creationContext;
    private CimCracCreationParameters cimCracCreationParameters;

    @Override
    public String getNativeCracFormat() {
        return "CimCrac";
    }

    @Override
    public CimCracCreationContext createCrac(CimCrac cimCrac, Network network, OffsetDateTime offsetDateTime, CracCreationParameters parameters) {
        // Set attributes
        this.crac = parameters.getCracFactory().create(cimCrac.getCracDocument().getMRID());
        this.network = network;
        this.cimTimeSeries = cimCrac.getCracDocument().getTimeSeries();
        this.creationContext = new CimCracCreationContext(crac);

        // Get warning messages from parameters parsing
        this.cimCracCreationParameters = parameters.getExtension(CimCracCreationParameters.class);
        if (cimCracCreationParameters != null) {
            cimCracCreationParameters.getFailedParseWarnings().forEach(message -> creationContext.getCreationReport().warn(message));
        }

        if (offsetDateTime == null) {
            creationContext.getCreationReport().warn("Timestamp is null for cim crac creator. No check will be performed.");
        } else {
            String cracTimePeriodStart = cimCrac.getCracDocument().getTimePeriodTimeInterval().getStart();
            String cracTimePeriodEnd = cimCrac.getCracDocument().getTimePeriodTimeInterval().getEnd();
            if (!isInTimeInterval(offsetDateTime, cracTimePeriodStart, cracTimePeriodEnd)) {
                creationContext.getCreationReport().error(String.format("Timestamp %s is not in time interval [%s %s].", offsetDateTime, cracTimePeriodStart, cracTimePeriodEnd));
                return creationContext.creationFailure();
            }
        }

        createContingencies();
        createCnecs();
        createRemedialActions();
        creationContext.buildCreationReport();
        return creationContext.creationSuccess(crac);
    }

    private void createContingencies() {
        new CimContingencyCreator(cimTimeSeries, crac, network, creationContext).createAndAddContingencies();
    }

    private void createCnecs() {
        new MonitoredSeriesCreator(cimTimeSeries, crac, network, creationContext).createAndAddMonitoredSeries();
    }

    private void createRemedialActions() {
        new RemedialActionSeriesCreator(cimTimeSeries, crac, network, creationContext, cimCracCreationParameters).createAndAddRemedialActionSeries();
    }

    private boolean isInTimeInterval(OffsetDateTime offsetDateTime, String startTime, String endTime) {
        OffsetDateTime startTimeBranch = OffsetDateTime.parse(startTime);
        OffsetDateTime endTimeBranch = OffsetDateTime.parse(endTime);
        // Select valid critical branches
        return !offsetDateTime.isBefore(startTimeBranch) && offsetDateTime.isBefore(endTimeBranch);
    }
}
