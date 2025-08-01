/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.PSTS_TO_REGULATE;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.ST_PST_REGULATION_SECTION;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class SearchTreeRaoPstRegulationParameters {
    private static final Map<String, String> DEFAULT_PSTS_TO_REGULATE = new HashMap<>();
    private Map<String, String> pstsToRegulate = DEFAULT_PSTS_TO_REGULATE;

    public Map<String, String> getPstsToRegulate() {
        return pstsToRegulate;
    }

    public void setPstsToRegulate(Map<String, String> pstsToRegulate) {
        this.pstsToRegulate = pstsToRegulate;
    }

    public static Map<String, String> getPstsToRegulate(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            Optional<SearchTreeRaoPstRegulationParameters> pstRegulationParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getPstRegulationParameters();
            return pstRegulationParameters.map(SearchTreeRaoPstRegulationParameters::getPstsToRegulate).orElse(DEFAULT_PSTS_TO_REGULATE);
        }
        return DEFAULT_PSTS_TO_REGULATE;
    }

    public static Optional<SearchTreeRaoPstRegulationParameters> load(PlatformConfig platformConfig) {
        return platformConfig.getOptionalModuleConfig(ST_PST_REGULATION_SECTION).map(
            config -> {
                SearchTreeRaoPstRegulationParameters pstRegulationParameters = new SearchTreeRaoPstRegulationParameters();
                List<String> pstsToRegulateAndMonitoredLines = config.getStringListProperty(PSTS_TO_REGULATE, List.of());
                Map<String, String> pstsToRegulate = new HashMap<>();
                for (String pstToRegulateAndMonitoredLine : pstsToRegulateAndMonitoredLines) {
                    Pattern pattern = Pattern.compile("^\\{(?<pstId>[a-zA-Z0-9_ -]+)}:\\{(?<monitoredLineId>[a-zA-Z0-9_ -]+)}$");
                    Matcher matcher = pattern.matcher(pstToRegulateAndMonitoredLine);
                    if (matcher.find()) {
                        pstsToRegulate.put(matcher.group("pstId"), matcher.group("monitoredLineId"));
                    }
                }
                pstRegulationParameters.setPstsToRegulate(pstsToRegulate);
                return pstRegulationParameters;
            });
    }
}
