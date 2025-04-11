/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.refprog.refprogxmlimporter;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
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
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * RefProg xml file importer
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class InterTemporalRefProg {
    private InterTemporalRefProg() {
    }

    private Map<Country, Map<Country, Double>> sumAllTieLines(Network networkWithPras) {
        // For each networkWithPra, compute exchange per zone on all tie lines
        // TODO Compute loadflow => get raoParameters
        Map<Country, Map<Country, Double>> exchangeValues = new HashMap<>();
        networkWithPras.getTieLines().forEach(tieLine -> {
            Country country1 = tieLine.getTerminal1().getVoltageLevel().getSubstation().get().getCountry().get();
            Country country2 = tieLine.getTerminal2().getVoltageLevel().getSubstation().get().getCountry().get();
            Double value = tieLine.getTerminal1().getP(); // DC => flux(terminal2) = - flux(terminal1)
            exchangeValues.putIfAbsent(country1, new HashMap<>());
            exchangeValues.get(country1).put(country2, exchangeValues.get(country1).getOrDefault(country2, 0.0) + value);
            exchangeValues.putIfAbsent(country2, new HashMap<>());
            exchangeValues.get(country2).put(country1, exchangeValues.get(country2).getOrDefault(country1, 0.0) - value);
        });
        return exchangeValues;
    }

    public ReferenceProgram importRefProg(InputStream inputStream, TemporalData<Network> networkWithPras) {
        PublicationDocument document = importXmlDocument(inputStream);
        List<ReferenceExchangeData> exchangeDataList = new ArrayList<>();
        document.getPublicationTimeSeries().forEach(timeSeries -> {
            String outAreaValue = timeSeries.getOutArea().getV();
            EICode outArea = new EICode(outAreaValue);
            String inAreaValue = timeSeries.getInArea().getV();
            EICode inArea = new EICode(inAreaValue);
            double flow = setFlow(timeSeries);
            exchangeDataList.add(new ReferenceExchangeData(outArea, inArea, flow));
        });
        ReferenceProgram refProg = new ReferenceProgram(exchangeDataList);

        networkWithPras.getDataPerTimestamp().forEach(((offsetDateTime, network) -> {
            Map<Country, Map<Country, Double>> map = sumAllTieLines(network);
            refProg.get().forEach(referenceExchangeData -> {
                if referenceExchangeData.get
            })
        }));

    }

    public static ReferenceProgram importRefProg(Path inputPath) {
        try (InputStream inputStream = new FileInputStream(inputPath.toFile())) {
            return importRefProg(inputStream);
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


    private static boolean isValidPeriodInterval(OffsetDateTime timeSeriesStart, Duration resolution, IntervalType interval, OffsetDateTime dateTime) {
        OffsetDateTime startDateTime = timeSeriesStart.plus(resolution.multipliedBy(interval.getPos().getV() - 1L));
        OffsetDateTime endDateTime = startDateTime.plus(resolution);
        return !dateTime.isBefore(startDateTime) && dateTime.isBefore(endDateTime);
    }

    private static double setFlow(PublicationTimeSeriesType timeSeries) {
        String timeSeriesInterval = timeSeries.getPeriod().get(0).getTimeInterval().getV();
        OffsetDateTime timeSeriesStart = OffsetDateTime.parse(timeSeriesInterval.substring(0, timeSeriesInterval.indexOf("/")), DateTimeFormatter.ISO_DATE_TIME);
        Duration resolution = Duration.parse(timeSeries.getPeriod().get(0).getResolution().getV().toString());
        List<IntervalType> validIntervals = timeSeries.getPeriod().get(0).getInterval().stream().toList();
        double flow = 0;
        if (validIntervals.isEmpty()) {
            String outArea = timeSeries.getOutArea().getV();
            String inArea = timeSeries.getInArea().getV();
        } else {
            IntervalType validInterval = validIntervals.get(0);
            flow = validInterval.getQty().getV().doubleValue();
        }
        return flow;
    }

}
