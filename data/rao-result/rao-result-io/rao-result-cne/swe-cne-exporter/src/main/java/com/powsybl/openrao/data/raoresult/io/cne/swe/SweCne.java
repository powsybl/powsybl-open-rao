/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.swe;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.io.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.ConstraintSeries;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.CriticalNetworkElementMarketDocument;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.Point;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.Reason;
import com.powsybl.openrao.data.raoresult.io.cne.swe.xsd.SeriesPeriod;

import javax.xml.datatype.DatatypeConfigurationException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.A01_CODING_SCHEME;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.A01_CURVE_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.B54_BUSINESS_TYPE_TS;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.CNE_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.DIVERGENCE_CODE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.DIVERGENCE_TEXT;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.SECURE_CODE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.SECURE_TEXT;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.SIXTY_MINUTES_DURATION;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.UNSECURE_CODE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.UNSECURE_TEXT;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil.createXMLGregorianCalendarNow;
import static com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneClassCreator.newPeriod;
import static com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneClassCreator.newPoint;
import static com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneClassCreator.newTimeSeries;
import static com.powsybl.openrao.data.raoresult.io.cne.swe.SweCneUtil.createPartyIDString;

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

    public SweCne(CimCracCreationContext cracCreationContext, RaoResult raoResult, Properties properties) {
        marketDocument = new CriticalNetworkElementMarketDocument();
        sweCneHelper = new SweCneHelper(cracCreationContext.getCrac(), raoResult, properties);
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
        fillHeader(cracCreationContext.getNetworkCaseDate());
        addTimeSeriesToCne(offsetDateTime);
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        // fill CNE
        createAllConstraintSeries(point);

        // add reason
        addReason(point);
    }

    // fills the header of the CNE
    private void fillHeader(OffsetDateTime offsetDateTime) {
        marketDocument.setMRID(sweCneHelper.getDocumentId());
        marketDocument.setRevisionNumber(String.valueOf(sweCneHelper.getRevisionNumber()));
        marketDocument.setType(CNE_TYPE);
        marketDocument.setProcessProcessType(sweCneHelper.getProcessType());
        marketDocument.setSenderMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, sweCneHelper.getSenderId()));
        marketDocument.setSenderMarketParticipantMarketRoleType(sweCneHelper.getSenderRole());
        marketDocument.setReceiverMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, sweCneHelper.getReceiverId()));
        marketDocument.setReceiverMarketParticipantMarketRoleType(sweCneHelper.getReceiverRole());
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
