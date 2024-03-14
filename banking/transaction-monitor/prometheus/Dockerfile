FROM --platform=linux/amd64 prom/prometheus:v2.50.1

# Configure to scrape from Management Center
COPY target/classes/prometheus.yml      /etc/prometheus
