# AppStore Caching Service

1. Start application
```
1. Run `mvn clean install` to build your application
2. Start application with `docker run --rm -d -p 8080:8080 -name appstore-caching-service daccloud/appstore-caching-service:latest`
3. To check that your application is running enter url `http://localhost:8080`
```

Integration tests
---
**How to run integration tests?**

Navigate to the parent maven module `appstore-caching-service` and run `mvn clean verify`


Add license to headers:
```
mvn com.mycila:license-maven-plugin:format -Dcurrent.year=<current_year>
```


Install on kubernetes:

1. Create a file `appstore-caching-service.yaml` and set properties

```yaml 
apiVersion: helm.fluxcd.io/v1
kind: HelmRelease
metadata:
  name: appstore-caching-service
  namespace: <namespace>
spec:
  chart:
    repository: http://libertyglobal.github.io/appstore-caching-service/charts/
    name: appstore-caching-service
    version: <version>
  values:
    ingress:
      domainName: <cluster_dns_name>
pvc:
  bundleGenerator:
    volumeName: <pvc_volume_name>
```

2. Run `kubectl apply -f appstore-caching-service.yaml`