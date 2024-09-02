/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.cneexportercommons.CneExporterParameters;
import com.powsybl.openrao.data.cneexportercommons.CneUtil;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracio.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.swecneexporter.xsd.*;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.iidm.network.Network;

import javax.xml.datatype.DatatypeConfigurationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import static com.powsybl.openrao.data.cneexportercommons.CneConstants.*;
import static com.powsybl.openrao.data.cneexportercommons.CneUtil.createXMLGregorianCalendarNow;
import static com.powsybl.openrao.data.swecneexporter.SweCneClassCreator.*;
import static com.powsybl.openrao.data.swecneexporter.SweCneUtil.*;

/**
 * Fills the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SweCne {
    private final CriticalNetworkElementMarketDocument marketDocument;
    private final SweCneHelper sweCneHelper;
    private final CimCracCreationContext cracCreationContext;

    public SweCne(Crac crac, Network network, CimCracCreationContext cracCreationContext, RaoResult raoResult, RaoParameters raoParameters, CneExporterParameters exporterParameters) {
        marketDocument = new CriticalNetworkElementMarketDocument();
        sweCneHelper = new SweCneHelper(crac, network, raoResult, raoParameters, exporterParameters);
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
        // the usage of a timestamp in CimCracCreator is mandatory so it shouldn't be an issue
        if (Objects.isNull(cracCreationContext.getTimeStamp())) {
            throw new OpenRaoException("Cannot export CNE file if the CRAC has no timestamp");
        }

        OffsetDateTime offsetDateTime = cracCreationContext.getTimeStamp().withMinute(0);
        fillHeader(sweCneHelper.getNetwork().getCaseDate().toInstant().atOffset(ZoneOffset.UTC));
        addTimeSeriesToCne(offsetDateTime);
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        // fill CNE
        createAllConstraintSeries(point);

        // add reason
        addReason(point);
    }

    // fills the header of the CNE
    private void fillHeader(OffsetDateTime offsetDateTime) {
        marketDocument.setMRID(sweCneHelper.getExporterParameters().getDocumentId());
        marketDocument.setRevisionNumber(String.valueOf(sweCneHelper.getExporterParameters().getRevisionNumber()));
        marketDocument.setType(CNE_TYPE);
        marketDocument.setProcessProcessType(sweCneHelper.getExporterParameters().getProcessType().getCode());
        marketDocument.setSenderMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, sweCneHelper.getExporterParameters().getSenderId()));
        marketDocument.setSenderMarketParticipantMarketRoleType(sweCneHelper.getExporterParameters().getSenderRole().getCode());
        marketDocument.setReceiverMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, sweCneHelper.getExporterParameters().getReceiverId()));
        marketDocument.setReceiverMarketParticipantMarketRoleType(sweCneHelper.getExporterParameters().getReceiverRole().getCode());
        marketDocument.setCreatedDateTime(createXMLGregorianCalendarNow());
        marketDocument.setTimePeriodTimeInterval(SweCneUtil.createEsmpDateTimeInterval(offsetDateTime));
    }

    // creates and adds the TimeSeries to the CNE
    private void addTimeSeriesToCne(OffsetDateTime offsetDateTime) {
        try {
            SeriesPeriod period = newPeriod(offsetDateTime, SIXTY_MINUTES_DURATION, newPoint(1));
            marketDocument.getTimeSeries().add(newTimeSeries(B54_BUSINESS_TYPE_TS, A01_CURVE_TYPE, period));
        } catch (DatatypeConfigurationException e) {
            throw new OpenRaoException("Failure in TimeSeries creation");
        }
    }

    // Creates and fills all ConstraintSeries
    private void createAllConstraintSeries(Point point) {
        List<ConstraintSeries> constraintSeriesList = new SweConstraintSeriesCreator(sweCneHelper, cracCreationContext).generate();
        point.getConstraintSeries().addAll(constraintSeriesList);
    }

    private void addReason(Point point) {
        Reason reason = new Reason();
        RaoResult raoResult = sweCneHelper.getRaoResult();
        boolean isDivergent = sweCneHelper.isAnyContingencyInFailure() || raoResult.getComputationStatus() == ComputationStatus.FAILURE;
        boolean isUnsecure;
        try {
            isUnsecure = !raoResult.isSecure(PhysicalParameter.FLOW, PhysicalParameter.ANGLE);
        } catch (OpenRaoException e) {
            // Sometimes we run this method without running angle monitoring. In that case, simply ignore AngleCnecs
            isUnsecure = !raoResult.isSecure(PhysicalParameter.FLOW);
        }
        if (isDivergent) {
            reason.setCode(DIVERGENCE_CODE);
            reason.setText(DIVERGENCE_TEXT);
        } else if (isUnsecure) {
            reason.setCode(UNSECURE_CODE);
            reason.setText(UNSECURE_TEXT);
        } else {
            reason.setCode(SECURE_CODE);
            reason.setText(SECURE_TEXT);
        }
        point.getReason().add(reason);
    }
}
