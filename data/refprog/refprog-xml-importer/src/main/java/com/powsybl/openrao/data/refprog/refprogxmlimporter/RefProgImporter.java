/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.refprog.refprogxmlimporter;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceExchangeData;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RefProg xml file importer
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class RefProgImporter {
    private RefProgImporter() {
    }

    public static ReferenceProgram importRefProg(InputStream inputStream, OffsetDateTime dateTime) {
        return importRefProg(inputStream, dateTime, ReportNode.NO_OP);
    }

    public static ReferenceProgram importRefProg(InputStream inputStream, OffsetDateTime dateTime, ReportNode reportNode) {
        PublicationDocument document = importXmlDocument(inputStream);
        if (!isValidDocumentInterval(document, dateTime, reportNode)) {
            RefProgReports.reportRefprogInvalidForDate(reportNode, dateTime);
            throw new OpenRaoException("RefProg file is not valid for this date " + dateTime);
        }
        List<ReferenceExchangeData> exchangeDataList = new ArrayList<>();
        document.getPublicationTimeSeries().forEach(timeSeries -> {
            String outAreaValue = timeSeries.getOutArea().getV();
            EICode outArea = new EICode(outAreaValue);
            String inAreaValue = timeSeries.getInArea().getV();
            EICode inArea = new EICode(inAreaValue);
            double flow = getFlow(dateTime, timeSeries, reportNode);
            exchangeDataList.add(new ReferenceExchangeData(outArea, inArea, flow));
        });
        RefProgReports.reportRefprogFileImported(reportNode);
        return new ReferenceProgram(exchangeDataList);
    }

    public static ReferenceProgram importRefProg(Path inputPath, OffsetDateTime dateTime) {
        return importRefProg(inputPath, dateTime, ReportNode.NO_OP);
    }

    public static ReferenceProgram importRefProg(Path inputPath, OffsetDateTime dateTime, ReportNode reportNode) {
        try (InputStream inputStream = new FileInputStream(inputPath.toFile())) {
            return importRefProg(inputStream, dateTime, reportNode);
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

    private static boolean isValidDocumentInterval(PublicationDocument document, OffsetDateTime dateTime, ReportNode reportNode) {
        if (document.getPublicationTimeInterval() != null) {
            String interval = document.getPublicationTimeInterval().getV();
            int sepPosition = interval.indexOf("/");
            OffsetDateTime startDateTime = OffsetDateTime.parse(interval.substring(0, sepPosition), DateTimeFormatter.ISO_DATE_TIME);
            OffsetDateTime endDateTime = OffsetDateTime.parse(interval.substring(sepPosition + 1), DateTimeFormatter.ISO_DATE_TIME);
            return !dateTime.isBefore(startDateTime) && dateTime.isBefore(endDateTime);
        } else {
            RefProgReports.reportRefprogImportFailedUnknownTimeInterval(reportNode);
            throw new OpenRaoException("Cannot import RefProg file because its publication time interval is unknown");
        }
    }

    private static boolean isValidPeriodInterval(OffsetDateTime timeSeriesStart, Duration resolution, IntervalType interval, OffsetDateTime dateTime) {
        OffsetDateTime startDateTime = timeSeriesStart.plus(resolution.multipliedBy(interval.getPos().getV() - 1L));
        OffsetDateTime endDateTime = startDateTime.plus(resolution);
        return !dateTime.isBefore(startDateTime) && dateTime.isBefore(endDateTime);
    }

    private static double getFlow(OffsetDateTime dateTime, PublicationTimeSeriesType timeSeries, ReportNode reportNode) {
        String timeSeriesInterval = timeSeries.getPeriod().get(0).getTimeInterval().getV();
        OffsetDateTime timeSeriesStart = OffsetDateTime.parse(timeSeriesInterval.substring(0, timeSeriesInterval.indexOf("/")), DateTimeFormatter.ISO_DATE_TIME);
        Duration resolution = Duration.parse(timeSeries.getPeriod().get(0).getResolution().getV().toString());
        List<IntervalType> validIntervals = timeSeries.getPeriod().get(0).getInterval().stream().filter(interval -> isValidPeriodInterval(timeSeriesStart, resolution, interval, dateTime)).toList();
        double flow = 0;
        if (validIntervals.isEmpty()) {
            String outArea = timeSeries.getOutArea().getV();
            String inArea = timeSeries.getInArea().getV();
            RefProgReports.reportRefprogFlowNotFoundForDate(reportNode, outArea, inArea, dateTime);
        } else {
            IntervalType validInterval = validIntervals.get(0);
            flow = validInterval.getQty().getV().doubleValue();
        }
        return flow;
    }

}
