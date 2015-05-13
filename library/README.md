AndroidPatternLock
==================

An Android library with the implementation of the pattern lock screen.

Android 手势密码view.

Some encryption methods are provided: MD5/SHA-1/SHA-256.

How to use:

1. Add this project to be your own project's library project.

2. Use the LockView in you layout xml file, as below:

      <com.tg.androidpatternlock.LockView
        android:id="@+id/lockview"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        />

3. In your activity:
      mLockView = (LockView) findViewById(R.id.lockview);

      You should also call below methods:
      
          mLockView.setPatternListener();
          
          mLockView.setPatternPasswordStorageFetcher();
          
          mLockView.setWorkMode();
  
      There are two work modes, creating and input, which respectively should be used when user is creating or inputing pattern password.
