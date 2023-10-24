package com.moelholm.tools.mediaorganizer.filesystem;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

@Component
@ConditionalOnProperty(name = "mediaorganizer.fileSystemType", havingValue = "dropbox")
public class DropboxFileSystem implements FileSystem {

    private final DropboxFileSystemProperties dropboxAccessToken;

    public DropboxFileSystem(DropboxFileSystemProperties dropboxAccessToken) {
        this.dropboxAccessToken = dropboxAccessToken;
    }

    @Override
    public boolean existingDirectory(Path pathToTest) {
        try {
            var dropboxPathToTest = toAbsoluteDropboxPath(pathToTest);
            var dropboxRequest = new DropboxFileRequest(dropboxPathToTest);
            var metaData =
                    postToDropboxAndGetResponse(
                            "/files/get_metadata", dropboxRequest, DropboxFile.class);
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
            var dropboxPath = toAbsoluteDropboxPath(from);
            var listFolderResponse =
                    postToDropboxAndGetResponse(
                            "/files/list_folder",
                            new DropboxFileRequest(dropboxPath),
                            DropboxListFolderResponse.class);
            var allFiles = new ArrayList<>(listFolderResponse.dropboxFiles());
            while (listFolderResponse.more()) {
                listFolderResponse =
                        postToDropboxAndGetResponse(
                                "/files/list_folder/continue",
                                new DropboxCursorRequest(listFolderResponse.cursor()),
                                DropboxListFolderResponse.class);
                allFiles.addAll(listFolderResponse.dropboxFiles());
            }
            return allFiles.stream().map(DropboxFile::pathLower).map(Paths::get);
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
                String.format("%s [A%s]", e.getMessage(), e.getResponseBodyAsString()), e);
    }

    private RestTemplate createRestTemplate() {
        return new RestTemplateBuilder().interceptors(createHeadersInterceptor()).build();
    }

    private ClientHttpRequestInterceptor createHeadersInterceptor() {
        return (request, bytes, execution) -> {
            request.getHeaders()
                    .put(
                            "Authorization",
                            singletonList(String.format("Bearer %s", dropboxAccessToken.accessToken())));
            request.getHeaders().put("Content-Type", singletonList("application/json"));
            return execution.execute(request, bytes);
        };
    }

    private <T> T postToDropboxAndGetResponse(String path, Object arg, Class<T> responseType)
            throws IOException {

        var url = String.format("https://api.dropboxapi.com/2%s", path);
        var resultJsonString = createRestTemplate().postForObject(url, arg, String.class);

        // Note (!) : this is a workaround for an ...ahem temporary issue... with
        // getting the Java POJO object directly from the RestTemplate
        var mapper = new ObjectMapper();
        return mapper.readValue(resultJsonString, responseType);
    }

    private static String toAbsoluteDropboxPath(Path pathToTest) {
        var pathAsString = pathToTest.toString();
        return pathAsString.startsWith("/") ? pathAsString : String.format("/%s", pathAsString);
    }

    public record DropboxFileRequest(String path) {
    }

    public record DropboxCursorRequest(String cursor) {
    }

    public record DropboxMoveRequest(
            @JsonProperty("from_path")
            String fromPath,
            @JsonProperty("to_path")
            String toPath
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DropboxListFolderResponse(
            @JsonProperty("entries")
            List<DropboxFile> dropboxFiles,
            @JsonProperty("has_more")
            boolean more,
            @JsonProperty("cursor")
            String cursor) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DropboxFile(
            @JsonProperty(".tag") String tag,
            @JsonProperty("path_lower") String pathLower) {
        public boolean isDirectory() {
            return "folder".equalsIgnoreCase(tag);
        }
    }

    @ConfigurationProperties(prefix = "dropbox")
    public record DropboxFileSystemProperties(String accessToken) {
    }
}
