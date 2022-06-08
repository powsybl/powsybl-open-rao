/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.cne_exporter_commons.CneExporterParameters;
import com.farao_community.farao.data.cne_exporter_commons.CneHelper;
import com.farao_community.farao.data.cne_exporter_commons.CneUtil;
import com.farao_community.farao.data.crac_creation.creator.cim.crac_creator.CimCracCreationContext;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.swe_cne_exporter.xsd.*;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;

import javax.xml.datatype.DatatypeConfigurationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.data.cne_exporter_commons.CneConstants.*;
import static com.farao_community.farao.data.cne_exporter_commons.CneUtil.createXMLGregorianCalendarNow;
import static com.farao_community.farao.data.swe_cne_exporter.SweCneClassCreator.*;
import static com.farao_community.farao.data.swe_cne_exporter.SweCneUtil.*;

/**
 * Fills the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SweCne {
    private CriticalNetworkElementMarketDocument marketDocument;
    private CneHelper cneHelper;

    public SweCne(Crac crac, Network network, CimCracCreationContext cracCreationContext, RaoResult raoResult, RaoParameters raoParameters, CneExporterParameters exporterParameters) {
        marketDocument = new CriticalNetworkElementMarketDocument();
        cneHelper = new CneHelper(crac, network, cracCreationContext, raoResult, raoParameters, exporterParameters);
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
        if (Objects.isNull(cneHelper.getCimCracCreationContext().getTimeStamp())) {
            throw new FaraoException("Cannot export CNE file if the CRAC has no timestamp");
        }

        OffsetDateTime offsetDateTime = cneHelper.getCimCracCreationContext().getTimeStamp().withMinute(0);
        fillHeader(cneHelper.getNetwork().getCaseDate().toDate().toInstant().atOffset(ZoneOffset.UTC));
        addTimeSeriesToCne(offsetDateTime);
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        // fill CNE
        createAllConstraintSeries(point);

        // add reason
        addResaon(point);
    }

    // fills the header of the CNE
    private void fillHeader(OffsetDateTime offsetDateTime) {
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
        marketDocument.setTimePeriodTimeInterval(SweCneUtil.createEsmpDateTimeInterval(offsetDateTime));
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
        List<ConstraintSeries> constraintSeriesList = new SweConstraintSeriesCreator(cneHelper).generate();
        point.getConstraintSeries().addAll(constraintSeriesList);
    }

    private void addResaon(Point point) {
        Reason reason = new Reason();
        RaoResult raoResult = cneHelper.getRaoResult();
        if (raoResult.getComputationStatus() == ComputationStatus.FAILURE) {
            reason.setCode(DIVERGENCE_CODE);
            reason.setCode(DIVERGENCE_TEXT);
        } else if (raoResult.getFunctionalCost(OptimizationState.AFTER_CRA) > 0) {
            reason.setCode(UNSECURE_CODE);
            reason.setText(UNSECURE_TEXT);
        } else {
            reason.setCode(SECURE_CODE);
            reason.setCode(SECURE_TEXT);
        }
        point.getReason().add(reason);
    }
}
