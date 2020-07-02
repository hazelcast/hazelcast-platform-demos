# Extra notes on the Management Center Dockerfile

The Dockerfile pre-configures for easy use in Kubernetes.

### Logon / Password

The [Dockerfile](./Dockerfile) pre-configures the logon ("_admin_") and password "_password1_" to simplify use.
These values are set as properties in `pom.xml` for this module.

This is not a suitable method for Production use! Not good for Dev or Test either for the that matter. This is only a demo.
Don't copy.

### License

For Docker and Kubernetes, the license is configured in the Dockerfile using the value in `~/.m2/settings.xml`.

See also the [project top level](../..) for details of how to do this.

### Data

The directory `src/main/resources/data` is copied from this project to become the `data` folder in
the Docker image.

This pre-configures the Management Center with the connections "_site1_" to Kubernetes service
"_site1-service.default.svc.cluster.local_", and "_site2_" on 
"_site2-service.default.svc.cluster.local_".

For Docker, these need slightly amended. Find your machine's network IP address (using something
like "`ifconfig | grep -w inet | grep -v 127.0.0.1`"). Then amend the configuration so that the
attempted connection to "_site1_" is that IP plus port 5701, and for "_site2_" is that IP plus
port 6701. Eg. "_site2_" might be on "_192.168.1.2:6702_".

## Changing

If you need to amend the customisation, start a new Docker container with the standard Management
Center Docker image, add the configuration you require (but don't configure a license), and then
export the "_/data_" folder from the Docker container.
