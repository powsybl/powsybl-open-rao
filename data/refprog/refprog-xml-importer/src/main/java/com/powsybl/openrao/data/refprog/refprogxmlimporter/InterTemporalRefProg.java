/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.refprog.refprogxmlimporter;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceExchangeData;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import xsd.etso_core_cmpts.QuantityType;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * RefProg xml file importer
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class InterTemporalRefProg {
    private InterTemporalRefProg() {
    }

    private static Map<Country, Map<Country, Double>> sumAllTieLines(Network networkWithPras) {
        // For each networkWithPra, compute exchange per zone on all tie lines
        Map<Country, Map<Country, Double>> exchangeValues = new HashMap<>();
        networkWithPras.getTieLines().forEach(tieLine -> {
            Country country1 = tieLine.getTerminal1().getVoltageLevel().getSubstation().get().getCountry().get();
            Country country2 = tieLine.getTerminal2().getVoltageLevel().getSubstation().get().getCountry().get();
            // TODO : handle NaN
            Double value = tieLine.getTerminal1().getP(); // DC => flux(terminal2) = - flux(terminal1)
            exchangeValues.putIfAbsent(country1, new HashMap<>());
            exchangeValues.get(country1).put(country2, exchangeValues.get(country1).getOrDefault(country2, 0.0) + value);
            exchangeValues.putIfAbsent(country2, new HashMap<>());
            exchangeValues.get(country2).put(country1, exchangeValues.get(country2).getOrDefault(country1, 0.0) - value);
        });
        return exchangeValues;
    }

    public static void updateRefProg(InputStream inputStream, TemporalData<Network> networkWithPras, RaoParameters raoParameters, String outputPath) {
        Map<Integer, Map<Country, Map<Country, Double>>> exchangeValuesByTs = new HashMap<>();
        networkWithPras.getDataPerTimestamp().forEach(((offsetDateTime, network) -> {
            // Compute loadflow
            String loadFlowProvider = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getLoadFlowProvider();
            LoadFlowParameters loadFlowParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters();
            LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider)
                .run(network, loadFlowParameters);
            // Compute exchange per zone
            int hour = 1 + offsetDateTime.getHour();
            exchangeValuesByTs.putIfAbsent(hour, sumAllTieLines(network));
        }));

        // TODO : check with a network without any PRAS that we're computing the same thing as before

        PublicationDocument document = importXmlDocument(inputStream);
        List<ReferenceExchangeData> exchangeDataList = new ArrayList<>();
        document.getPublicationTimeSeries().forEach(timeSeries -> {
            String outAreaValue = timeSeries.getOutArea().getV();
            EICode outArea = new EICode(outAreaValue);
            String inAreaValue = timeSeries.getInArea().getV();
            EICode inArea = new EICode(inAreaValue);
            setFlow(timeSeries, inArea, outArea, exchangeValuesByTs);
        });
        exportXmlDocument(document, outputPath);
    }

    private static void setFlow(PublicationTimeSeriesType timeSeries, EICode inArea, EICode outArea, Map<Integer, Map<Country, Map<Country, Double>>> exchangeValuesByTs) {
        String timeSeriesInterval = timeSeries.getPeriod().get(0).getTimeInterval().getV();
        OffsetDateTime timeSeriesStart = OffsetDateTime.parse(timeSeriesInterval.substring(0, timeSeriesInterval.indexOf("/")), DateTimeFormatter.ISO_DATE_TIME);
        Duration resolution = Duration.parse(timeSeries.getPeriod().get(0).getResolution().getV().toString());
        List<IntervalType> validIntervals = timeSeries.getPeriod().get(0).getInterval().stream().toList();
        double oldFlow = 0;
        double newFlow = 0;
        // TODO : why did we set inArea outArea here
        if (!validIntervals.isEmpty()) {
            IntervalType validInterval = validIntervals.get(0);
            Map<Country, Map<Country, Double>> exchangeValuesForSpecificTs = exchangeValuesByTs.get(validInterval.getPos().getV());
            oldFlow = validInterval.getQty().getV().doubleValue();
            // TODO : quel sens inArea -> outArea ou outArea -> inArea?
            if (Objects.nonNull(inArea.getCountry()) && Objects.nonNull(outArea.getCountry())) {
                if (Objects.nonNull(exchangeValuesForSpecificTs.get(inArea.getCountry()))) {
                    newFlow = exchangeValuesForSpecificTs.get(inArea.getCountry()).get(outArea.getCountry());
                    if (!Double.isNaN(newFlow)) {
                        QuantityType newFlowQuantity = new QuantityType();
                        newFlowQuantity.setV(BigDecimal.valueOf(newFlow));
                        validInterval.setQty(newFlowQuantity);
                    }
                }
            }
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

    private static void exportXmlDocument(PublicationDocument publicationDocument, String outputPath) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDocument.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(publicationDocument, new FileOutputStream(outputPath));
        } catch (FileNotFoundException e) {
            throw new OpenRaoException(e);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}

