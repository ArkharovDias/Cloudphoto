package org.example.services;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class IOService {

    private Class sourceClass;

    public IOService(Class sourceClass){
        this.sourceClass = sourceClass;
    }

    public String readInputStreamAsString(String fileName) throws IOException {
        return IOUtils.toString(getInputStream(fileName), StandardCharsets.UTF_8);
    }

    public InputStream getInputStream(String fileName){
        return sourceClass.getResourceAsStream(fileName);
    }
}
