# Overview
This app logs in to the AWS Console using Synapse as the OpenID Connect
(OIDC) identity provider

# Configurations
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

# Team to role map
This defines the mapping between the synapse team and the AWS role. When
mapping team ID to AWS Role, this app' uses the first match it encounters,
iterating through the team/role list in the order given. 


# Claims
The `USER_CLAIMS` config is a comma separated list of claims from the list of
available claims, given here:
https://rest-docs.synapse.org/rest/org/sagebionetworks/repo/model/oauth/OIDCClaimName.html

For example: setting `USER_CLAIMS=userid,email` will display
`ServiceCatalogEndusers/1234567:joe.smith@gmail.com` in AWS. 
