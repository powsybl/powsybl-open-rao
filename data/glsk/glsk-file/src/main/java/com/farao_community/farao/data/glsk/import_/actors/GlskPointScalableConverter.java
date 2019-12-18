/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.import_.actors;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.glsk.import_.EICode;
import com.farao_community.farao.data.glsk.import_.GlskPoint;
import com.farao_community.farao.data.glsk.import_.GlskRegisteredResource;
import com.farao_community.farao.data.glsk.import_.GlskShiftKey;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Convert a single GlskPoint to Scalable
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public final class GlskPointScalableConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointScalableConverter.class);

    private GlskPointScalableConverter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * @param network IIDM network
     * @param glskPoint GLSK Point
     * @return powsybl-core Scalable
     */
    public static Scalable convert(Network network, GlskPoint glskPoint, TypeGlskFile typeGlskFile) {
        Objects.requireNonNull(glskPoint.getGlskShiftKeys());
        if (!glskPoint.getGlskShiftKeys().get(0).getBusinessType().equals("B45")) {
            //B42 and B43 proportional
            if (glskPoint.getGlskShiftKeys().size() > 2) {
                throw new FaraoException("Multi shift keys not supported for proportional GLSK.");
            }

            List<Float> percentages = new ArrayList<>();
            List<Scalable> scalables = new ArrayList<>();

            for (GlskShiftKey glskShiftKey : glskPoint.getGlskShiftKeys()) {
                if (glskShiftKey.getBusinessType().equals("B42") && glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    //B42 country
                    convertCountryProportional(network, glskShiftKey, percentages, scalables);
                } else if (glskShiftKey.getBusinessType().equals("B42") && !glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    //B42 explicit
                    convertExplicitProportional(network, glskShiftKey, percentages, scalables, typeGlskFile);
                } else if (glskShiftKey.getBusinessType().equals("B43") && !glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    //B43 participation factor
                    convertParticipationFactor(network, glskShiftKey, percentages, scalables, typeGlskFile);
                } else {
                    throw new FaraoException("In convert glskShiftKey business type not supported");
                }
            }
            return Scalable.proportional(percentages, scalables);
        } else {
            //B45 merit order
            return convertMeritOrder(network, glskPoint, typeGlskFile);
        }
    }

    /**
     * convert merit order glsk point to scalable
     * @param network iidm network
     * @param glskPoint glsk point merit order
     * @return stack scalable
     */
    private static Scalable convertMeritOrder(Network network, GlskPoint glskPoint, TypeGlskFile typeGlskFile) {
        Objects.requireNonNull(network);

        Map<Integer, String> orders = new HashMap<>(); //Merit order position
        int maxPosition = -1;
        for (GlskShiftKey glskShiftKey : glskPoint.getGlskShiftKeys()) {
            GlskRegisteredResource generatorRegisteredResource = Objects.requireNonNull(glskShiftKey.getRegisteredResourceArrayList()).get(0);
            String generatorId = generatorRegisteredResource.getGeneratorId(typeGlskFile);
            double incomingMaxP = generatorRegisteredResource.getMaximumCapacity().orElse(Double.MAX_VALUE);
            double incomingMinP = generatorRegisteredResource.getMinimumCapacity().orElse(-Double.MAX_VALUE);
            //set MinP and MaxP
            Generator generator = network.getGenerator(generatorId);
            if (generator != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("MeritOrder ShiftKey changes %s MaxP value from %s to: %s", generatorId, generator.getMaxP(), incomingMaxP));
                    LOGGER.debug(String.format("MeritOrder ShiftKey changes %s MinP value from %s to: %s", generatorId, generator.getMinP(), incomingMinP));
                }
                generator.setMaxP(incomingMaxP);
                generator.setMinP(incomingMinP);
            }
            orders.put(glskShiftKey.getMeritOrderPosition(), generatorId); //order Scalable according to position in a map
            maxPosition = Math.max(maxPosition, glskShiftKey.getMeritOrderPosition()); //get max position
        }

        List<String> generatorIds = new ArrayList<>(); //scalable list
        for (int i = 1; i <= maxPosition; ++i) {
            generatorIds.add(orders.get(i)); //add to scalable list
        }
        return Scalable.stack(generatorIds.toArray(new String[0]));
    }

    /**
     * convert country proportional glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertCountryProportional(Network network, GlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        Country country = new EICode(glskShiftKey.getSubjectDomainmRID()).getCountry();

        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) GSK");
            //calculate sum P of country's generators
            double totalCountryP = network.getGeneratorStream().filter(generator -> generator.getTerminal().getVoltageLevel().getSubstation().getCountry().orElse(null).equals(country))
                    .mapToDouble(Generator::getTargetP).sum();
            //calculate factor of each generator
            network.getGeneratorStream().filter(generator -> generator.getTerminal().getVoltageLevel().getSubstation().getCountry().orElse(null).equals(country))
                    .forEach(generator -> percentages.add(100 * glskShiftKey.getQuantity().floatValue() * (float) generator.getTargetP() / (float) totalCountryP));
            network.getGeneratorStream().filter(generator -> generator.getTerminal().getVoltageLevel().getSubstation().getCountry().orElse(null).equals(country))
                    .forEach(generator -> scalables.add(Scalable.onGenerator(generator.getId())));
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) LSK");
            //calculate sum P of country's loads
            double totalCountryP = network.getLoadStream().filter(load -> load.getTerminal().getVoltageLevel().getSubstation().getCountry().orElse(null).equals(country))
                    .mapToDouble(Load::getP0).sum();
            network.getLoadStream().filter(load -> load.getTerminal().getVoltageLevel().getSubstation().getCountry().orElse(null).equals(country))
                    .forEach(load -> percentages.add(100 * glskShiftKey.getQuantity().floatValue() * (float) load.getP0() / (float) totalCountryP));
            network.getLoadStream().filter(load -> load.getTerminal().getVoltageLevel().getSubstation().getCountry().orElse(null).equals(country))
                    .forEach(load -> scalables.add(Scalable.onLoad(load.getId(), -Double.MAX_VALUE, Double.MAX_VALUE)));
        }
    }

    /**
     * convert explicit glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertExplicitProportional(Network network, GlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables, TypeGlskFile typeGlskFile) {
        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional GSK");
            List<String> genenratorsList = glskShiftKey.getRegisteredResourceArrayList().stream().map(generatorResource -> generatorResource.getGeneratorId(typeGlskFile)).collect(Collectors.toList());
            double totalP = network.getGeneratorStream().filter(generator -> genenratorsList.contains(generator.getId())).mapToDouble(Generator::getTargetP).sum();
            //calculate factor of each generator
            network.getGeneratorStream().filter(generator -> genenratorsList.contains(generator.getId()))
                    .forEach(generator -> percentages.add(100 * glskShiftKey.getQuantity().floatValue() * (float) generator.getTargetP() / (float) totalP));
            network.getGeneratorStream().filter(generator -> genenratorsList.contains(generator.getId()))
                    .forEach(generator -> scalables.add(Scalable.onGenerator(generator.getId())));
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional LSK");
            List<String> loadsList = glskShiftKey.getRegisteredResourceArrayList().stream().map(loadResource -> loadResource.getLoadId(typeGlskFile)).collect(Collectors.toList());
            double totalP = network.getLoadStream().filter(load -> loadsList.contains(load.getId())).mapToDouble(Load::getP0).sum();
            network.getLoadStream().filter(load -> loadsList.contains(load.getId()))
                    .forEach(load -> percentages.add(100 * glskShiftKey.getQuantity().floatValue() * (float) load.getP0() / (float) totalP));
            network.getLoadStream().filter(load -> loadsList.contains(load.getId()))
                    .forEach(load -> scalables.add(Scalable.onLoad(load.getId(), -Double.MAX_VALUE, Double.MAX_VALUE)));
        }
    }

    /**
     * convert participation factor glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertParticipationFactor(Network network, GlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables, TypeGlskFile typeGlskFile) {
        List<GlskRegisteredResource> resourceList = glskShiftKey.getRegisteredResourceArrayList();

        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B43 GSK");
            double totalFactor = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .filter(generatorResource -> network.getGenerator(generatorResource.getGeneratorId(typeGlskFile)) != null)
                    .mapToDouble(GlskRegisteredResource::getParticipationFactor).sum();

            resourceList.stream().filter(generatorResource -> network.getGenerator(generatorResource.getGeneratorId(typeGlskFile)) != null)
                    .forEach(generatorResource -> percentages.add(100 * glskShiftKey.getQuantity().floatValue() * (float) generatorResource.getParticipationFactor() / (float) totalFactor));
            resourceList.stream().filter(generatorResource -> network.getGenerator(generatorResource.getGeneratorId(typeGlskFile)) != null)
                    .forEach(generatorResource -> scalables.add(Scalable.onGenerator(generatorResource.getGeneratorId(typeGlskFile))));
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B43 LSK");
            double totalFactor = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .filter(loadResource -> network.getLoad(loadResource.getLoadId(typeGlskFile)) != null)
                    .mapToDouble(GlskRegisteredResource::getParticipationFactor).sum();

            resourceList.stream().filter(loadResource -> network.getLoad(loadResource.getLoadId(typeGlskFile)) != null)
                    .forEach(loadResource -> percentages.add(100 * glskShiftKey.getQuantity().floatValue() * (float) loadResource.getParticipationFactor() / (float) totalFactor));
            resourceList.stream().filter(loadResource -> network.getLoad(loadResource.getLoadId(typeGlskFile)) != null)
                    .forEach(loadResource -> scalables.add(Scalable.onLoad(loadResource.getLoadId(typeGlskFile), -Double.MAX_VALUE, Double.MAX_VALUE)));
        }
    }
}
