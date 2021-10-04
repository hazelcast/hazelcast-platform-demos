FROM hazelcast/docker-grafana-graphite:latest

# Remove unwanted dashboards in original image
RUN rm /src/dashboards/flight-telemetry.json
RUN rm /src/dashboards/Jet.json

# Add dashboards
COPY target/classes/myhome.json      /src/dashboards

# Set logon/password, override admin_user/admin_password in /etc/grafana/grafana.ini
ENV GF_SECURITY_ADMIN_USER=$MY_ADMINUSER
ENV GF_SECURITY_ADMIN_PASSWORD=$MY_ADMINPASSWORD

# Change homepage in $GF_PATHS_CONFIG, remove leading semicolong
#RUN sed 's#;default_home_dashboard_path =#default_home_dashboard_path = /etc/grafana/provisioning/dashboards/general/myhome.json#' \
# /etc/grafana/grafana.ini > /tmp/grafana.ini 
#ENV GF_PATHS_CONFIG=/tmp/grafana.ini
