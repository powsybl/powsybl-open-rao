/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.refprog.refprogxmlimporter;

import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * RefProg xml file importer
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class InterTemporalRefProg {

    private static final List<String> DE_TSOS = List.of("D2", "D4", "D7", "D8");

    private InterTemporalRefProg() {
    }

    public static void updateRefProg(InputStream inputStream, TemporalData<Map<String, Double>> netRedispatchingPerCountryTemporalData, Map<String, Map<String, Map<String, Double>>> becValues, String outputPath) {
        // Load initial RefProg
        PublicationDocument document = importXmlDocument(inputStream);

        document.getPublicationTimeSeries().forEach(pubTS -> {
            String inArea;
            String outArea;
            try {
                inArea = new CountryEICode(pubTS.getInArea().getV()).getCountry().toString();
                outArea = new CountryEICode(pubTS.getOutArea().getV()).getCountry().toString();
            } catch (IllegalArgumentException e) {
                //BUSINESS_WARNS.warn("cannot find EICode for country {} or {}.", pubTS.getInArea(), pubTS.getOutArea());
                return;
            }

            int i = 0;
            for (Map<String, Double> netRedispatchingPerCountry : netRedispatchingPerCountryTemporalData.getDataPerTimestamp().values()) {
                IntervalType interval = pubTS.getPeriod().get(0).getInterval().get(i);
                final Double[] qty = {interval.getQty().getV().doubleValue()};

                netRedispatchingPerCountry.forEach((country, netRD) -> {
                    if (becValues.containsKey(inArea) && becValues.get(inArea).containsKey(outArea)) {
                        qty[0] = qty[0] + becValues.get(inArea).get(outArea).get(country) * netRD;
                    }
                });
                interval.getQty().setV(BigDecimal.valueOf(Math.round(qty[0])));
                i++;
            }
        });

        exportXmlDocument(document, outputPath);
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
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(publicationDocument, new FileOutputStream(outputPath));
        } catch (FileNotFoundException e) {
            throw new OpenRaoException(e);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}

