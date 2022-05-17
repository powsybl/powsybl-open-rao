/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.swe_cne_exporter.xsd.ConstraintSeries;
import com.farao_community.farao.data.swe_cne_exporter.xsd.CriticalNetworkElementMarketDocument;
import com.farao_community.farao.data.swe_cne_exporter.xsd.Point;
import com.farao_community.farao.data.swe_cne_exporter.xsd.SeriesPeriod;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;
import org.joda.time.DateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.data.swe_cne_exporter.SweCneClassCreator.*;
import static com.farao_community.farao.data.swe_cne_exporter.SweCneConstants.*;
import static com.farao_community.farao.data.swe_cne_exporter.SweCneUtil.*;

/**
 * Fills the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SweCne {
    private CriticalNetworkElementMarketDocument marketDocument;
    private SweCneHelper cneHelper;

    public SweCne(Crac crac, Network network, CimCracCreationContext cracCreationContext, RaoResult raoResult, RaoParameters raoParameters, SweCneExporterParameters exporterParameters) {
        marketDocument = new CriticalNetworkElementMarketDocument();
        cneHelper = new SweCneHelper(crac, network, cracCreationContext, raoResult, raoParameters, exporterParameters);
    }

    public CriticalNetworkElementMarketDocument getMarketDocument() {
        return marketDocument;
    }

    // Main method
    public void generate() {
        // Reset unique IDs
        SweCneUtil.initUniqueIds();

        // this will crash if cneHelper.getCracCreationContext().getTimeStamp() is null
        // the usage of a timestamp in FbConstraintCracCreator is mandatory so it shouldn't be an issue
        if (Objects.isNull(cneHelper.getCracCreationContext().getTimeStamp())) {
            throw new FaraoException("Cannot export CNE file if the CRAC has no timestamp");
        }

        OffsetDateTime offsetDateTime = cneHelper.getCracCreationContext().getTimeStamp().withMinute(0);
        fillHeader(cneHelper.getNetwork().getCaseDate().toDate().toInstant().atOffset(ZoneOffset.UTC));
        addTimeSeriesToCne(offsetDateTime);
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        // fill CNE
        createAllConstraintSeries(point);
    }

    // fills the header of the CNE
    private void fillHeader(OffsetDateTime networkTime) {
        marketDocument.setMRID(SweCneUtil.randomizeString("", 5));
        marketDocument.setRevisionNumber(REVISION_NUMBER_1);
        marketDocument.setType(CNE_TYPE);
        marketDocument.setProcessProcessType(Z01_PROCESS_TYPE);
        marketDocument.setSenderMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, RTE_EIC));
        marketDocument.setSenderMarketParticipantMarketRoleType(AO4_SENDER_ROLE);
        marketDocument.setReceiverMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, CORESO_EIC));
        marketDocument.setReceiverMarketParticipantMarketRoleType(A36_RECEIVER_ROLE);
        marketDocument.setCreatedDateTime(createXMLGregorianCalendarNow());
        OffsetDateTime intervalStart = networkTime.minusMinutes(30);
        OffsetDateTime intervalEnd = networkTime.plusMinutes(30);
        marketDocument.setTimePeriodTimeInterval(SweCneUtil.createEsmpDateTimeInterval(intervalStart, intervalEnd));
    }

    // creates and adds the TimeSeries to the CNE
    private void addTimeSeriesToCne(OffsetDateTime offsetDateTime) {
        try {
            SeriesPeriod period = newPeriod(offsetDateTime, SIXTY_MINUTES_DURATION, newPoint(1));
            marketDocument.getTimeSeries().add(newTimeSeries(B54_BUSINESS_TYPE_TS, A01_CURVE_TYPE, period));
        } catch (DatatypeConfigurationException e) {
            throw new FaraoException("Failure in TimeSeries creation");
        }
    }

    // Creates and fills all ConstraintSeries
    private void createAllConstraintSeries(Point point) {
        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();
        constraintSeriesList.addAll(new SweCneCnecsCreator(cneHelper).generate());
        constraintSeriesList.addAll(new SweCneRemedialActionsCreator(cneHelper, constraintSeriesList).generate());
        point.getConstraintSeries().addAll(constraintSeriesList);
    }
}
