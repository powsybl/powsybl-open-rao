/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.cne_exporter_commons.CneUtil;
import com.farao_community.farao.data.core_cne_exporter.xsd.ConstraintSeries;
import com.farao_community.farao.data.core_cne_exporter.xsd.CriticalNetworkElementMarketDocument;
import com.farao_community.farao.data.core_cne_exporter.xsd.Point;
import com.farao_community.farao.data.core_cne_exporter.xsd.SeriesPeriod;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.std_creation_context.StandardCracCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;

import javax.xml.datatype.DatatypeConfigurationException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.*;
import static com.farao_community.farao.data.cne_exporter_commons.CneUtil.createXMLGregorianCalendarNow;
import static com.farao_community.farao.data.core_cne_exporter.CoreCneClassCreator.*;
import static com.farao_community.farao.data.core_cne_exporter.CoreCneUtil.*;

/**
 * Fills the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CoreCne {
    private CriticalNetworkElementMarketDocument marketDocument;
    private CneHelper cneHelper;
    private StandardCracCreationContext cracCreationContext;

    public CoreCne(Crac crac, Network network, StandardCracCreationContext cracCreationContext, RaoResult raoResult, RaoParameters raoParameters, CneExporterParameters exporterParameters) {
        marketDocument = new CriticalNetworkElementMarketDocument();
        cneHelper = new CneHelper(crac, network, raoResult, raoParameters, exporterParameters);
        this.cracCreationContext = cracCreationContext;
    }

    public CriticalNetworkElementMarketDocument getMarketDocument() {
        return marketDocument;
    }

    // Main method
    public void generate() {
        // Reset unique IDs
        CneUtil.initUniqueIds();

        // this will crash if cneHelper.getCracCreationContext().getTimeStamp() is null
        // the usage of a timestamp in FbConstraintCracCreator is mandatory so it shouldn't be an issue
        if (Objects.isNull(cracCreationContext.getTimeStamp())) {
            throw new FaraoException("Cannot export CNE file if the CRAC has no timestamp");
        }

        OffsetDateTime offsetDateTime = cracCreationContext.getTimeStamp().withMinute(0);
        fillHeader();
        addTimeSeriesToCne(offsetDateTime);
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        // fill CNE
        createAllConstraintSeries(point);
    }

    // fills the header of the CNE
    private void fillHeader() {
        marketDocument.setMRID(cneHelper.getExporterParameters().getDocumentId());
        marketDocument.setRevisionNumber(String.valueOf(cneHelper.getExporterParameters().getRevisionNumber()));
        marketDocument.setType(CNE_TYPE);
        marketDocument.setProcessProcessType(cneHelper.getExporterParameters().getProcessType().getCode());
        marketDocument.setSenderMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, cneHelper.getExporterParameters().getSenderId()));
        marketDocument.setSenderMarketParticipantMarketRoleType(cneHelper.getExporterParameters().getSenderRole().getCode());
        marketDocument.setReceiverMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, cneHelper.getExporterParameters().getReceiverId()));
        marketDocument.setReceiverMarketParticipantMarketRoleType(cneHelper.getExporterParameters().getReceiverRole().getCode());
        marketDocument.setCreatedDateTime(createXMLGregorianCalendarNow());
        marketDocument.setTimePeriodTimeInterval(createEsmpDateTimeIntervalForWholeDay(cneHelper.getExporterParameters().getTimeInterval()));
        marketDocument.setDomainMRID(createAreaIDString(A01_CODING_SCHEME, cneHelper.getExporterParameters().getDomainId()));
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
        constraintSeriesList.addAll(new CoreCneCnecsCreator(cneHelper, cracCreationContext).generate());
        constraintSeriesList.addAll(new CoreCneRemedialActionsCreator(cneHelper, cracCreationContext, constraintSeriesList).generate());
        point.getConstraintSeries().addAll(constraintSeriesList);
    }
}
