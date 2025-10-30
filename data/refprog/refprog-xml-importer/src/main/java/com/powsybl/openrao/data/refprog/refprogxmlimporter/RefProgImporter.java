/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.refprog.refprogxmlimporter;

import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceExchangeData;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * RefProg xml file importer
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class RefProgImporter {
    private RefProgImporter() {
    }

    public static ReferenceProgram importRefProg(InputStream inputStream, OffsetDateTime dateTime) {
        PublicationDocument document = importXmlDocument(inputStream);
        if (!isValidDocumentInterval(document, dateTime)) {
            BUSINESS_LOGS.error("RefProg file is not valid for this date {}", dateTime);
            throw new OpenRaoException("RefProg file is not valid for this date " + dateTime);
        }
        List<ReferenceExchangeData> exchangeDataList = new ArrayList<>();
        document.getPublicationTimeSeries().forEach(timeSeries -> {
            String outAreaValue = timeSeries.getOutArea().getV();
            EICode outArea = new EICode(outAreaValue);
            String inAreaValue = timeSeries.getInArea().getV();
            EICode inArea = new EICode(inAreaValue);
            double flow = getFlow(dateTime, timeSeries);
            exchangeDataList.add(new ReferenceExchangeData(outArea, inArea, flow));
        });
        TECHNICAL_LOGS.info("RefProg file was imported");
        return new ReferenceProgram(exchangeDataList);
    }

    public static ReferenceProgram importRefProg(Path inputPath, OffsetDateTime dateTime) {
        try (InputStream inputStream = new FileInputStream(inputPath.toFile())) {
            return importRefProg(inputStream, dateTime);
        } catch (IOException e) {
            throw new OpenRaoException(e);
        }
    }

    private static PublicationDocument importXmlDocument(InputStream inputStream) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDocument.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (PublicationDocument) jaxbUnmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new OpenRaoException(e);
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
            BUSINESS_LOGS.error("Cannot import RefProg file because its publication time interval is unknown");
            throw new OpenRaoException("Cannot import RefProg file because its publication time interval is unknown");
        }
    }

    private static boolean isValidPeriodInterval(OffsetDateTime timeSeriesStart, Duration resolution, IntervalType interval, OffsetDateTime dateTime) {
        OffsetDateTime startDateTime = timeSeriesStart.plus(resolution.multipliedBy(interval.getPos().getV() - 1L));
        OffsetDateTime endDateTime = startDateTime.plus(resolution);
        return !dateTime.isBefore(startDateTime) && dateTime.isBefore(endDateTime);
    }

    private static double getFlow(OffsetDateTime dateTime, PublicationTimeSeriesType timeSeries) {
        String timeSeriesInterval = timeSeries.getPeriod().get(0).getTimeInterval().getV();
        OffsetDateTime timeSeriesStart = OffsetDateTime.parse(timeSeriesInterval.substring(0, timeSeriesInterval.indexOf("/")), DateTimeFormatter.ISO_DATE_TIME);
        Duration resolution = Duration.parse(timeSeries.getPeriod().get(0).getResolution().getV().toString());
        List<IntervalType> validIntervals = timeSeries.getPeriod().get(0).getInterval().stream().filter(interval -> isValidPeriodInterval(timeSeriesStart, resolution, interval, dateTime)).toList();
        double flow = 0;
        if (validIntervals.isEmpty()) {
            String outArea = timeSeries.getOutArea().getV();
            String inArea = timeSeries.getInArea().getV();
            BUSINESS_WARNS.warn("Flow value between {} and {} is not found for this date {}", outArea, inArea, dateTime);
        } else {
            IntervalType validInterval = validIntervals.get(0);
            flow = validInterval.getQty().getV().doubleValue();
        }
        return flow;
    }

}
