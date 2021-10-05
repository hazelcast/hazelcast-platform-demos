FROM hazelcast/docker-grafana-graphite:latest

# Remove unwanted dashboards in original image
RUN rm /src/dashboards/flight-telemetry.json
RUN rm /src/dashboards/Jet.json

ARG MY_ADMINUSER
ARG MY_ADMINPASSWORD

# Add dashboards
COPY target/classes/myhome.json      /src/dashboards

#RUN sed 's#;default_home_dashboard_path =#default_home_dashboard_path = /etc/grafana/provisioning/dashboards/general/myhome.json#' \

# For older Grafana, use /opt/grafana/conf/custom.ini to set logon, password and initial homepage
RUN sed 's/;admin_user = admin/admin_user = '$MY_ADMINUSER'/' < /opt/grafana/conf/custom.ini \
 | sed 's/;admin_password = admin/admin_password = '$MY_ADMINPASSWORD'/' \
 > /tmp/custom.ini
RUN cat /tmp/custom.ini > /opt/grafana/conf/custom.ini  

# Wizzy needs password also
RUN sed 's#"username": "admin"#"username": "'$MY_ADMINUSER'"#' < /src/conf/wizzy.json \
 | sed 's#"password": "admin"#"password": "'$MY_ADMINPASSWORD'"#' \
 > tmp/wizzy.json
RUN cat /tmp/wizzy.json > /src/conf/wizzy.json