# Project specific R8 rules.
# For more details, see
#   https://developer.android.com/build/shrink-code

# Keep the line numbers so that stack traces stay readable.
# The mapping file (uploaded to Crashlytics and Play Console) restores
# the original class and method names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
