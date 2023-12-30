Helm chart for New Relic Java APM example application.

The deployments use the example client and server code from the official docs.

Client: https://docs.newrelic.com/docs/apm/agents/java-agent/api-guides/java-agent-api-instrumenting-example-app-external-datastore-calls-dt/#complete-client

Server: https://docs.newrelic.com/docs/apm/agents/java-agent/api-guides/java-agent-api-instrumenting-example-app-external-datastore-calls-dt/#server-complete

After checking out this repo, you will need to build the Docker images required for the chart:

```
cd /path/to/newrelic-apm-kubernetes-examples/java/docker
docker build -f Dockerfile-server -t your-repo/newrelic-server:latest .
docker build -f Dockerfile-client -t your-repo/newrelic-client:latest .
docker login
docker push your-repo/newrelic-server:latest
docker push your-repo/newrelic-client:latest
```
