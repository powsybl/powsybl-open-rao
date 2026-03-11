 package com.powsybl.openrao.data.crac.api.io.utils;

 import java.io.File;
 import java.io.IOException;
 import java.io.RandomAccessFile;
 import java.util.List;
 import org.junit.jupiter.api.Assertions;
 import org.junit.jupiter.params.ParameterizedTest;
 import org.junit.jupiter.params.provider.ValueSource;

 class BufferTest {

   private File createLargeFile(long size) throws IOException {
     var f = File.createTempFile("createLargeFile", ".test");
     f.deleteOnExit();
     try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
       raf.setLength(size);
     }
     return f;
   }

   @ParameterizedTest
   @ValueSource(longs = {1, 500, 1024})
   void testBuffer(long sizeMB) throws IOException {
     var size = sizeMB * 1024 * 1024;
     var buffers = List.of(BufferSize.UNBUFFERED, BufferSize.SMALL, BufferSize.LARGE,
         BufferSize.EXTRA_LARGE);
     var inFile = createLargeFile(size);
     try {
       for (var bf : buffers) {
         try (var out = TmpFile.create(".testBuffer", BufferSize.MEDIUM)) {
           //when
           var inReader = SafeFileReader.create(inFile, bf);
           var outWriter = SafeFileReader.create(out.getTempFile(), BufferSize.MEDIUM);
           inReader.withReadStreamVoid(is -> outWriter.withWriteStream(is::transferTo));
           //then
           Assertions.assertEquals(size, IOUtils.getSafeFileSize(out.getTempFile()));
         }

       }
     } finally {
       IOUtils.safeDelete(inFile);
     }
   }

 }
