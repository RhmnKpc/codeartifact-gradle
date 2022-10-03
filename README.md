# Codeartifact gradle plugin

Gradle plugin which authenticates against [AWS CodeArtifact](https://aws.amazon.com/es/codeartifact/) using your local credentials or access key id and access secret key obtain
the token.
## Usage
`
`Define environment variables with profile:

```
AWS_CODE_ARTIFACT_URL= https://domain-id.d.codeartifact.eu-west-1.amazonaws.com/maven/repository?profile=sample-profile
```

`
`Or define environment variables with access key and secret:

```
AWS_CODE_ARTIFACT_URL=https://domain-id.d.codeartifact.eu-west-1.amazonaws.com/maven/repository
AWS_CODE_ARTIFACT_ACCESS_KEY_ID=xxxxxxx
AWS_CODE_ARTIFACT_SECRET_ACCESS_KEY=xxxxxxxx

```

`
`In your build.gradle file:

`
`Publishing artifact and adding code artifact repository
```
plugins {
    id 'maven-publish'
    id 'io.bera.codeartifact' version '1.0.0.RELEASE'
}

repositories {
    maven {
        url "$AWS_CODE_ARTIFACT_URL"
    }
}

```

`
`Only for publishing
```
plugins {
    id 'maven-publish'
    id 'io.bera.codeartifact' version '1.0.0.RELEASE'
}


```

`
`Adding only code artifact repository without publishing
```
plugins {
    id 'io.bera.codeartifact' version '1.0.0.RELEASE'
}

repositories {
     maven {
        url "$AWS_CODE_ARTIFACT_URL"
    }
}

```