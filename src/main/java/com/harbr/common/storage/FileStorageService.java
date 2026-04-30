package com.harbr.common.storage;

import java.io.InputStream;

public interface FileStorageService {

    String store(String directory, String filename, InputStream content, String contentType);

    void delete(String url);
}