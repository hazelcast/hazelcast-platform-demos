FROM --platform=linux/amd64 prom/prometheus:v2.41.0

# Configure to scrape from Management Center
COPY target/classes/prometheus.yml      /etc/prometheus
