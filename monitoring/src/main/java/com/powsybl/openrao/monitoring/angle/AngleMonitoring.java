/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.angle;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.AbstractMonitoring;
import com.powsybl.openrao.monitoring.MonitoringInput;
import com.powsybl.openrao.monitoring.redispatching.RedispatchAction;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 *
 */
public class AngleMonitoring extends AbstractMonitoring<AngleCnec> {

    public AngleMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        super(loadFlowProvider, loadFlowParameters);
    }

    public AngleMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters, ComputationManager computationManager) {
        super(loadFlowProvider, loadFlowParameters, computationManager);
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with AngleMonitoringResult
     */
    public static RaoResult runAndUpdateRaoResult(String loadFlowProvider,
                                                       LoadFlowParameters loadFlowParameters,
                                                       int numberOfLoadFlowsInParallel,
                                                       MonitoringInput monitoringInput) throws OpenRaoException {
        final MonitoringResult angleMonitoringResult = new AngleMonitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), angleMonitoringResult);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     */
    public static RaoResult runAndUpdateRaoResult(String loadFlowProvider,
                                                       LoadFlowParameters loadFlowParameters,
                                                       ComputationManager computationManager,
                                                       int numberOfLoadFlowsInParallel,
                                                       MonitoringInput monitoringInput) throws OpenRaoException {
        final MonitoringResult angleMonitoringResult = new AngleMonitoring(loadFlowProvider, loadFlowParameters, computationManager).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), angleMonitoringResult);
    }

    @Override
    // For angle monitoring, we only keep network actions that only have elementary actions that are injection action
    protected Set<NetworkAction> getValidNetworkActionsAssociatedToCnec(Network network, Crac crac, Cnec cnec, PhysicalParameter physicalParameter, ZonalData<Scalable> scalableZonalData) {
        Set<NetworkAction> availableNetworkActions =
            crac.getNetworkActions().stream()
                .filter(networkAction ->
                    networkAction.getUsageRules().stream()
                        .filter(OnConstraint.class::isInstance)
                        .map(OnConstraint.class::cast)
                        .anyMatch(onConstraint -> onConstraint.getCnec().equals(cnec)))
                .filter(networkAction -> networkAction.getElementaryActions().stream()
                    .allMatch(ea -> isValidInjectionAction(ea, network, networkAction.getId())))
                .collect(Collectors.toSet());

        if (!availableNetworkActions.isEmpty()) {
            Set<Country> glskCountries = getCountriesFromGlsk(scalableZonalData);
            availableNetworkActions = availableNetworkActions.stream().filter(networkAction -> networkAction.getElementaryActions().stream()
                .allMatch(ea -> checkWithGlsk(glskCountries, getCountryFromInjectionAction(ea, network), networkAction.getId()))).collect(Collectors.toSet());
        }

        return availableNetworkActions;
    }

    @Override
    protected void rebalanceNetwork(Network network, Set<NetworkAction> networkActionsToApply, ZonalData<Scalable> scalableZonalData) {
        // Get power to be redispatched and network elements to be excluded
        EnumMap<Country, Double> powerToBeRedispatched = new EnumMap<>(Country.class);
        Set<String> networkElementsToBeExcluded = new HashSet<>();
        networkActionsToApply.forEach(networkAction ->
            networkAction.getElementaryActions().forEach(ea ->
                storeEnergyToRedispatchAndNetworkElementsToExclude(ea, network, networkElementsToBeExcluded, powerToBeRedispatched)
            ));
        redispatchNetworkActions(network, powerToBeRedispatched, networkElementsToBeExcluded, scalableZonalData);
    }

    @Override
    protected AngleCnecMonitoringResult computeCnecResult(AngleCnec angleCnec, Network network, Unit unit) {
        return new AngleCnecMonitoringResult(angleCnec, unit, network);
    }

    @Override
    protected Set<AngleCnec> getCnecs(Crac crac) {
        return crac.getAngleCnecs();
    }

    @Override
    protected AngleCnecMonitoringResult makeFailedCnecResult(AngleCnec cnec) {
        return new AngleCnecMonitoringResult(cnec, Unit.DEGREE, Double.NaN);
    }

    // Helper functions

    private boolean isValidInjectionAction(Action ea,
                                           Network network,
                                           String naId) {

        if (!(ea instanceof LoadAction) && !(ea instanceof GeneratorAction)) {
            BUSINESS_WARNS.warn("Remedial action {} is ignored : it has an elementary action that's not an injection setpoint.", naId);
            return false;
        }

        Injection<?> ne = getInjectionSetpointIdentifiable(ea, network);
        Optional<Substation> substation = ne.getTerminal().getVoltageLevel().getSubstation();

        if (substation.isEmpty()) {
            BUSINESS_WARNS.warn("Remedial action {} is ignored : it has an elementary action that doesn't have a substation.", naId);
            return false;
        }

        Optional<Country> country = substation.get().getCountry();
        if (country.isEmpty()) {
            BUSINESS_WARNS.warn("Remedial action {} is ignored : it has an elementary action that doesn't have a country.", naId);
            return false;
        }

        return true;
    }

    private boolean checkWithGlsk(Set<Country> glskCountries, Country country, String naId) {
        if (!glskCountries.contains(country)) {
            BUSINESS_WARNS.warn("Remedial action {} is ignored : it has an elementary action on a country that's not defined in the GLSK.", naId);
            return false;
        }
        return true;
    }

    private Injection<?> getInjectionSetpointIdentifiable(Action ea, Network network) {
        if (ea instanceof GeneratorAction generatorAction) {
            return (Injection<?>) network.getIdentifiable(generatorAction.getGeneratorId());
        }
        if (ea instanceof LoadAction loadAction) {
            return (Injection<?>) network.getIdentifiable(loadAction.getLoadId());
        } else {
            throw new OpenRaoException(String.format("Elementary action %s is not a generator or load action", ea.getId()));
        }
    }

    private void storeEnergyToRedispatchAndNetworkElementsToExclude(Action ea,
                                                                    Network network,
                                                                    Set<String> networkElementsToBeExcluded,
                                                                    Map<Country, Double> powerToBeRedispatched) {

        // We only keep valid injection network action so we should not get nullPointerException
        Identifiable<?> ne = getInjectionSetpointIdentifiable(ea, network);
        Country country = ((Injection<?>) ne).getTerminal().getVoltageLevel().getSubstation().get().getCountry().get();

        if (ne.getType().equals(IdentifiableType.GENERATOR)) {
            powerToBeRedispatched.merge(
                country,
                ((Generator) ne).getTargetP() - ((GeneratorAction) ea).getActivePowerValue().getAsDouble(),
                Double::sum
            );
            networkElementsToBeExcluded.add(ne.getId());
        } else if (ne.getType().equals(IdentifiableType.LOAD)) {
            powerToBeRedispatched.merge(
                country,
                -((Load) ne).getP0() + ((LoadAction) ea).getActivePowerValue().getAsDouble(),
                Double::sum
            );
            networkElementsToBeExcluded.add(ne.getId());
        }
    }

    private Country getCountryFromInjectionAction(Action ea, Network network) {
        Identifiable<?> ne = getInjectionSetpointIdentifiable(ea, network);
        return ((Injection<?>) ne).getTerminal().getVoltageLevel().getSubstation().get().getCountry().get();
    }

    private static Set<Country> getCountriesFromGlsk(ZonalData<Scalable> scalableZonalData) {
        Set<Country> glskCountries = new TreeSet<>(Comparator.comparing(Country::getName));
        if (Objects.isNull(scalableZonalData)) {
            String error = "ScalableZonalData undefined (no GLSK given)";
            BUSINESS_LOGS.error(error);
            throw new OpenRaoException(error);
        }
        for (String zone : scalableZonalData.getDataPerZone().keySet()) {
            glskCountries.add(new CountryEICode(zone).getCountry());
        }
        return glskCountries;
    }

    private void redispatchNetworkActions(Network network, EnumMap<Country, Double> powerToBeRedispatched, Set<String> networkElementsToBeExcluded, ZonalData<Scalable> scalableZonalData) {
        // Apply redispatch actions per country
        powerToBeRedispatched.forEach((country, powerToRedispatch) -> {
            BUSINESS_LOGS.info("Redispatching {} MW in {} [start]", powerToRedispatch, country);
            // Necessarily exist because we check beforehand if the country is in the GLSK
            List<Scalable> countryScalables = scalableZonalData.getDataPerZone().entrySet().stream().filter(entry -> country.equals(new CountryEICode(entry.getKey()).getCountry()))
                .map(Map.Entry::getValue).toList();
            if (countryScalables.size() > 1) {
                throw new OpenRaoException(String.format("> 1 (%s) glskPoints defined for country %s", countryScalables.size(), country));
            }
            new RedispatchAction(powerToRedispatch, networkElementsToBeExcluded, countryScalables.get(0)).apply(network);
            BUSINESS_LOGS.info("Redispatching {} MW in {} [end]", powerToRedispatch, country);
        });
    }

}
