AndroidPatternLock
==================

An android library with the implementation of the pattern lock screen


The data is read from a db which is pre-created. Prior to Android 2.3, any compressed asset file with an uncompressed size of over 1 MB cannot be read from the APK. With this limitation, the db file is cut into several pieces which are combined back together to the one database file on the first run of the app. An index is also created to speed up the query of the db.
