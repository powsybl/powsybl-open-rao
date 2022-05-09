/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;

import java.util.Map;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class GlobalRemedialActionLimitationParameters {

    private final Integer maxCurativeRa;
    private final Integer maxCurativeTso;
    private final Map<String, Integer> maxCurativePstPerTso;
    private final Map<String, Integer> maxCurativeTopoPerTso;
    private final Map<String, Integer> maxCurativeRaPerTso;

    public GlobalRemedialActionLimitationParameters(Integer maxCurativeRa,
                                                    Integer maxCurativeTso,
                                                    Map<String, Integer> maxCurativePstPerTso,
                                                    Map<String, Integer> maxCurativeTopoPerTso,
                                                    Map<String, Integer> maxCurativeRaPerTso) {
        this.maxCurativeRa = maxCurativeRa;
        this.maxCurativeTso = maxCurativeTso;
        this.maxCurativePstPerTso = maxCurativePstPerTso;
        this.maxCurativeTopoPerTso = maxCurativeTopoPerTso;
        this.maxCurativeRaPerTso = maxCurativeRaPerTso;
    }

    public Integer getMaxCurativeRa() {
        return maxCurativeRa;
    }

    public Integer getMaxCurativeTso() {
        return maxCurativeTso;
    }

    public Map<String, Integer> getMaxCurativePstPerTso() {
        return maxCurativePstPerTso;
    }

    public Map<String, Integer> getMaxCurativeTopoPerTso() {
        return maxCurativeTopoPerTso;
    }

    public Map<String, Integer> getMaxCurativeRaPerTso() {
        return maxCurativeRaPerTso;
    }

    public static GlobalRemedialActionLimitationParameters buildFromRaoParameters(RaoParameters raoParameters) {

        /*
        GlobalRemedialActionLimitationParameters only contains values that are not dependant of the perimeter. It can
        therefore be instantiated from a RaoParameters alone.
         */
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters == null) {
            throw new FaraoException("RaoParameters must contain SearchTreeRaoParameters when running a SearchTreeRao");
        }

        return new GlobalRemedialActionLimitationParameters(searchTreeRaoParameters.getMaxCurativeRa(),
            searchTreeRaoParameters.getMaxCurativeTso(),
            searchTreeRaoParameters.getMaxCurativePstPerTso(),
            searchTreeRaoParameters.getMaxCurativeTopoPerTso(),
            searchTreeRaoParameters.getMaxCurativeRaPerTso());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GlobalRemedialActionLimitationParameters that = (GlobalRemedialActionLimitationParameters) o;
        return Objects.equals(maxCurativeRa, that.maxCurativeRa) && Objects.equals(maxCurativeTso, that.maxCurativeTso) && Objects.equals(maxCurativePstPerTso, that.maxCurativePstPerTso) && Objects.equals(maxCurativeTopoPerTso, that.maxCurativeTopoPerTso) && Objects.equals(maxCurativeRaPerTso, that.maxCurativeRaPerTso);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxCurativeRa, maxCurativeTso, maxCurativePstPerTso, maxCurativeTopoPerTso, maxCurativeRaPerTso);
    }
}
