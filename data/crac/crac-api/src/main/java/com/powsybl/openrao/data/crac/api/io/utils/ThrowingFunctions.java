
package com.powsybl.openrao.data.crac.api.io.utils;

/**
 *
 * @author Georg Haider {@literal <georg.haider at artelys.com>}
 *
 */

public interface ThrowingFunctions {

    @FunctionalInterface
    interface Runner<T, R> {
        R run(T t) throws Exception; //NOSONAR S112
    }

    @FunctionalInterface
    interface VoidRunner<T> {
        void run(T t) throws Exception; //NOSONAR S112
    }

}
