package com.qualcomm.ftcrobotcontroller;

/* Application Contexxt Provider

This wrapper for the android Application class is used to allow a static call
to retrieve the current Application from any point in code.  Useful for allowing
OpModes to easily access the Application context.

See http://www.myandroidsolutions.com/2013/04/27/android-get-application-context/

For this to be used, this class must be specified as the application to use in the
AndroidManifest.xml file.

*/

import android.app.Application;
import android.content.Context;

public class FTCApplicationContextProvider extends Application {

    /**
     * Keeps a reference of the application context
     */
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();

        sContext = getApplicationContext();

    }

    /**
     * Returns the application context
     *
     * @return application context
     */
    public static Context getContext() {
        return sContext;
    }

}