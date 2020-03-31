# Overview
This app logs in to the AWS Console using Synapse as the OpenID Connect
(OIDC) identity provider

## Configurations
The app must be configured with five parameters which can be passed as
properties, environment variables, or a properties file on the class
loader search path called [global.properties](src/main/resources/global.properties)
like so:

```
SYNAPSE_OAUTH_CLIENT_ID=xxxxxx
SYNAPSE_OAUTH_CLIENT_SECRET=xxxxxx
TEAM_TO_ROLE_ARN_MAP=[{"teamId":"xxxxxx","roleArn":"arn:aws:iam::xxxxxx:role/ServiceCatalogEndusers"}, ...]
AWS_REGION=us-east-1
SESSION_TIMEOUT_SECONDS=43200
USER_CLAIMS=userid
```

## Team to role map
This defines the mapping between the synapse team and the AWS role. When
mapping team ID to AWS Role, this app' uses the first match it encounters,
iterating through the team/role list in the order given. 

## Claims
The `USER_CLAIMS` config is a comma separated list of claims from the list of
available claims, given here:
https://rest-docs.synapse.org/rest/org/sagebionetworks/repo/model/oauth/OIDCClaimName.html

For example: setting `USER_CLAIMS=userid,email` will display
`ServiceCatalogEndusers/1234567:joe.smith@gmail.com` in AWS. 

## Building the app
This is a java application which we build with standard [apache maven](https://maven.apache.org/what-is-maven.html)
tooling. AWS beanstalk requires files to be in a
[standard structure](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-tomcat-platform-directorystructure.html).

```buildoutcfg
mvn clean package
```
## Deployments
We deploy this application to an existing [AWS beanstalk](https://aws.amazon.com/elasticbeanstalk/)
container which is defined by our our cloudformation templates in our
[synapse-login-aws-infra](https://github.com/Sage-Bionetworks/synapse-login-aws-infra) repo.

## Continuous Integration
We have configured Travis to deploy the application using the
[AWS EB CLI](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/eb-cli3.html)

## Contributions
Contributions are welcome

## Issues
* https://sagebionetworks.jira.com/projects/SC

## Builds
* https://travis-ci.org/Sage-Bionetworks/synapse-login-scipoolprod
