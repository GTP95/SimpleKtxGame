Seems like you are interested to learn something new today! Good to hear ;)

Like with any other game we need to start with setting up our development environment.

I will use [IntelliJ](https://www.jetbrains.com/idea/) as IDE which should be similar to [Android Studio](https://developer.android.com/studio). 
I won't explain how it works in Eclipse since I would not recommend this IDE unless you are working on a Rich Client Platform project which we don't.
The version that I use while writing this page is 2019.2 with LibGDX 1.9.10 and LibKTX 1.9.10-b1 

**Update as of 2020-12-02:** updated codebase to LibGDX 1.9.12 and LibKTX 1.9.12-b1.

To start it off download the latest version of [LibGDX Liftoff Jar](https://github.com/libgdx/gdx-liftoff/releases) and run it.
Fill in the project name and optionally change the package and main class name. Then click on "PROJECT OPTIONS" 

![Home](./images/gdx-liftoff-home.png)

Make sure to have "Kotlin" listed in the "LANGUAGES" column. If not, add it using the plus simbol in the bottom-right of that column. To make things easier, click on the "PLATFORMS" column and remove Android. Otherwise, you will need to provie a path to the Android SDK later on.

![Addons](./images/gdx-liftoff-addons.png)

Then click on "Choose" to select a template. Pick the "Kotlin + KTX" template and click on the "OK" button.

![Template](./images/gdx-liftoff-templates.png)

This should be the end result for the "ADD-ONS" window, it doesn't metter if you selected some extensions or not:

![Addons final](./images/gdx-liftoff-addons-final.png)

Click on next. You will get a list of third-party extensions that we can ignore for now. Click on next again. 

In the "SETTINGS" page, make sure to fill in the Java version installed on your computer and the latest version of LibGDX (or accept the defaults if you're unsure).

![Settings](./images/gdx-liftoff-settings.png)

Click on "GENERATE". If everything went according to plan, you should see a window like this:

![Setup complete](./images/gdx-liftoff-setup-complete.png)

You can now close gdx-liftoff and open your project inside IntelliJ.

Inside IntelliJ, open the "Gradle" tab on the left, go to Lwjgl3 > tasks > application and double click on "run".

![Run](./images/gradle-run.png)

If everything went fine, you'll see the LibKTX logo:

![Logo](./images/libktx-logo.png)
# Known issues
This section may no longer be applicable, as this wiki transitioned to the new `gdex-liftoff` tool. It is still here in case it might help someone.

In case you receive a "Could not load main class" error then don't be frustrated. This is a very common problem that a lot of people face out there. To save you the time of googling hours for a solution, here it is:
- Go to the build.gradle file of your desktop project
- Add following lines
    ```diff
    project.ext.mainClassName = "com.libktx.game.desktop.DesktopLauncher"
    project.ext.assetsDir = new File("../android/assets")

    + dependencies {
    +   runtimeClasspath files("../core/build/classes/kotlin/main")
    +   runtimeClasspath files("build/classes/kotlin/main")
    + }
    ```
- Refresh your gradle project
- Try it again. It should work now
