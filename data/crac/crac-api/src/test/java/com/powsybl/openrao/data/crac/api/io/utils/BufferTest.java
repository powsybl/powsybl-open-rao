 package com.powsybl.openrao.data.crac.api.io.utils;

 import java.io.File;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.RandomAccessFile;
 import java.util.List;
 import org.junit.jupiter.params.ParameterizedTest;
 import org.junit.jupiter.params.provider.ValueSource;

 public class BufferTest {

   private File createLargeFile(long size) throws IOException {
     var f = File.createTempFile("createLargeFile", "test");
     f.deleteOnExit();
     try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
       raf.setLength(size);
     }
     return f;
   }

   @ParameterizedTest
   @ValueSource(longs = {1024 * 1024, 500 * 1024 * 1024, 1024 * 1024 * 1024})
   public void testBuffer(long size) throws IOException {
     File f = null;
     try {
       f = createLargeFile(size);
       for (var bf : List.of(BufferSize.UNBUFFERED, BufferSize.SMALL, BufferSize.LARGE,
           BufferSize.EXTRA_LARGE)) {
         SafeFileReader.create(f, bf)
             .withReadStream(is -> is.transferTo(OutputStream.nullOutputStream()));
       }
     } finally {
       if (null != f)
          f.delete();
     }
   }

 }
