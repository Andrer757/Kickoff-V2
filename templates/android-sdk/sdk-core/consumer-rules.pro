# Keep public API classes and methods for SDK consumers
-keep public class ${configs.packageName}.sdk.core.** {
    public protected *;
}
