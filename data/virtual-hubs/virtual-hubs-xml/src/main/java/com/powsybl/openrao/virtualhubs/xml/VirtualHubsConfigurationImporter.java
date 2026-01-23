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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationImporter {
    public VirtualHubsConfiguration importConfiguration(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Cannot import configuration from null input stream");
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(inputStream);
            final Element configurationEl = document.getDocumentElement();
            final NodeList marketAreas = configurationEl.getElementsByTagName("MarketArea");
            final NodeList virtualHubs = configurationEl.getElementsByTagName("VirtualHub");
            final NodeList borderDirections = configurationEl.getElementsByTagName("BorderDirection");
            final NodeList internalHvdcs = configurationEl.getElementsByTagName("HVDC");
            final Map<String, MarketArea> marketAreasMap = new TreeMap<>();

            final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();

            importMarketAreas(marketAreas, marketAreasMap, configuration);
            importVirtualHubs(virtualHubs, marketAreasMap, configuration);
            importBorderDirections(borderDirections, configuration);
            importInternalHvdcs(internalHvdcs, configuration);

            return configuration;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new VirtualHubsConfigProcessingException(e);
        }
    }

    private static void importMarketAreas(final NodeList marketAreas, final Map<String, MarketArea> marketAreasMap, final VirtualHubsConfiguration configuration) {
        for (int marketAreaIndex = 0; marketAreaIndex < marketAreas.getLength(); marketAreaIndex++) {
            final Node node = marketAreas.item(marketAreaIndex);
            final String code = node.getAttributes().getNamedItem("Code").getNodeValue();
            final String eic = node.getAttributes().getNamedItem("Eic").getNodeValue();
            final boolean isMcParticipant = Boolean.parseBoolean(node.getAttributes().getNamedItem("MCParticipant").getNodeValue());
            final boolean isAhc = getAhc(node);
            final MarketArea marketArea = new MarketArea(code, eic, isMcParticipant, isAhc);
            marketAreasMap.put(code, marketArea);
            configuration.addMarketArea(marketArea);
        }
    }

    private static void importVirtualHubs(final NodeList virtualHubs, final Map<String, MarketArea> marketAreasMap, final VirtualHubsConfiguration configuration) {
        for (int virtualHubIndex = 0; virtualHubIndex < virtualHubs.getLength(); virtualHubIndex++) {
            final Node node = virtualHubs.item(virtualHubIndex);
            final String code = node.getAttributes().getNamedItem("Code").getNodeValue();
            final String eic = node.getAttributes().getNamedItem("Eic").getNodeValue();
            final boolean isMcParticipant = Boolean.parseBoolean(node.getAttributes().getNamedItem("MCParticipant").getNodeValue());
            final boolean isAhc = getAhc(node);
            final String nodeName = node.getAttributes().getNamedItem("NodeName").getNodeValue();
            final MarketArea marketArea = marketAreasMap.get(node.getAttributes().getNamedItem("RelatedMA").getNodeValue());
            final String oppositeHub = Optional.ofNullable(node.getAttributes().getNamedItem("OppositeHub")).map(Node::getNodeValue).orElse(null);
            configuration.addVirtualHub(new VirtualHub(code, eic, isMcParticipant, isAhc, nodeName, marketArea, oppositeHub));
        }
    }

    private static void importBorderDirections(final NodeList borderDirections, final VirtualHubsConfiguration configuration) {
        for (int borderDirectionIndex = 0; borderDirectionIndex < borderDirections.getLength(); borderDirectionIndex++) {
            final Node node = borderDirections.item(borderDirectionIndex);
            final String from = node.getAttributes().getNamedItem("From").getNodeValue();
            final String to = node.getAttributes().getNamedItem("To").getNodeValue();
            final boolean isAhc = getAhc(node);
            configuration.addBorderDirection(new BorderDirection(from, to, isAhc));
        }
    }

    private static void importInternalHvdcs(final NodeList internalHvdcs, final VirtualHubsConfiguration configuration) {
        for (int internalHvdcIndex = 0; internalHvdcIndex < internalHvdcs.getLength(); internalHvdcIndex++) {
            final Element hvdcNode = (Element) internalHvdcs.item(internalHvdcIndex);
            final NodeList poles = hvdcNode.getElementsByTagName("pole");
            final Element pole1 = (Element) poles.item(0);
            final Element pole2 = (Element) poles.item(1); // null if there is no such element
            importHvdcPoleConfiguration(pole1, configuration);
            importHvdcPoleConfiguration(pole2, configuration);
        }
    }

    private static void importHvdcPoleConfiguration(final Element pole, final VirtualHubsConfiguration configuration) {
        if (pole == null) {
            return;
        }

        final List<HvdcConverter> hvdcConverters = new ArrayList<>();
        final List<HvdcLine> hvdcLines = new ArrayList<>();

        // HVDC -> pole -> converterList -> converter (node, station)
        final NodeList converters = pole.getElementsByTagName("converter");
        for (int converterIndex = 0; converterIndex < converters.getLength(); converterIndex++) {
            final NamedNodeMap hvdcConverterAttributes = converters.item(converterIndex).getAttributes();
            hvdcConverters.add(new HvdcConverter(
                hvdcConverterAttributes.getNamedItem("node").getNodeValue(),
                hvdcConverterAttributes.getNamedItem("station").getNodeValue()));
        }
        // HVDC -> pole -> lineList -> line (from, to)
        final NamedNodeMap hvdcLineAttributes = pole.getElementsByTagName("line").item(0).getAttributes();
        hvdcLines.add(new HvdcLine(
            hvdcLineAttributes.getNamedItem("from").getNodeValue(),
            hvdcLineAttributes.getNamedItem("to").getNodeValue()));

        configuration.addInternalHvdc(new InternalHvdc(hvdcConverters, hvdcLines));
    }

    private static Boolean getAhc(Node node) {
        return Optional.ofNullable(node.getAttributes().getNamedItem("AHC"))
            .map(Node::getNodeValue)
            .map(Boolean::parseBoolean)
            .orElse(false);
    }
}
