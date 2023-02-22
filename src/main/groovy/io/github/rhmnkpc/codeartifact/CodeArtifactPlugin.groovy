package io.github.rhmnkpc.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.AuthenticationContainer
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.artifacts.repositories.DefaultUrlArtifactRepository
import org.gradle.api.internal.artifacts.repositories.metadata.MavenMutableModuleMetadataFactory
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.internal.authentication.DefaultAuthenticationContainer
import org.gradle.internal.component.external.model.ModuleComponentArtifactMetadata
import org.gradle.internal.hash.ChecksumService
import org.gradle.internal.instantiation.InstantiatorFactory
import org.gradle.internal.isolation.IsolatableFactory
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.resource.local.FileResourceRepository
import org.gradle.internal.resource.local.LocallyAvailableResourceFinder

import javax.inject.Inject

class CodeArtifactPlugin implements Plugin<Project> {

    private final FileResolver fileResolver;
    private final RepositoryTransportFactory transportFactory;
    private final DefaultUrlArtifactRepository.Factory urlArtifactRepository;
    private final LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata> locallyAvailableResourceFinder;
    private final FileResourceRepository fileResourceRepository;
    private final MavenMutableModuleMetadataFactory metadataFactory;
    private final IsolatableFactory isolatableFactory;
    private final ChecksumService checksumService;
    private final InstantiatorFactory instantiatorFactory;
    private final FeaturePreviews featurePreviews;
    private MavenArtifactRepository codeArtifactRepository;
    private final ObjectFactory objectFactory;
    private final ProviderFactory providerFactory;

    private final Instantiator instantiator
    private final CollectionCallbackActionDecorator callbackDecorator

    @Inject
    CodeArtifactPlugin(Instantiator instantiator, CollectionCallbackActionDecorator callbackDecorator, ObjectFactory objectFactory, ProviderFactory providerFactory, FileResolver fileResolver, RepositoryTransportFactory transportFactory, DefaultUrlArtifactRepository.Factory urlArtifactRepositoryFactory, LocallyAvailableResourceFinder<ModuleComponentArtifactMetadata> locallyAvailableResourceFinder, FileResourceRepository fileResourceRepository, MavenMutableModuleMetadataFactory metadataFactory, IsolatableFactory isolatableFactory, ChecksumService checksumService, InstantiatorFactory instantiatorFactory, FeaturePreviews featurePreviews) {
        this.fileResolver = fileResolver
        this.transportFactory = transportFactory
        this.urlArtifactRepository = urlArtifactRepositoryFactory
        this.locallyAvailableResourceFinder = locallyAvailableResourceFinder
        this.fileResourceRepository = fileResourceRepository
        this.metadataFactory = metadataFactory
        this.isolatableFactory = isolatableFactory
        this.checksumService = checksumService
        this.instantiatorFactory = instantiatorFactory
        this.featurePreviews = featurePreviews
        this.objectFactory = objectFactory;
        this.providerFactory = providerFactory

    }

    @Override
    void apply(Project project) {
        Provider<CodeArtifactToken> codeArtifactTokenProvider = project.getGradle().getSharedServices().registerIfAbsent("codeartifact-token", CodeArtifactToken.class, {})
        AuthenticationContainer authenticationContainer = new DefaultAuthenticationContainer(instantiator, callbackDecorator);

        this.codeArtifactRepository = new CodeArtifactRepository(fileResolver, transportFactory, locallyAvailableResourceFinder, instantiatorFactory, null,
                null, null, authenticationContainer, null, fileResourceRepository, metadataFactory, isolatableFactory,
                objectFactory, urlArtifactRepository, checksumService, providerFactory, featurePreviews, project.properties["AWS_CODE_ARTIFACT_URL"]);
        def awsAccessKey = project.properties["AWS_CODE_ARTIFACT_ACCESS_KEY_ID"]
        def awsSecretAccessKey = project.properties["AWS_CODE_ARTIFACT_SECRET_ACCESS_KEY"]
        String token = codeArtifactTokenProvider.get().getToken(codeArtifactRepository.getUrl(), awsAccessKey, awsSecretAccessKey)
        codeArtifactRepository.credentials({
            it.username = "aws"
            it.password = token
        })
        setup(project)
        configProjectRepositories(project, token)
    }

    void setup(Project project) {
        project.afterEvaluate({ p ->
            p.plugins.withId('maven-publish', {
                publishPlugin ->
                    p.extensions.configure('publishing', {
                        publishing ->
                            configRepositories(publishing.repositories)
                            configPackaging(publishing.publications, project)
                    })
            })
        })
    }

    static void configPackaging(PublicationContainer publications, Project project) {
        MavenPublication mavenPublication = publications.create("publish", MavenPublication.class)
        mavenPublication.artifactId = "$project.name"
        mavenPublication.groupId = "$project.group"
        mavenPublication.version = "$project.version"
        mavenPublication.from(project.components["java"])

    }

    void configRepositories(RepositoryHandler repositories) {
        repositories.add(codeArtifactRepository)
    }

    void configProjectRepositories(Project project, String token) {
        if (!project.repositories.metaClass.respondsTo(project.repositories, 'codeartifact', String, String, Object)) {
            project.repositories.metaClass.codeartifact = { String repoUrl, String profile = 'default', def closure = null ->
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
}
