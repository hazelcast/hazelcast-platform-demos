FROM grafana/grafana:8.4.3

# Args to pass to ENV, set by dockerfile-maven-plugin.
ARG MY_ADMINUSER
ARG MY_ADMINPASSWORD

# Add Prometheus datasource
COPY target/classes/datasources     /etc/grafana/provisioning/datasources

# Add dashboards for this demo
COPY target/classes/dashboards      /etc/grafana/provisioning/dashboards

# Set logon/password, override admin_user/admin_password in /etc/grafana/grafana.ini
ENV GF_SECURITY_ADMIN_USER=$MY_ADMINUSER
ENV GF_SECURITY_ADMIN_PASSWORD=$MY_ADMINPASSWORD

# Change homepage in $GF_PATHS_CONFIG, remove leading semicolon
RUN sed 's#;default_home_dashboard_path =#default_home_dashboard_path = /etc/grafana/provisioning/dashboards/general/myhome.json#' \
 /etc/grafana/grafana.ini > /tmp/grafana.ini 
ENV GF_PATHS_CONFIG=/tmp/grafana.ini
