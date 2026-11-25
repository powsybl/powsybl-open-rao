/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.xml;

import com.powsybl.openrao.virtualhubs.BorderDirection;
import com.powsybl.openrao.virtualhubs.ControlArea;
import com.powsybl.openrao.virtualhubs.HvdcConverter;
import com.powsybl.openrao.virtualhubs.HvdcLine;
import com.powsybl.openrao.virtualhubs.HvdcPole;
import com.powsybl.openrao.virtualhubs.InternalHvdc;
import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.Region;
import com.powsybl.openrao.virtualhubs.SynchronousArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.Voltage;
import com.powsybl.openrao.virtualhubs.xml.xsd.BorderDirections;
import com.powsybl.openrao.virtualhubs.xml.xsd.Configuration;
import com.powsybl.openrao.virtualhubs.xml.xsd.ControlAreas;
import com.powsybl.openrao.virtualhubs.xml.xsd.ConverterListType;
import com.powsybl.openrao.virtualhubs.xml.xsd.HVDCType;
import com.powsybl.openrao.virtualhubs.xml.xsd.InitialVoltages;
import com.powsybl.openrao.virtualhubs.xml.xsd.InternalHVDCs;
import com.powsybl.openrao.virtualhubs.xml.xsd.LOT3Parameter;
import com.powsybl.openrao.virtualhubs.xml.xsd.LineListType;
import com.powsybl.openrao.virtualhubs.xml.xsd.LocalLOT3Parameters;
import com.powsybl.openrao.virtualhubs.xml.xsd.MarketAreas;
import com.powsybl.openrao.virtualhubs.xml.xsd.PoleType;
import com.powsybl.openrao.virtualhubs.xml.xsd.SynchronousAreas;
import com.powsybl.openrao.virtualhubs.xml.xsd.VirtualHubs;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

            final Map<String, SynchronousArea> synchronousAreasMap = new TreeMap<>();
            final Map<String, ControlArea> controlAreasMap = new TreeMap<>();
            final Map<String, MarketArea> marketAreasMap = new TreeMap<>();
            final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();

            final com.powsybl.openrao.virtualhubs.xml.xsd.Region region = unmarshalled.getRegion();
            if (region != null) {
                configuration.setRegion(new Region(region.getCode(), region.getEic()));
            }

            final SynchronousAreas synchronousAreas = unmarshalled.getSynchronousAreas();
            if (synchronousAreas != null) {
                importSynchronousAreas(synchronousAreas.getSynchronousArea(), synchronousAreasMap, configuration);
            }

            final ControlAreas controlAreas = unmarshalled.getControlAreas();
            if (controlAreas != null) {
                importControlAreas(controlAreas.getControlArea(), controlAreasMap, configuration);
            }

            final MarketAreas marketAreas = unmarshalled.getMarketAreas();
            if (marketAreas != null) {
                importMarketAreas(marketAreas.getMarketArea(), synchronousAreasMap, controlAreasMap, marketAreasMap, configuration);
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
                importInternalHvdcs(internalHvdcs.getHVDC(), marketAreasMap, configuration);
            }

            return configuration;
        } catch (JAXBException e) {
            throw new VirtualHubsConfigProcessingException(e);
        }
    }

    private static void importSynchronousAreas(final List<com.powsybl.openrao.virtualhubs.xml.xsd.SynchronousArea> rawSynchronousAreas,
                                               final Map<String, SynchronousArea> synchronousAreasMap,
                                               final VirtualHubsConfiguration configuration) {
        for (com.powsybl.openrao.virtualhubs.xml.xsd.SynchronousArea rawSynchronousArea : rawSynchronousAreas) {
            final String code = rawSynchronousArea.getCode();
            final String eic = rawSynchronousArea.getEic();
            final boolean isLive = rawSynchronousArea.isLive();
            final boolean isMainArea = Optional.ofNullable(rawSynchronousArea.isMainArea()).orElse(false);

            Map<String, String> localLOT3ParametersMap = null;
            final LocalLOT3Parameters localLOT3Parameters = rawSynchronousArea.getLocalLOT3Parameters();
            if (localLOT3Parameters != null && localLOT3Parameters.getLocalLOT3Parameter() != null) {
                localLOT3ParametersMap = localLOT3Parameters.getLocalLOT3Parameter().stream()
                    .collect(Collectors.toMap(LOT3Parameter::getKey, LOT3Parameter::getValue));
            }

            List<Voltage> initialVoltagesList = null;
            final InitialVoltages initialVoltages = rawSynchronousArea.getInitialVoltages();
            if (initialVoltages != null && initialVoltages.getVoltage() != null) {
                initialVoltagesList = initialVoltages.getVoltage().stream()
                    .map(voltage -> new Voltage(voltage.getNominal(), voltage.getInitial(), voltage.getSlackSearchPriority()))
                    .toList();
            }

            final SynchronousArea synchronousArea = new SynchronousArea(
                code,
                eic,
                isLive,
                isMainArea,
                localLOT3ParametersMap,
                initialVoltagesList);
            synchronousAreasMap.put(code, synchronousArea);
            configuration.addSynchronousArea(synchronousArea);
        }
    }

    private static void importControlAreas(final List<com.powsybl.openrao.virtualhubs.xml.xsd.ControlArea> rawControlAreas,
                                           final Map<String, ControlArea> controlAreasMap,
                                           final VirtualHubsConfiguration configuration) {
        for (com.powsybl.openrao.virtualhubs.xml.xsd.ControlArea rawControlArea : rawControlAreas) {
            final String code = rawControlArea.getCode();
            final String eic = rawControlArea.getEic();
            final boolean isMcParticipant = rawControlArea.isMCParticipant();
            final boolean isTechCounterparty = Optional.ofNullable(rawControlArea.isTechCounterparty()).orElse(false);
            final boolean isSdac = Optional.ofNullable(rawControlArea.isSDAC()).orElse(false);

            final ControlArea controlArea = new ControlArea(code, eic, isMcParticipant, isTechCounterparty, isSdac);
            controlAreasMap.put(code, controlArea);
            configuration.addControlArea(controlArea);
        }
    }

    private static void importMarketAreas(final List<com.powsybl.openrao.virtualhubs.xml.xsd.MarketArea> rawMarketAreas,
                                          final Map<String, SynchronousArea> synchronousAreasMap,
                                          final Map<String, ControlArea> controlAreasMap,
                                          final Map<String, MarketArea> marketAreasMap,
                                          final VirtualHubsConfiguration configuration) {
        for (com.powsybl.openrao.virtualhubs.xml.xsd.MarketArea rawMarketArea : rawMarketAreas) {
            final String code = rawMarketArea.getCode();
            final String eic = rawMarketArea.getEic();
            final boolean isMcParticipant = rawMarketArea.isMCParticipant();
            final boolean isAhc = Optional.ofNullable(rawMarketArea.isAHC()).orElse(false);
            final boolean isTechCounterparty = Optional.ofNullable(rawMarketArea.isTechCounterparty()).orElse(false);
            final boolean isSdac = Optional.ofNullable(rawMarketArea.isSDAC()).orElse(false);

            final String synchronousAreaCode = rawMarketArea.getSynchronousArea();
            final SynchronousArea synchronousArea = synchronousAreaCode != null ? synchronousAreasMap.get(synchronousAreaCode) : null;

            final String relatedCaCode = rawMarketArea.getRelatedCA();
            final ControlArea relatedCa = relatedCaCode != null ? controlAreasMap.get(relatedCaCode) : null;

            final boolean isItcp = Optional.ofNullable(rawMarketArea.isITCP()).orElse(false);
            final boolean isIgnoreECs = Optional.ofNullable(rawMarketArea.isIgnoreECs()).orElse(false);

            final MarketArea marketArea = new MarketArea(
                code, eic, isMcParticipant, isAhc, isTechCounterparty, isSdac, synchronousArea, relatedCa, isItcp, isIgnoreECs
            );
            marketAreasMap.put(code, marketArea);
            configuration.addMarketArea(marketArea);
        }
    }

    private static void importVirtualHubs(final List<com.powsybl.openrao.virtualhubs.xml.xsd.VirtualHub> rawVirtualHubs,
                                          final Map<String, MarketArea> marketAreasMap,
                                          final VirtualHubsConfiguration configuration) {
        for (com.powsybl.openrao.virtualhubs.xml.xsd.VirtualHub rawVirtualHub : rawVirtualHubs) {
            final String code = rawVirtualHub.getCode();
            final String eic = rawVirtualHub.getEic();
            final boolean isMcParticipant = rawVirtualHub.isMCParticipant();
            final String nodeName = rawVirtualHub.getNodeName();
            final String oppositeHub = rawVirtualHub.getOppositeHub();
            final MarketArea relatedMa = marketAreasMap.get(rawVirtualHub.getRelatedMA());
            final boolean isAhc = Optional.ofNullable(rawVirtualHub.isAHC()).orElse(false);
            final boolean isSdac = Optional.ofNullable(rawVirtualHub.isSDAC()).orElse(false);
            final BigDecimal splittingFactor = rawVirtualHub.getSplittingFactor();
            final BigDecimal lf = rawVirtualHub.getLF();
            final BigDecimal rlf = rawVirtualHub.getRLF();
            configuration.addVirtualHub(new VirtualHub(
                code, eic, isMcParticipant, isAhc, nodeName, relatedMa, oppositeHub, isSdac, splittingFactor, lf, rlf
            ));
        }
    }

    private static void importBorderDirections(final List<com.powsybl.openrao.virtualhubs.xml.xsd.BorderDirection> rawBorderDirections,
                                               final VirtualHubsConfiguration configuration) {
        for (com.powsybl.openrao.virtualhubs.xml.xsd.BorderDirection rawBorderDirection : rawBorderDirections) {
            final String from = rawBorderDirection.getFrom();
            final String to = rawBorderDirection.getTo();
            final boolean isAhc = Optional.ofNullable(rawBorderDirection.isAHC()).orElse(false);
            final boolean isSdac = Optional.ofNullable(rawBorderDirection.isSDAC()).orElse(false);
            configuration.addBorderDirection(new BorderDirection(from, to, isAhc, isSdac));
        }
    }

    private static void importInternalHvdcs(final List<HVDCType> internalHvdcs,
                                            final Map<String, MarketArea> marketAreasMap,
                                            final VirtualHubsConfiguration configuration) {
        internalHvdcs.forEach(hvdc -> importInternalHvdc(hvdc, marketAreasMap, configuration));
    }

    private static void importInternalHvdc(final HVDCType internalHvdc,
                                           final Map<String, MarketArea> marketAreasMap,
                                           final VirtualHubsConfiguration configuration) {
        final String eic = internalHvdc.getEic();
        final String code = internalHvdc.getCode();
        final List<HvdcPole> hvdcPoles = internalHvdc.getPole()
            .stream()
            .map(VirtualHubsConfigurationImporter::poleTypeToHvdcPole)
            .filter(Objects::nonNull)
            .toList();
        final MarketArea relatedMa = marketAreasMap.get(internalHvdc.getRelatedMA());
        final boolean isMthvdc = internalHvdc.isMTHVDC();
        final boolean isBipolar = internalHvdc.isBipolar();

        configuration.addInternalHvdc(new InternalHvdc(
            eic, code, hvdcPoles, relatedMa, isMthvdc, isBipolar
        ));
    }

    private static HvdcPole poleTypeToHvdcPole(final PoleType pole) {
        if (pole == null) {
            return null;
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
                hvdcLines.add(new HvdcLine(hvdcLine.getId(), hvdcLine.getFrom(), hvdcLine.getTo()));
            }
        }

        return new HvdcPole(pole.getId(), pole.getEic(), hvdcConverters, hvdcLines);
    }
}
