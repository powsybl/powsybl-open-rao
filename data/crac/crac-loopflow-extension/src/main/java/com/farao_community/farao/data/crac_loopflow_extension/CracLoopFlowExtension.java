/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.powsybl.commons.extensions.AbstractExtension;

import java.util.List;
import java.util.Map;

/**
 * CCR Core CC needs additional parameter for Loop Flow which is used as a constraint during optimization.
 * These additional parameters (GlskProvider, Countries) are set as extension to class Crac.
 * The motivation is to keep unique interface for all Rao (close optim rao, linear rao, search tree rao, etc).
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CracLoopFlowExtension extends AbstractExtension<Crac> {
    private GlskProvider glskProvider; //glsk for loop flow
    private List<String> countriesForLoopFlow; // list of countries for loop flow

    private Map<Cnec, Map<String, Double>> ptdfs; //memorize previously calculated ptdf, set from LoopFlowComputationResult
    private Map<String, Double> netPositions; //memorize previously calculated net postions, set from LoopFlowComputationResult

    public GlskProvider getGlskProvider() {
        return glskProvider;
    }

    public void setGlskProvider(GlskProvider glskProvider) {
        this.glskProvider = glskProvider;
    }

    public List<String> getCountriesForLoopFlow() {
        return countriesForLoopFlow;
    }

    public void setCountriesForLoopFlow(List<String> countriesForLoopFlow) {
        this.countriesForLoopFlow = countriesForLoopFlow;
    }

    public Map<Cnec, Map<String, Double>> getPtdfs() {
        return ptdfs;
    }

    public void setPtdfs(Map<Cnec, Map<String, Double>> ptdfs) {
        this.ptdfs = ptdfs;
    }

    public Map<String, Double> getNetPositions() {
        return netPositions;
    }

    public void setNetPositions(Map<String, Double> netPositions) {
        this.netPositions = netPositions;
    }

    @Override
    public String getName() {
        return "CracLoopFlowExtension";
    }
}
