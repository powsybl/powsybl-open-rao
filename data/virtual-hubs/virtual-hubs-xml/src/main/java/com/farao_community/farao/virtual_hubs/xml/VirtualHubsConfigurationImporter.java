/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs.xml;

import com.farao_community.farao.virtual_hubs.MarketArea;
import com.farao_community.farao.virtual_hubs.VirtualHub;
import com.farao_community.farao.virtual_hubs.VirtualHubsConfiguration;
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
import java.util.TreeMap;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubsConfigurationImporter {
    public VirtualHubsConfiguration importConfiguration(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "Cannot import configuration from null input stream");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element configurationEl = document.getDocumentElement();
            NodeList marketAreas = configurationEl.getElementsByTagName("MarketArea");
            NodeList virtualHubs = configurationEl.getElementsByTagName("VirtualHub");
            Map<String, MarketArea> marketAreasMap = new TreeMap<>();

            VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();

            for (int i = 0; i < marketAreas.getLength(); i++) {
                Node node = marketAreas.item(i);
                String code = node.getAttributes().getNamedItem("Code").getNodeValue();
                String eic = node.getAttributes().getNamedItem("Eic").getNodeValue();
                boolean isMcParticipant = Boolean.parseBoolean(node.getAttributes().getNamedItem("MCParticipant").getNodeValue());
                MarketArea marketArea = new MarketArea(code, eic, isMcParticipant);
                marketAreasMap.put(code, marketArea);
                configuration.addMarketArea(marketArea);
            }
            for (int i = 0; i < virtualHubs.getLength(); i++) {
                Node node = virtualHubs.item(i);
                String code = node.getAttributes().getNamedItem("Code").getNodeValue();
                String eic = node.getAttributes().getNamedItem("Eic").getNodeValue();
                boolean isMcParticipant = Boolean.parseBoolean(node.getAttributes().getNamedItem("MCParticipant").getNodeValue());
                String nodeName = node.getAttributes().getNamedItem("NodeName").getNodeValue();
                MarketArea marketArea = marketAreasMap.get(node.getAttributes().getNamedItem("RelatedMA").getNodeValue());
                configuration.addVirtualHub(new VirtualHub(code, eic, isMcParticipant, nodeName, marketArea));
            }
            return configuration;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new VirtualHubsConfigProcessingException(e);
        }
    }
}
