# Shizuku instantiates the UserService by class name, via reflection, in a separate shell process.
-keep class io.github.ptt98hd.totch.shizuku.TouchUserService { public <init>(...); }

# AIDL interfaces crossing the process boundary (descriptors are derived from class names).
-keep class io.github.ptt98hd.totch.shizuku.ITouchService { *; }
-keep class io.github.ptt98hd.totch.shizuku.ITouchService$** { *; }
-keep class io.github.ptt98hd.totch.shizuku.ILineCallback { *; }
-keep class io.github.ptt98hd.totch.shizuku.ILineCallback$** { *; }
