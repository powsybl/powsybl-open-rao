/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.refprog.refprog_xml_importer;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.refprog.reference_program.ReferenceExchangeData;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.util.EICode;
import com.powsybl.iidm.network.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RefProg xml file importer
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class RefProgImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefProgImporter.class);

    private RefProgImporter() {

    }

    public static ReferenceProgram importRefProg(InputStream inputStream, OffsetDateTime dateTime) {
        PublicationDocument document = importXmlDocument(inputStream);
        if (!isValidDocumentInterval(document, dateTime)) {
            LOGGER.error("RefProg file is not valid for this date {}", dateTime);
            throw new FaraoException("RefProg file is not valid for this date " + dateTime);
        }
        List<ReferenceExchangeData> exchangeDataList = new ArrayList<>();
        document.getPublicationTimeSeries().forEach(timeSeries -> {
            Country outArea = null;
            Country inArea = null;
            try {
                outArea = (new EICode(timeSeries.getOutArea().getV())).getCountry();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("EIC code {} is not mapped to a country. The flow from this area will not be saved.", timeSeries.getOutArea().getV());
            }
            try {
                inArea = (new EICode(timeSeries.getInArea().getV())).getCountry();
            } catch (IllegalArgumentException e) {
                LOGGER.warn("EIC code {} is not mapped to a country. The flow to this area will not be saved.", timeSeries.getOutArea().getV());
            }
            if (outArea != null || inArea != null) {
                double flow = getFlow(dateTime, timeSeries);
                exchangeDataList.add(new ReferenceExchangeData(outArea, inArea, flow));
            }
        });
        LOGGER.info("RefProg file was imported");
        return new ReferenceProgram(exchangeDataList);
    }

    public static ReferenceProgram importRefProg(String inputPath, OffsetDateTime dateTime) {
        try (InputStream inputStream = new FileInputStream(inputPath)) {
            return importRefProg(inputStream, dateTime);
        } catch (IOException e) {
            throw new FaraoException(e);
        }
    }

    private static PublicationDocument importXmlDocument(InputStream inputStream) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDocument.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (PublicationDocument) jaxbUnmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new FaraoException(e);
        }
    }

    private static boolean isValidDocumentInterval(PublicationDocument document, OffsetDateTime dateTime) {
        if (document.getPublicationTimeInterval() != null) {
            String interval = document.getPublicationTimeInterval().getV();
            int sepPosition = interval.indexOf("/");
            OffsetDateTime startDateTime = OffsetDateTime.parse(interval.substring(0, sepPosition), DateTimeFormatter.ISO_DATE_TIME);
            OffsetDateTime endDateTime = OffsetDateTime.parse(interval.substring(sepPosition + 1), DateTimeFormatter.ISO_DATE_TIME);
            return !dateTime.isBefore(startDateTime) && dateTime.isBefore(endDateTime);
        } else {
            LOGGER.error("Cannot import RefProg file because its publication time interval is unknown");
            throw new FaraoException("Cannot import RefProg file because its publication time interval is unknown");
        }
    }

    private static boolean isValidPeriodInterval(OffsetDateTime timeSeriesStart, Duration resolution, PublicationDocument.PublicationTimeSeries.Period.Interval interval, OffsetDateTime dateTime) {
        OffsetDateTime startDateTime = timeSeriesStart.plus(resolution.multipliedBy(interval.getPos().getV().intValue() - 1));
        OffsetDateTime endDateTime = startDateTime.plus(resolution);
        return !dateTime.isBefore(startDateTime) && dateTime.isBefore(endDateTime);
    }

    private static double getFlow(OffsetDateTime dateTime, PublicationDocument.PublicationTimeSeries timeSeries) {
        String timeSeriesInterval = timeSeries.getPeriod().getTimeInterval().getV();
        OffsetDateTime timeSeriesStart = OffsetDateTime.parse(timeSeriesInterval.substring(0, timeSeriesInterval.indexOf("/")), DateTimeFormatter.ISO_DATE_TIME);
        Duration resolution = Duration.parse(timeSeries.getPeriod().getResolution().getV());
        List<PublicationDocument.PublicationTimeSeries.Period.Interval> validIntervals = timeSeries.getPeriod().getInterval().stream().filter(interval -> isValidPeriodInterval(timeSeriesStart, resolution, interval, dateTime)).collect(Collectors.toList());
        double flow = 0;
        if (validIntervals.isEmpty()) {
            String outArea = timeSeries.getOutArea().getV();
            String inArea = timeSeries.getInArea().getV();
            LOGGER.warn("Flow value between {} and {} is not found for this date {}", outArea, inArea, dateTime);
        } else {
            PublicationDocument.PublicationTimeSeries.Period.Interval validInterval = validIntervals.get(0);
            flow = validInterval.getQty().getV().doubleValue();
        }
        return flow;
    }

}
