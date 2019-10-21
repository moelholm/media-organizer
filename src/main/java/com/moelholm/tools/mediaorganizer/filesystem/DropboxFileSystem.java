package com.moelholm.tools.mediaorganizer.filesystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class DropboxFileSystem implements FileSystem {

    @Value("${dropbox.accessToken}")
    private String dropboxAccessToken;

    @Override
    public boolean existingDirectory(Path pathToTest) {
        try {
            var dropboxPathToTest = toAbsoluteDropboxPath(pathToTest);
            var dropBoxRequest = new DropboxFileRequest(dropboxPathToTest);
            var metaData =
                    postToDropboxAndGetResponse(
                            "/files/get_metadata", dropBoxRequest, DropboxFile.class);
            return metaData.isDirectory();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) { // -(file-does-not-exist)-
                return false;
            }
            throw asRuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Stream<Path> streamOfAllFilesFromPath(Path from) {
        try {
            var dropboxPathToTest = toAbsoluteDropboxPath(from);
            var dropBoxRequest = new DropboxFileRequest(dropboxPathToTest);
            var listFolderResponse =
                    postToDropboxAndGetResponse(
                            "/files/list_folder", dropBoxRequest, DropboxListFolderResponse.class);
            return listFolderResponse.getDropboxFiles().stream() //
                    .map(DropboxFile::getPathLower) //
                    .map(Paths::get);
        } catch (HttpClientErrorException e) {
            throw asRuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void move(Path from, Path to) throws IOException {
        try {
            var fromDropboxPath = toAbsoluteDropboxPath(from);
            var toDropboxPath = toAbsoluteDropboxPath(to);
            var dropBoxRequest = new DropboxMoveRequest(fromDropboxPath, toDropboxPath);
            postToDropboxAndGetResponse("/files/move", dropBoxRequest, DropboxFile.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new FileAlreadyExistsException(to.toString());
            }
            throw asRuntimeException(e);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private RuntimeException asRuntimeException(HttpClientErrorException e) {
        return new RuntimeException(
                String.format("%s [%s]", e.getMessage(), e.getResponseBodyAsString()), e);
    }

    private RestTemplate createRestTemplate() {
        var restTemplate = new RestTemplate();
        restTemplate.setInterceptors(
                Collections.singletonList(
                        (request, bytes, execution) -> {
                            request.getHeaders()
                                    .put(
                                            "Authorization",
                                            Collections.singletonList(
                                                    String.format(
                                                            "Bearer %s", dropboxAccessToken)));
                            request.getHeaders()
                                    .put(
                                            "Content-Type",
                                            Collections.singletonList("application/json"));
                            return execution.execute(request, bytes);
                        }));
        return restTemplate;
    }

    private <T> T postToDropboxAndGetResponse(String path, Object arg, Class<T> responseType)
            throws IOException {

        var resultJsonString =
                createRestTemplate()
                        .postForObject(
                                String.format("https://api.dropboxapi.com/2%s", path),
                                arg,
                                String.class);

        // Note (!) : this is a workaround for an ...ahem temporary issue... with
        // getting the Java
        // POJO
        // object directly from the RestTemplate
        var mapper = new ObjectMapper();
        return mapper.readValue(resultJsonString, responseType);
    }

    private static String toAbsoluteDropboxPath(Path pathToTest) {
        var pathAsString = pathToTest.toString();
        return pathAsString.startsWith("/") ? pathAsString : String.format("/%s", pathAsString);
    }

    public static class DropboxFileRequest {

        private String path;

        public DropboxFileRequest(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public static class DropboxMoveRequest {

        @JsonProperty("from_path")
        private String fromPath;

        @JsonProperty("to_path")
        private String toPath;

        public DropboxMoveRequest(String fromPath, String toPath) {
            this.fromPath = fromPath;
            this.toPath = toPath;
        }

        public String getFromPath() {
            return fromPath;
        }

        public String getToPath() {
            return toPath;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DropboxListFolderResponse {

        @JsonProperty("entries")
        private List<DropboxFile> dropboxFiles;

        @Override
        public String toString() {
            return dropboxFiles.stream()
                    .map(DropboxFile::toString)
                    .collect(Collectors.joining("\n"));
        }

        public List<DropboxFile> getDropboxFiles() {
            return dropboxFiles;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DropboxFile {

        @JsonProperty(".tag")
        private String tag;

        @JsonProperty("path_lower")
        private String pathLower;

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this);
        }

        public boolean isDirectory() {
            return "folder".equalsIgnoreCase(tag);
        }

        public String getPathLower() {
            return pathLower;
        }
    }
}
