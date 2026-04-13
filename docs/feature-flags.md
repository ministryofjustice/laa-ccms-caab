## Feature flags

Feature flags can be used to enabled or disable access to certain features of the application.

### Available feature flags

| Feature Flag | Description                                                                    | Default |
|--------------|--------------------------------------------------------------------------------|---------|
| amendments   | Provides access to all functionality related to amending a case / application. | false   |

### Enabling a feature flag

To enable a feature, or list of features, set the following application property:

```yaml
laa:
  ccms:
    features:
      - feature: amendments
        enabled: true
```

### Creating a new feature flag

To create a feature flag, simply add the name of your feature to the `uk.gov.laa.ccms.caab.feature.Feature` enum.


### Using feature flags

#### Controller Methods - @RequiresFeature

To put a controller method under a feature flag, use the `@RequiresFeature` annotation as follows.

If `FEATURE_1` is enabled, the controller method will execute as usual.
Otherwise the user will be redirected to a 'feature unsupported' screen.

```java
  @GetMapping("/resource")
  @RequiresFeature(Feature.FEATURE_1)
  public String allResources(Model model) {

    List<Resource> resources = service.getResources();

    model.addAttribute("resources", resources);

    return "resource_view";
  }
```

#### Anywhere - `FeatureService.isEnabled(Feature feature)`

If further granularity is required, the FeatureService can be injected directly. A `isEnabled(Feature feature)`
method is available to check whether a feature is enabled. This can be used to carry out any conditional business logic.

```java
@Controller
@RequiredArgsConstructor
public class ResourceController {

  private final FeatureService featureService;

  @GetMapping("/resource")
  public String allResources(Model model) {

    List<Resource> resources = service.getResources();

    if (featureService.isEnabled(Feature.FEATURE_1)) {
      // filter resources related to FEATURE_1
    }

    model.addAttribute("resources", resources);

    return "resource_view";
  }
}
```