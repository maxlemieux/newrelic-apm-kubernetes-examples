Helm chart for New Relic Java APM example application.

The deployments use the example client and server code from the official docs.

Client: https://docs.newrelic.com/docs/apm/agents/java-agent/api-guides/java-agent-api-instrumenting-example-app-external-datastore-calls-dt/#complete-client

Server: https://docs.newrelic.com/docs/apm/agents/java-agent/api-guides/java-agent-api-instrumenting-example-app-external-datastore-calls-dt/#server-complete

After checking out this repo, you will need to build the Docker images required for the chart:

```
cd ~
git clone git@github.com:maxlemieux/newrelic-apm-kubernetes-examples.git

cd newrelic-apm-kubernetes-examples/java/docker

docker login myregistry.azurecr.io

# Start the cross platform build system
docker buildx create --name mybuilder --use
docker buildx inspect --bootstrap

# Update this command to use your actual registry name
docker buildx build --platform linux/amd64,linux/arm64 -t myregistry.azurecr.io/java-example-client:latest -f docker/Dockerfile-client . --push
docker buildx build --platform linux/amd64,linux/arm64 -t myregistry.azurecr.io/java-example-server:latest -f docker/Dockerfile-server . --push

