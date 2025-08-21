/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs.xml;

import com.powsybl.openrao.virtualhubs.BorderDirection;
import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
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
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element configurationEl = document.getDocumentElement();
            NodeList marketAreas = configurationEl.getElementsByTagName("MarketArea");
            NodeList virtualHubs = configurationEl.getElementsByTagName("VirtualHub");
            NodeList borderDirections = configurationEl.getElementsByTagName("BorderDirection");
            Map<String, MarketArea> marketAreasMap = new TreeMap<>();

            VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();

            for (int i = 0; i < marketAreas.getLength(); i++) {
                Node node = marketAreas.item(i);
                String code = node.getAttributes().getNamedItem("Code").getNodeValue();
                String eic = node.getAttributes().getNamedItem("Eic").getNodeValue();
                boolean isMcParticipant = Boolean.parseBoolean(node.getAttributes().getNamedItem("MCParticipant").getNodeValue());
                boolean isAhc = getAhc(node);
                MarketArea marketArea = new MarketArea(code, eic, isMcParticipant, isAhc);
                marketAreasMap.put(code, marketArea);
                configuration.addMarketArea(marketArea);
            }
            for (int i = 0; i < virtualHubs.getLength(); i++) {
                Node node = virtualHubs.item(i);
                String code = node.getAttributes().getNamedItem("Code").getNodeValue();
                String eic = node.getAttributes().getNamedItem("Eic").getNodeValue();
                boolean isMcParticipant = Boolean.parseBoolean(node.getAttributes().getNamedItem("MCParticipant").getNodeValue());
                boolean isAhc = getAhc(node);
                String nodeName = node.getAttributes().getNamedItem("NodeName").getNodeValue();
                MarketArea marketArea = marketAreasMap.get(node.getAttributes().getNamedItem("RelatedMA").getNodeValue());
                String oppositeHub = Optional.ofNullable(node.getAttributes().getNamedItem("OppositeHub")).map(Node::getNodeValue).orElse(null);
                configuration.addVirtualHub(new VirtualHub(code, eic, isMcParticipant, isAhc, nodeName, marketArea, oppositeHub));
            }
            for (int i = 0; i < borderDirections.getLength(); i++) {
                Node node = borderDirections.item(i);
                String from = node.getAttributes().getNamedItem("From").getNodeValue();
                String to = node.getAttributes().getNamedItem("To").getNodeValue();
                boolean isAhc = getAhc(node);
                configuration.addBorderDirection(new BorderDirection(from, to, isAhc));
            }
            return configuration;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new VirtualHubsConfigProcessingException(e);
        }
    }

    private static Boolean getAhc(Node node) {
        return Optional.ofNullable(node.getAttributes().getNamedItem("AHC"))
            .map(Node::getNodeValue)
            .map(Boolean::parseBoolean)
            .orElse(false);
    }
}
