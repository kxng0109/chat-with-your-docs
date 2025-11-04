package io.github.kxng0109.chatwithdocs;

import org.springframework.mock.web.MockMultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class TestUtils {
    private TestUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static MockMultipartFile createValidPdfMockFile() {

        String fileName = "test-document.pdf";

        InputStream inputStream = TestUtils.class.getClassLoader().getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new RuntimeException(new FileNotFoundException("Test file '" + fileName + "' not found in resources."));
        }

        try {
            byte[] fileContent = inputStream.readAllBytes();
            inputStream.close();

            return new MockMultipartFile(
                    "file",
                    fileName,
                    "application/pdf",
                    fileContent
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to read test file: " + fileName, e);
        }
    }
}
