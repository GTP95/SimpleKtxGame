As mentioned in the [previous section](https://github.com/Quillraven/SimpleKtxGame/wiki/Graphics-and-Collections) all following sections (including this one) are optional and were not covered in the original LibGDX tutorial. However, I recommend to read them as well because they are basic things that you will need in every game. <br>
Without further ado let's jump into **logging**. Please refer to the [official LibGDX documentation](https://github.com/libgdx/libgdx/wiki/Logging) to learn more about logging. I will only focus on the LibKTX extensions.

***

So, what is the first thing we need to do? Correct! Update our **project's build.gradle** file and re-sync the project :)

```Diff
        // ...
        api "io.github.libktx:ktx-app:$ktxVersion"
        api "io.github.libktx:ktx-collections:$ktxVersion"
        api "io.github.libktx:ktx-graphics:$ktxVersion"
+       api "io.github.libktx:ktx-log:$ktxVersion"
    }
}
```

After that we can already use the [LibKTX log](https://github.com/libktx/ktx/blob/master/log/README.md) extensions. Please refer to the **Why?** section of this documentation as it already explains the benefits of using LibKTX for logging. In short, it addresses the unnecesarry string building, creation and vararg calls by introducing **inlined lambdas** which means that instead of Java's

```Java
if (Gdx.app.logLevel >= Application.LOG_DEBUG) Gdx.app.debug("someTag", "My message: " + someObject);
```

we get

```Kotlin
debug("someTag"){ "My message: $someObject }
```

***

To demonstrate it in a real example we will update our **GameScreen** and log a debug message whenever we miss a raindrop and when the `dispose` method is called. <br>
First we create a **private toplevel** log which is the fastest and best way to create a static log instance. The alternative would be a **companion object** which is not as performant as the private toplevel (refer to _Christophe Beyls_ [Kotlin's hidden costs](https://www.youtube.com/watch?v=xaVLJmTf1kA) talk). <br>

```Diff
+import ktx.log.logger

+private val log = logger<GameScreen>()

class GameScreen(val game: Game) : KtxScreen {
// ...
```

After that we can log anything related to our GameScreen file. We will add following two debug lines.

```Diff
// ...
raindrops.iterate { raindrop, iterator ->
    raindrop.y -= 200 * delta
    if (raindrop.y + 64 < 0) {
        iterator.remove()
+       log.debug { "Missed a raindrop!" }
    }
    if (raindrop.overlaps(bucket)) {
        dropsGathered++
        dropSound.play()
        iterator.remove()
    }
}
// ...
override fun dispose() {
+   log.debug { "Disposing ${this.javaClass.simpleName}" }
    dropImage.dispose()
    bucketImage.dispose()
    dropSound.dispose()
    rainMusic.dispose()
}
// ...
```

To see our new debug lines we also need to adjust our **DesktopLauncher** to set the correct loglevel. This can be done anywhere but we will do it in the DesktopLauncher for now. The **default** loglevel is **Info**. We will set it to **Debug**.

```Kotlin
object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration().apply {
            title = "Drop"
            width = 800
            height = 480
        }
        LwjglApplication(Game(), config).logLevel = Application.LOG_DEBUG
    }
}
```

And with that we are done with logging basics! I told you it will be short ;) The final code can be checked out with the [03-log branch](https://github.com/Quillraven/SimpleKtxGame/tree/03-log).<br>
[Next](https://github.com/Quillraven/SimpleKtxGame/wiki/Assets-and-TextureAtlas) we will introduce a better way of handling our **assets** like textures, music and sounds. Also, we will improve **render performance** by using a **TextureAtlas** to avoid texture swapping.
