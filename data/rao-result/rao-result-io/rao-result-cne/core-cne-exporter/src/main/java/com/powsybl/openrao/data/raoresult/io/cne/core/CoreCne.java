/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.core;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.UcteCracCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneHelper;
import com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.ConstraintSeries;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.CriticalNetworkElementMarketDocument;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.Point;
import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.SeriesPeriod;

import javax.xml.datatype.DatatypeConfigurationException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.A01_CODING_SCHEME;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.A01_CURVE_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.B54_BUSINESS_TYPE_TS;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.CNE_TYPE;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneConstants.SIXTY_MINUTES_DURATION;
import static com.powsybl.openrao.data.raoresult.io.cne.commons.CneUtil.createXMLGregorianCalendarNow;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneClassCreator.newPeriod;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneClassCreator.newPoint;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneClassCreator.newTimeSeries;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneUtil.CORE_CNE_EXPORT_PROPERTIES_PREFIX;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneUtil.createAreaIDString;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneUtil.createEsmpDateTimeIntervalForWholeDay;
import static com.powsybl.openrao.data.raoresult.io.cne.core.CoreCneUtil.createPartyIDString;

/**
 * Fills the classes that constitute the CNE file structure
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CoreCne {
    private final CriticalNetworkElementMarketDocument marketDocument;
    private final CneHelper cneHelper;
    private final UcteCracCreationContext cracCreationContext;

    public CoreCne(UcteCracCreationContext cracCreationContext, RaoResult raoResult, Properties properties) {
        marketDocument = new CriticalNetworkElementMarketDocument();
        cneHelper = new CneHelper(cracCreationContext.getCrac(), raoResult, properties, CORE_CNE_EXPORT_PROPERTIES_PREFIX);
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
            throw new OpenRaoException("Cannot export CNE file if the CRAC has no timestamp");
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
        marketDocument.setMRID(cneHelper.getDocumentId());
        marketDocument.setRevisionNumber(String.valueOf(cneHelper.getRevisionNumber()));
        marketDocument.setType(CNE_TYPE);
        marketDocument.setProcessProcessType(cneHelper.getProcessType());
        marketDocument.setSenderMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, cneHelper.getSenderId()));
        marketDocument.setSenderMarketParticipantMarketRoleType(cneHelper.getSenderRole());
        marketDocument.setReceiverMarketParticipantMRID(createPartyIDString(A01_CODING_SCHEME, cneHelper.getReceiverId()));
        marketDocument.setReceiverMarketParticipantMarketRoleType(cneHelper.getReceiverRole());
        marketDocument.setCreatedDateTime(createXMLGregorianCalendarNow());
        marketDocument.setTimePeriodTimeInterval(createEsmpDateTimeIntervalForWholeDay(cneHelper.getTimeInterval()));
        marketDocument.setDomainMRID(createAreaIDString(A01_CODING_SCHEME, cneHelper.getDomainId()));
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
        List<ConstraintSeries> constraintSeriesList = new ArrayList<>();
        constraintSeriesList.addAll(new CoreCneCnecsCreator(cneHelper, cracCreationContext).generate());
        constraintSeriesList.addAll(new CoreCneRemedialActionsCreator(cneHelper, cracCreationContext, constraintSeriesList).generate());
        point.getConstraintSeries().addAll(constraintSeriesList);
    }
}
