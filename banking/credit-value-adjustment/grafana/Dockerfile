FROM hazelcast/docker-grafana-graphite:latest

# Remove unwanted dashboards in original image
RUN rm /src/dashboards/flight-telemetry.json
RUN rm /src/dashboards/Jet.json

# Add dashboards for this demo
COPY target/classes/CVA-Map-stats-dashboard.json      /src/dashboards
