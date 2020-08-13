# Extra notes on the Grafana Dockerfile

The Dockerfile pre-configures for easy use in Kubernetes.

### Logon / Password

The [Dockerfile](./Dockerfile) pre-configures the logon ("_admin_") and password "_password1_" to simplify use.
These values are set as properties in `pom.xml` for this module.

This is not a suitable method for Production use! Not good for Dev or Test either for the that matter. This is only a demo.
Don't copy.
