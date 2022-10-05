
package io.github.rhmnkpc.codeartifact;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters.None;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

public class CodeArtifactToken implements BuildService<None> {

    private final ConcurrentHashMap<String, String> tokensCache = new ConcurrentHashMap<>();

    public String getToken(URI uri) throws MalformedURLException {
        return getToken(uri.toString(), getProfile(uri));
    }

    private String getToken(String uri, String profile) throws MalformedURLException {
        CodeArtifactUrl codeArtifactUrl = CodeArtifactUrl.of(uri);
        return tokensCache.computeIfAbsent("AWS_CODE_ARTIFACT_TOKEN", tc -> CodeArtifactTokenFactory.getAuthorizationToken(codeArtifactUrl, profile).authorizationToken());

    }

    @Override
    public None getParameters() {
        return null;
    }

    private String getProfile(URI uri) {
        return UriUtils.parseQueryParams(uri.getQuery()).getOrDefault("profile", null);
    }
}
