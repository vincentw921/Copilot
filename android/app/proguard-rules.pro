# R8 rules for release builds.
#
# Firestore documents are read/written through the manual toMap()/fromMap()
# helpers in Item and AircraftProfile (no reflection), and Room generates
# its bindings at compile time, so no model-keep rules are needed here.
# Firebase and AndroidX ship their own consumer rules.

# Keep line numbers so Play Console crash reports are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
