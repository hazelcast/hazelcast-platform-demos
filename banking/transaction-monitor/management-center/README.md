# Extra notes on the Management Center Dockerfile

The Dockerfile pre-configures for easy use in Kubernetes.

### Logon / Password

The [Dockerfile](./Dockerfile) pre-configures the logon ("_admin_") and password "_password1_" to simplify use.
These values are set as properties in `pom.xml` for this module.

This is not a suitable method for Production use! Not good for Dev or Test either for the that matter. This is only a demo.
Don't copy.

### License

For Docker and Kubernetes, the license is configured in the Dockerfile using the value in `~/.m2/settings.xml`.

See also the [project top level](../../..) for details of how to do this.
