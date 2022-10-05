
package io.github.rhmnkpc.codeartifact;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.CodeartifactClientBuilder;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse;

public class CodeArtifactTokenFactory {

    public static GetAuthorizationTokenResponse getAuthorizationToken(CodeArtifactUrl codeArtifactUrl, String profileName) {
        CodeartifactClientBuilder builder = CodeartifactClient.builder()
                .region(Region.of(codeArtifactUrl.getRegion()));

        if (profileName != null) {
            builder = builder.credentialsProvider(ProfileCredentialsProvider.create(profileName));
        } else {
            AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(System.getProperty("AWS_CODE_ARTIFACT_ACCESS_KEY_ID"), System.getProperty("AWS_CODE_ARTIFACT_SECRET_ACCESS_KEY"));
            builder = builder.credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials)).region(Region.of(codeArtifactUrl.getRegion()));
        }
        CodeartifactClient client = builder.build();

        return client
                .getAuthorizationToken(req -> req.domain(codeArtifactUrl.getArtifactDomain()).domainOwner(codeArtifactUrl.getArtifactOwner()));
    }
}
