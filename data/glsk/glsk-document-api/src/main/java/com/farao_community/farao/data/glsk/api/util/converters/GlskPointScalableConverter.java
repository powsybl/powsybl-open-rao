/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.glsk.api.util.converters;

import com.farao_community.farao.commons.CountryEICode;
import com.farao_community.farao.data.glsk.api.AbstractGlskPoint;
import com.farao_community.farao.data.glsk.api.AbstractGlskRegisteredResource;
import com.farao_community.farao.data.glsk.api.AbstractGlskShiftKey;
import com.farao_community.farao.data.glsk.api.GlskException;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;

import static com.farao_community.farao.data.glsk.api.util.converters.NetworkUtil.getMaximumGeneratorCapacity;
import static com.farao_community.farao.data.glsk.api.util.converters.NetworkUtil.getMinimumGeneratorCapacity;

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
    public static Scalable convert(Network network, AbstractGlskPoint glskPoint) {
        Objects.requireNonNull(glskPoint.getGlskShiftKeys());
        if (!glskPoint.getGlskShiftKeys().get(0).getBusinessType().equals("B45")) {
            //B42 and B43 proportional
            List<Float> percentages = new ArrayList<>();
            List<Scalable> scalables = new ArrayList<>();

            for (AbstractGlskShiftKey glskShiftKey : glskPoint.getGlskShiftKeys()) {
                if (glskShiftKey.getBusinessType().equals("B42") && glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    //B42 country
                    convertCountryProportional(network, glskShiftKey, percentages, scalables);
                } else if (glskShiftKey.getBusinessType().equals("B42") && !glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    //B42 explicit
                    convertExplicitProportional(network, glskShiftKey, percentages, scalables);
                } else if (glskShiftKey.getBusinessType().equals("B43") && !glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    //B43 participation factor
                    convertParticipationFactor(network, glskShiftKey, percentages, scalables);
                } else if (glskShiftKey.getBusinessType().equals("B44") && !glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    //B44 remaining capacity
                    convertRemainingCapacity(network, glskShiftKey, percentages, scalables);
                } else {
                    throw new GlskException("In convert glskShiftKey business type not supported");
                }
            }
            return Scalable.proportional(percentages, scalables, true);
        } else {
            //B45 merit order
            return convertMeritOrder(network, glskPoint);
        }
    }

    private static void convertRemainingCapacity(Network network, AbstractGlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        LOGGER.debug("GLSK Type B44, not empty registered resources list --> remaining capacity proportional GSK");
        // Remaining capacity algorithm is supposed to put all generators at Pmin at the same time when decreasing
        // generation, and to put all generators at Pmax at the same time when increasing generation.
        // Though the scaling is not symmetrical.
        List<AbstractGlskRegisteredResource> generatorResources = NetworkUtil.getAvailableGeneratorsAsResources(glskShiftKey.getRegisteredResourceArrayList(), network);
        Scalable upScalable = createRemainingCapacityScalable(network, glskShiftKey, generatorResources, GlskPointScalableConverter::getRemainingCapacityUp);
        Scalable downScalable = createRemainingCapacityScalable(network, glskShiftKey, generatorResources, GlskPointScalableConverter::getRemainingCapacityDown);
        percentages.add(100.f);
        scalables.add(Scalable.upDown(upScalable, downScalable));
    }

    private static Scalable createRemainingCapacityScalable(Network network, AbstractGlskShiftKey glskShiftKey, List<AbstractGlskRegisteredResource> generatorResources, BiFunction<AbstractGlskRegisteredResource, Network, Double> remainingCapacityFunction) {
        List<Float> percentages = new ArrayList<>();
        List<Scalable> scalables = new ArrayList<>();
        double totalFactor = generatorResources.stream().mapToDouble(resource -> remainingCapacityFunction.apply(resource, network)).sum();
        generatorResources.forEach(generatorResource -> {
            percentages.add((float) (100 * glskShiftKey.getQuantity().floatValue() * remainingCapacityFunction.apply(generatorResource, network) / totalFactor));
            scalables.add(createGeneratorScalableWithLimits(generatorResource, network));
        });
        return Scalable.proportional(percentages, scalables);
    }

    /**
     * convert merit order glsk point to scalable
     * @param network iidm network
     * @param glskPoint glsk point merit order
     * @return stack scalable
     */
    private static Scalable convertMeritOrder(Network network, AbstractGlskPoint glskPoint) {
        Objects.requireNonNull(network);

        Scalable upScalable = Scalable.stack(glskPoint.getGlskShiftKeys().stream()
                .filter(abstractGlskShiftKey -> abstractGlskShiftKey.getMeritOrderPosition() > 0)
                .sorted(Comparator.comparingInt(AbstractGlskShiftKey::getMeritOrderPosition))
                .map(glskShiftKey -> {
                    AbstractGlskRegisteredResource generatorRegisteredResource = Objects.requireNonNull(glskShiftKey.getRegisteredResourceArrayList()).get(0);
                    return createGeneratorScalableWithLimits(generatorRegisteredResource, network);
                }).toArray(Scalable[]::new));

        Scalable downScalable = Scalable.stack(glskPoint.getGlskShiftKeys().stream()
                .filter(abstractGlskShiftKey -> abstractGlskShiftKey.getMeritOrderPosition() < 0)
                .sorted(Comparator.comparingInt(AbstractGlskShiftKey::getMeritOrderPosition).reversed())
                .map(glskShiftKey -> {
                    AbstractGlskRegisteredResource generatorRegisteredResource = Objects.requireNonNull(glskShiftKey.getRegisteredResourceArrayList()).get(0);
                    return createGeneratorScalableWithLimits(generatorRegisteredResource, network);
                }).toArray(Scalable[]::new));
        return Scalable.upDown(upScalable, downScalable);
    }

    /**
     * convert country proportional glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertCountryProportional(Network network, AbstractGlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        Country country = new CountryEICode(glskShiftKey.getSubjectDomainmRID()).getCountry();

        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) GSK");
            List<Generator> generators = NetworkUtil.getCountryGenerators(country, network);
            addPercentagesAndScalablesFromGenerators(generators, glskShiftKey, percentages, scalables);
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) LSK");
            List<Load> loads = NetworkUtil.getCountryLoads(country, network);
            addPercentagesAndScalablesFromLoads(loads, glskShiftKey, percentages, scalables);
        }
    }

    /**
     * convert explicit glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertExplicitProportional(Network network, AbstractGlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional GSK");
            List<Generator> generators = NetworkUtil.getAvailableGenerators(glskShiftKey.getRegisteredResourceArrayList(), network);
            addPercentagesAndScalablesFromGenerators(generators, glskShiftKey, percentages, scalables);
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional LSK");
            List<Load> loads = NetworkUtil.getAvailableLoads(glskShiftKey.getRegisteredResourceArrayList(), network);
            addPercentagesAndScalablesFromLoads(loads, glskShiftKey, percentages, scalables);
        }
    }

    /**
     * convert participation factor glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertParticipationFactor(Network network, AbstractGlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B43 GSK");
            List<AbstractGlskRegisteredResource> generatorResources = NetworkUtil.getAvailableGeneratorsAsResources(glskShiftKey.getRegisteredResourceArrayList(), network);
            double totalFactor = generatorResources.stream().mapToDouble(AbstractGlskRegisteredResource::getParticipationFactor).sum();
            generatorResources.forEach(generatorResource -> percentages.add(100 * glskShiftKey.getQuantity().floatValue() * (float) generatorResource.getParticipationFactor() / (float) totalFactor));
            generatorResources.forEach(generatorResource -> scalables.add(Scalable.onGenerator(generatorResource.getGeneratorId())));
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B43 LSK");
            List<AbstractGlskRegisteredResource> loadResources = NetworkUtil.getAvailableLoadsAsResources(glskShiftKey.getRegisteredResourceArrayList(), network);
            double totalFactor = loadResources.stream().mapToDouble(AbstractGlskRegisteredResource::getParticipationFactor).sum();
            loadResources.forEach(loadResource -> percentages.add(100 * glskShiftKey.getQuantity().floatValue() * (float) loadResource.getParticipationFactor() / (float) totalFactor));
            loadResources.forEach(loadResource -> scalables.add(Scalable.onLoad(loadResource.getLoadId())));
        }
    }

    private static double getRemainingCapacityUp(AbstractGlskRegisteredResource resource, Network network) {
        Generator generator = network.getGenerator(resource.getGeneratorId());
        double maxP = getMaximumGeneratorCapacity(resource, network);
        return Math.max(0., maxP - NetworkUtil.pseudoTargetP(generator));
    }

    private static double getRemainingCapacityDown(AbstractGlskRegisteredResource resource, Network network) {
        Generator generator = network.getGenerator(resource.getGeneratorId());
        double minP = getMinimumGeneratorCapacity(resource, network);
        return Math.max(0, NetworkUtil.pseudoTargetP(generator) - minP);
    }

    private static Scalable createGeneratorScalableWithLimits(AbstractGlskRegisteredResource resource, Network network) {
        String generatorId = resource.getGeneratorId();
        double incomingMaxP = getMaximumGeneratorCapacity(resource, network);
        double incomingMinP = getMinimumGeneratorCapacity(resource, network);
        return Scalable.onGenerator(generatorId, incomingMinP, incomingMaxP);
    }

    private static void addPercentagesAndScalablesFromGenerators(List<Generator> generators,
                                                                 AbstractGlskShiftKey glskShiftKey,
                                                                 List<Float> percentages,
                                                                 List<Scalable> scalables) {
        double totalP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
        //calculate factor of each generator
        generators.forEach(generator -> percentages.add(
                100 * glskShiftKey.getQuantity().floatValue() * (float) NetworkUtil.pseudoTargetP(generator) / (float) totalP)
        );
        generators.forEach(generator -> scalables.add(Scalable.onGenerator(generator.getId())));
    }

    private static void addPercentagesAndScalablesFromLoads(List<Load> loads,
                                                            AbstractGlskShiftKey glskShiftKey,
                                                            List<Float> percentages,
                                                            List<Scalable> scalables) {
        double totalP = loads.stream().mapToDouble(NetworkUtil::pseudoP0).sum();
        //calculate factor of each generator
        loads.forEach(load -> percentages.add(
                100 * glskShiftKey.getQuantity().floatValue() * (float) NetworkUtil.pseudoP0(load) / (float) totalP)
        );
        loads.forEach(load -> scalables.add(Scalable.onLoad(load.getId())));
    }
}
