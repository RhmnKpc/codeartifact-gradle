package io.github.rhmnkpc.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication

class CodeArtifactPlugin implements Plugin<Project> {
    private Logger logger

    void apply(Project project) {
        this.logger = project.logger;
        Provider<CodeArtifactToken> serviceProvider = project
                .getGradle()
                .getSharedServices()
                .registerIfAbsent('codeartifact-token', CodeArtifactToken.class, {})

        def awsAccessKey = project.properties["AWS_CODE_ARTIFACT_ACCESS_KEY_ID"]
        def awsSecretAccessKey = project.properties["AWS_CODE_ARTIFACT_SECRET_ACCESS_KEY"]
        def awsUrl = project.properties["AWS_CODE_ARTIFACT_URL"]
        String token = serviceProvider.get().getToken(URI.create(awsUrl), awsAccessKey, awsSecretAccessKey)

        setupCodeartifactRepositories(project, token)
        setupCodeartifactRepositoriesByUrl(project, token)
    }

    private void setupCodeartifactRepositories(Project project, String token) {
        if (!project.allprojects.repositories.metaClass.respondsTo(project.repositories, 'codeartifact', String, String, Object)) {
            project.repositories.metaClass.codeartifact = { String repoUrl, String profile = 'default', def closure = null ->
                logger.debug "Getting token for $repoUrl in profile $profile"
                delegate.maven {
                    url repoUrl
                    credentials {
                        username 'aws'
                        password token
                    }
                }
                if (closure) {
                    closure.delegate = delegate
                    closure()
                }
            }
        }
    }

    private void setupCodeartifactRepositoriesByUrl(Project project, String token) {
        project.afterEvaluate({ p ->
            configRepositories(p.repositories, token)
            p.plugins.withId('maven-publish', { publishPlugin ->
                p.extensions.configure('publishing', { publishing ->
                    configPackaging(publishing.publications, project)
                    configRepositories(publishing.getRepositories(), token)
                })
            })
        })
    }

    private void configRepositories(RepositoryHandler repositories, String token) {
        ListIterator it = repositories.listIterator()
        while (it.hasNext()) {
            def artifactRepository = it.next()
            if (artifactRepository instanceof MavenArtifactRepository) {
                MavenArtifactRepository mavenRepo = (MavenArtifactRepository) artifactRepository;
                URI repoUri = mavenRepo.getUrl()
                if (isCodeArtifactUri(repoUri) && areCredentialsEmpty(mavenRepo)) {
                    logger.info('Getting token for {}', repoUri.toString())
                    mavenRepo.credentials({
                        username 'aws'
                        password token
                    })

                    mavenRepo.setUrl(repoUri)
                }
            }
        }
    }

    private void configPackaging(PublicationContainer publications, Project project) {
        MavenPublication mavenPublication = publications.create("publish", MavenPublication.class)
        mavenPublication.artifactId = "$project.name"
        mavenPublication.groupId = "$project.group"
        mavenPublication.version = "$project.version"
        mavenPublication.from(project.components["java"])

    }

    private boolean areCredentialsEmpty(MavenArtifactRepository mavenRepo) {
        return mavenRepo.getCredentials().getPassword() == null && mavenRepo.getCredentials().getUsername() == null
    }

    private boolean isCodeArtifactUri(URI uri) {
        return uri.toString().matches('(?i).+\\.codeartifact\\..+\\.amazonaws\\..+')
    }
}