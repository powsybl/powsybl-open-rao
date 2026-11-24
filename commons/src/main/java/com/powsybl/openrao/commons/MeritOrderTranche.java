/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

/**
 * One step of a merit-order GLSK: the generator may move its targetP within [minP, maxP].
 *
 * @author Rodolphe Griset {@literal <rodolphe.griset at artelys.com>}
 */
public record MeritOrderTranche(String generatorId, double minP, double maxP) { }
