/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public final class MCDContextWrapper {

    private MCDContextWrapper() {

    }

    public static Runnable wrapWithMdcContext(Runnable task) {
        //save the current MDC context
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            setMDCContext(contextMap);
            try {
                task.run();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
            }
        };
    }

    public static <T> Callable<T> wrapWithMdcContext(Callable<T> task) {
        //save the current MDC context
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            setMDCContext(contextMap);
            try {
                return task.call();
            } finally {
                // once the task is complete, clear MDC
                MDC.clear();
            }
        };
    }

    public static void setMDCContext(Map<String, String> contextMap) {
        MDC.clear();
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
    }
}
