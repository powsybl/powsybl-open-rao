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
import com.powsybl.iidm.network.Substation;
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

    private static Map<EICode, Map<EICode, Double>> sumAllTieLines(Network networkWithPras) {
        // For each networkWithPra, compute exchange per zone on all tie lines
        Map<EICode, Map<EICode, Double>> exchangeValues = new HashMap<>();
        networkWithPras.getTieLines().forEach(tieLine -> {
            Country country1;
            Country country2;
            Optional<Substation> substation1 = tieLine.getTerminal1().getVoltageLevel().getSubstation();
            if (substation1.isEmpty()) {
                BUSINESS_WARNS.warn("Substation not found for tieLine {} terminal 1 voltage level {}.", tieLine, tieLine.getTerminal1().getVoltageLevel());
                return;
            } else {
                country1 = substation1.get().getCountry().get();
            }

            Optional<Substation> substation2 = tieLine.getTerminal2().getVoltageLevel().getSubstation();
            if (substation2.isEmpty()) {
                BUSINESS_WARNS.warn("Substation not found for tieLine {} terminal 2 voltage level {}.", tieLine, tieLine.getTerminal2().getVoltageLevel());
                return;
            } else {
                country2 = substation2.get().getCountry().get();
            }
            EICode eiCode1;
            EICode eiCode2;

            try {
                eiCode1 = new EICode(country1);
            } catch (IllegalArgumentException e) {
                BUSINESS_WARNS.warn("cannot find EICode for country {}.", country1);
                return;
            }
            try {
                eiCode2 = new EICode(country2);
            } catch (IllegalArgumentException e) {
                BUSINESS_WARNS.warn("cannot find EICode for country {}.", country2);
                return;
            }
            Double value = tieLine.getTerminal1().getP(); // DC => flux(terminal2) = - flux(terminal1)
            if (Double.isNaN(value) && tieLine.getTerminal1().isConnected() && tieLine.getTerminal2().isConnected()) {
                BUSINESS_WARNS.warn("NaN for tieLine {} terminal 1 getP.", tieLine);
                return;
            }
            exchangeValues.putIfAbsent(eiCode1, new HashMap<>());
            exchangeValues.get(eiCode1).put(eiCode2, exchangeValues.get(eiCode1).getOrDefault(eiCode2, 0.0) + value);
            exchangeValues.putIfAbsent(eiCode2, new HashMap<>());
            exchangeValues.get(eiCode2).put(eiCode1, exchangeValues.get(eiCode2).getOrDefault(eiCode1, 0.0) - value);
        });
        return exchangeValues;
    }

    public static void updateRefProg(InputStream inputStream, TemporalData<Network> networkWithPras, RaoParameters raoParameters, String outputPath) {
        Map<Integer, Map<EICode, Map<EICode, Double>>> exchangeValuesByTs = new HashMap<>();
        networkWithPras.getDataPerTimestamp().forEach(((offsetDateTime, network) -> {
            BUSINESS_WARNS.warn("**** Timestamp {} ****", offsetDateTime);
            // Compute loadflow
            String loadFlowProvider = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getLoadFlowProvider();
            LoadFlowParameters loadFlowParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters();
            LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider)
                .run(network, loadFlowParameters);
            if (loadFlowResult.isFailed()) {
                BUSINESS_WARNS.warn("LoadFlow error.");
            }
            // Compute exchange per zone
            int hour = 1 + offsetDateTime.getHour();
            exchangeValuesByTs.putIfAbsent(hour, sumAllTieLines(network));
        }));

        // Load initial RefProg
        PublicationDocument document = importXmlDocument(inputStream);
        document.getPublicationTimeSeries().forEach(timeSeries -> {
            String outAreaValue = timeSeries.getOutArea().getV();
            EICode outArea = new EICode(outAreaValue);
            String inAreaValue = timeSeries.getInArea().getV();
            EICode inArea = new EICode(inAreaValue);
            // Modify specific timeseries according to exchangeValuesByTs
            setFlow(timeSeries, inArea, outArea, exchangeValuesByTs);
        });
        exportXmlDocument(document, outputPath);
    }

    private static void setFlow(PublicationTimeSeriesType timeSeries, EICode inArea, EICode outArea, Map<Integer, Map<EICode, Map<EICode, Double>>> exchangeValuesByTs) {
        List<IntervalType> validIntervals = timeSeries.getPeriod().get(0).getInterval().stream().toList();
        double oldFlow = 0;
        double newFlow = 0;
        // TODO why check validIntervals empty
        if (validIntervals.isEmpty()) {
            BUSINESS_WARNS.warn("Valid intervals is empty");
        } else if (validIntervals.size() != 24) {
            BUSINESS_WARNS.warn("Valid intervals has wrong size ({} instead of 24)", validIntervals.size());
        } else {
            for (IntervalType validInterval : validIntervals) {
                Map<EICode, Map<EICode, Double>> exchangeValuesForSpecificTs = exchangeValuesByTs.get(validInterval.getPos().getV());
                oldFlow = validInterval.getQty().getV().doubleValue();
                if (Objects.nonNull(exchangeValuesForSpecificTs.get(inArea))) {
                    if (Objects.nonNull(exchangeValuesForSpecificTs.get(inArea).get(outArea))) {
                        newFlow = exchangeValuesForSpecificTs.get(inArea).get(outArea);
                        if (Double.isNaN(newFlow)) {
                            BUSINESS_WARNS.warn("New flow is NaN for inArea {} and outArea {} and Ts {}", inArea, outArea, validInterval.getPos().getV());
                        } else {
                            BUSINESS_WARNS.warn("New flow set from {} to {} for inArea {} and outArea {} and Ts {}", oldFlow, newFlow, inArea, outArea, validInterval.getPos().getV());
                            QuantityType newFlowQuantity = new QuantityType();
                            newFlowQuantity.setV(BigDecimal.valueOf(newFlow));
                            validInterval.setQty(newFlowQuantity);
                        }
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

