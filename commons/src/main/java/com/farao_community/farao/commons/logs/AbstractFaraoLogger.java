package com.farao_community.farao.commons.logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFaraoLogger implements FaraoLogger {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(format, arguments);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(format, arguments);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(format, arguments);
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(format, arguments);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(format, arguments);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }
}
