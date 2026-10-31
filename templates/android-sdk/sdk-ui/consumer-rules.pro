# Keep public UI components for SDK consumers
-keep public class ${configs.packageName}.sdk.ui.** {
    public protected *;
}
