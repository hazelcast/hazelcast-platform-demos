FROM hazelcast/docker-grafana-graphite:latest

ARG MY_ADMINUSER
ARG MY_ADMINPASSWORD

# Remove unwanted dashboards in original image
RUN rm /src/dashboards/flight-telemetry.json
RUN rm /src/dashboards/Jet.json

# Add Prometheus datasource
COPY target/classes/datasources/prometheus.json                          /src/datasources

# Add dashboards for this demo
COPY target/classes/dashboards/general/CVA-Map-stats-dashboard.json      /src/dashboards
COPY target/classes/dashboards/general/myhome.json                       /src/dashboards

# Admin logon/password
#############################################
# TODO Below does not work. Old Grafana 4.6.1
#############################################
#RUN echo admin_user=$MY_ADMINUSER >> /opt/grafana/conf/custom.ini
#RUN echo admin_password=$MY_ADMINPASSWORD >> /opt/grafana/conf/custom.ini
#RUN cd /src && wizzy init && wizzy set grafana username $MY_ADMINUSER
#RUN cd /src && wizzy init && wizzy set grafana password $MY_ADMINPASSWORD

# Reset home dashboard
#############################################
# TODO Old Grafana 4.6.1
#############################################
