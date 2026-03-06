package com.powsybl.openrao.data.crac.util;

import java.io.File;
import java.net.URISyntaxException;

public abstract class TestBase {

  public File getResourceAsFile(String file) {
    try {
      var url = getClass().getResource(file);
      if (url == null) {
        throw new RuntimeException("Resource not found on classpath: " + file);
      }
      return new File(url.toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException("Error loading resource: " + file, e);
    }
  }

}