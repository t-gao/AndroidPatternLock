AndroidPatternLock
==================

An Android library with the implementation of the pattern lock screen.

Android 手势密码view.

Some encryption methods are provided: MD5/SHA-1/SHA-256.

* How to use:

    1. Add this project to be your own project's library project.

    2. Use the LockView in you layout xml file, as below:
    
    ```xml
            <com.tg.androidpatternlock.LockView
            android:id="@+id/lockview"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            />
    ```
        
    3. In your activity:
    
        <p> Make these calls:
        
        ```java
            mLockView = (LockView) findViewById(R.id.lockview);
            mLockView.setPatternListener();
            mLockView.setPatternPasswordStorageFetcher();
            mLockView.setWorkMode();
        ```
        </p>
        
        <p> There are two work modes, creating and input, which respectively should be used when user is creating or inputing pattern password.</p>
              
        <p> Below calls are optional:
        
        ```java
	    mLockView.setSkipPolicy(LockView.SkipPolicy_AutoConnect);
            mLockView.setPathColorCorrect(getResources().getColor(R.color.some_color));
            mLockView.setCircleColorNormal(getResources().getColor(R.color.some_color));
            mLockView.setCircleColorCorrect(getResources().getColor(R.color.some_color));
            mLockView.setUiStyle(LockView.UiStyle_Circle);
        ```
        </p>

* Screen shots:

	Creating mode                                                   |  Inputing mode
	:--------------------------------------------------------------:|:-------------------------------------------------:
	![Alt text](/sample/screen-shot2.png?raw=true "Creating mode")  |  ![Alt text](/sample/screen-shot.png?raw=true "Inputing mode")

