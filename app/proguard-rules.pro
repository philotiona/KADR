# KADR R8 rules (Lab 8, task 4)
# Keep file names and line numbers so crash reports can be decoded with mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Debug logs are removed from release code entirely (checklist item "no debug logs").
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
