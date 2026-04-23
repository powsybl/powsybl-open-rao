/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.xml;

import com.powsybl.openrao.virtualhubs.BorderDirection;
import com.powsybl.openrao.virtualhubs.HvdcConverter;
import com.powsybl.openrao.virtualhubs.HvdcLine;
import com.powsybl.openrao.virtualhubs.InternalHvdc;
import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.xsd.BorderDirections;
import com.powsybl.openrao.virtualhubs.xml.xsd.Configuration;
import com.powsybl.openrao.virtualhubs.xml.xsd.ConverterListType;
import com.powsybl.openrao.virtualhubs.xml.xsd.HVDCType;
import com.powsybl.openrao.virtualhubs.xml.xsd.InternalHVDCs;
import com.powsybl.openrao.virtualhubs.xml.xsd.LineListType;
import com.powsybl.openrao.virtualhubs.xml.xsd.MarketAreas;
import com.powsybl.openrao.virtualhubs.xml.xsd.PoleType;
import com.powsybl.openrao.virtualhubs.xml.xsd.VirtualHubs;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class VirtualHubsConfigurationImporter {
    public VirtualHubsConfiguration importConfiguration(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Cannot import configuration from null input stream");

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
            final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            final Configuration unmarshalled = (Configuration) jaxbUnmarshaller.unmarshal(inputStream);

            final Map<String, MarketArea> marketAreasMap = new TreeMap<>();
            final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();

            final MarketAreas marketAreas = unmarshalled.getMarketAreas();
            if (marketAreas != null) {
                importMarketAreas(marketAreas.getMarketArea(), marketAreasMap, configuration);
            }

            final VirtualHubs virtualHubs = unmarshalled.getVirtualHubs();
            if (virtualHubs != null) {
                importVirtualHubs(virtualHubs.getVirtualHub(), marketAreasMap, configuration);
            }

            final BorderDirections borderDirections = unmarshalled.getBorderDirections();
            if (borderDirections != null) {
                importBorderDirections(borderDirections.getBorderDirection(), configuration);
            }

            final InternalHVDCs internalHvdcs = unmarshalled.getInternalHVDCs();
            if (internalHvdcs != null) {
                importInternalHvdcs(internalHvdcs.getHVDC(), configuration);
            }

            return configuration;
        } catch (JAXBException e) {
            throw new VirtualHubsConfigProcessingException(e);
        }
    }

    private static void importMarketAreas(final List<com.powsybl.openrao.virtualhubs.xml.xsd.MarketArea> rawMarketAreas, final Map<String, MarketArea> marketAreasMap, final VirtualHubsConfiguration configuration) {
        for (com.powsybl.openrao.virtualhubs.xml.xsd.MarketArea rawMarketArea : rawMarketAreas) {
            final String code = rawMarketArea.getCode();
            final String eic = rawMarketArea.getEic();
            final boolean isMcParticipant = rawMarketArea.isMCParticipant();
            final boolean isAhc = Optional.ofNullable(rawMarketArea.isAHC()).orElse(false);
            final MarketArea marketArea = new MarketArea(code, eic, isMcParticipant, isAhc);
            marketAreasMap.put(code, marketArea);
            configuration.addMarketArea(marketArea);
        }
    }

    private static void importVirtualHubs(final List<com.powsybl.openrao.virtualhubs.xml.xsd.VirtualHub> rawVirtualHubs, final Map<String, MarketArea> marketAreasMap, final VirtualHubsConfiguration configuration) {
        for (com.powsybl.openrao.virtualhubs.xml.xsd.VirtualHub rawVirtualHub : rawVirtualHubs) {
            final String code = rawVirtualHub.getCode();
            final String eic = rawVirtualHub.getEic();
            final boolean isMcParticipant = rawVirtualHub.isMCParticipant();
            final boolean isAhc = Optional.ofNullable(rawVirtualHub.isAHC()).orElse(false);
            final String nodeName = rawVirtualHub.getNodeName();
            final MarketArea marketArea = marketAreasMap.get(rawVirtualHub.getRelatedMA());
            final String oppositeHub = rawVirtualHub.getOppositeHub();
            configuration.addVirtualHub(new VirtualHub(code, eic, isMcParticipant, isAhc, nodeName, marketArea, oppositeHub));
        }
    }

    private static void importBorderDirections(final List<com.powsybl.openrao.virtualhubs.xml.xsd.BorderDirection> rawBorderDirections, final VirtualHubsConfiguration configuration) {
        for (com.powsybl.openrao.virtualhubs.xml.xsd.BorderDirection rawBorderDirection : rawBorderDirections) {
            final String from = rawBorderDirection.getFrom();
            final String to = rawBorderDirection.getTo();
            final boolean isAhc = Optional.ofNullable(rawBorderDirection.isAHC()).orElse(false);
            configuration.addBorderDirection(new BorderDirection(from, to, isAhc));
        }
    }

    private static void importInternalHvdcs(final List<HVDCType> internalHvdcs, final VirtualHubsConfiguration configuration) {
        internalHvdcs.stream()
            .map(HVDCType::getPole)
            .flatMap(Collection::stream)
            .forEach(pole -> importHvdcPoleConfiguration(pole, configuration));
    }

    private static void importHvdcPoleConfiguration(final PoleType pole, final VirtualHubsConfiguration configuration) {
        if (pole == null) {
            return;
        }

        final List<HvdcConverter> hvdcConverters = new ArrayList<>();
        final List<HvdcLine> hvdcLines = new ArrayList<>();

        // HVDC -> pole -> converterList -> converter (node, station)
        final List<Serializable> converters = pole.getConverterList().getContent();
        for (Serializable serializableConverter : converters) {
            if (serializableConverter instanceof JAXBElement<?> element && element.getValue() instanceof ConverterListType.Converter converter) {
                hvdcConverters.add(new HvdcConverter(converter.getNode(), converter.getStation()));
            }
        }
        // HVDC -> pole -> lineList -> line (from, to)
        final List<Serializable> lineLists = pole.getLineList().getContent();
        for (Serializable serializableConverter : lineLists) {
            if (serializableConverter instanceof JAXBElement<?> element && element.getValue() instanceof LineListType.Line hvdcLine) {
                hvdcLines.add(new HvdcLine(hvdcLine.getFrom(), hvdcLine.getTo()));
            }
        }

        configuration.addInternalHvdc(new InternalHvdc(hvdcConverters, hvdcLines));
    }
}
