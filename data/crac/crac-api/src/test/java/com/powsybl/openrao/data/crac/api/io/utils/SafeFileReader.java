package com.powsybl.openrao.data.crac.api.io.utils;

import com.powsybl.openrao.data.crac.api.TestBase;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader.RunException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SafeFileReaderTest extends TestBase {

  @Test
  void should_create_correctly() {
    var file = getResourceAsFile("/com/powsybl/openrao/data/crac/api/io/utils/small_file.txt");
    var sfr = SafeFileReader.create(file, BufferSize.DEFAULT);
    Assertions.assertEquals(45, sfr.getFileSize());
    Assertions.assertEquals("small_file.txt", sfr.getFileName());
    Assertions.assertTrue(sfr.hasFileExtension("txt"));
    Assertions.assertFalse(sfr.hasFileExtension("xyz"));
  }

  @Test
  void should_read_correctly() {
    //when
    var file = getResourceAsFile("/com/powsybl/openrao/data/crac/api/io/utils/small_file.txt");
    var reader = SafeFileReader.create(file, BufferSize.DEFAULT);
    //then
    byte[] result1 = reader.withReadStream(InputStream::readAllBytes);
    Assertions.assertEquals("a very small file, for testing only. EOF here", new String(result1));
    //and then
    byte[] result2 = reader.withReadStream(InputStream::readAllBytes);
    Assertions.assertEquals("a very small file, for testing only. EOF here", new String(result2));
  }

  @Test
  void should_handle_read_error() {
    //when
    var sfr = SafeFileReader.create(new File("unknown.txt"), BufferSize.DEFAULT);
    Assertions.assertEquals(-1, sfr.getFileSize());
    Assertions.assertEquals("unknown.txt", sfr.getFileName());
    //then
    var ex = Assertions.assertThrowsExactly(UncheckedIOException.class, () -> sfr.withReadStream(InputStream::readAllBytes));
    Assertions.assertEquals("Error opening read stream: unknown.txt", ex.getMessage());
  }

  @Test
  void should_handle_read_run_error() {
    //when
    var file = getResourceAsFile("/com/powsybl/openrao/data/crac/api/io/utils/small_file.txt");
    var sfr = SafeFileReader.create(file, BufferSize.DEFAULT);
    //then
    var ex = Assertions.assertThrowsExactly(RunException.class, () -> sfr.withReadStreamVoid( is -> {
      throw new IOException("testing"); }
    ));
    Assertions.assertEquals("java.io.IOException: testing", ex.getMessage());
  }

}