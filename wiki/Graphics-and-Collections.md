This section will introduce the LibKTX [graphics](https://github.com/libktx/ktx/blob/master/graphics/README.md) and [collections](https://github.com/libktx/ktx/blob/master/collections/README.md) extensions to see how they can further improve our life as a game developer. It is also the _last mandatory_ section for the game. All following sections are something in addition to further improve your Kotlin game development skills. <br>
As a starting point we use the code from the [previous section](https://github.com/Quillraven/SimpleKtxGame/wiki/Application) which you can find in the repository under the [01-app branch](https://github.com/Quillraven/SimpleKtxGame/tree/01-app).

First thing we need to do again is to update our project's **build.gradle** file to add the additional LibKTX extensions. Re-sync your project once you have added the two additional lines.
```Diff
        //...
        api "com.badlogicgames.gdx:gdx:$gdxVersion"
        api "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
        api "io.github.libktx:ktx-app:$ktxVersion"
+       api "io.github.libktx:ktx-collections:$ktxVersion"
+       api "io.github.libktx:ktx-graphics:$ktxVersion"
    }
}
```

With the new **graphics** extensions we can now replace `batch.begin()` and `batch.end()` with `batch.use { ... }`. While this is not a huge change it is still a nicer way to read the code in my opinion. <br>
In addition there are other graphics extensions which are not used in this simple game but which are very handy. Please check them out in more detail at the LibKTX [graphics](https://github.com/libktx/ktx/blob/master/graphics/README.md) repository.

The **collections** extensions add a lot of utility to LibGDX's own collections like Array or ObjectMap. Unfortunately, those collections do not implement any of the **java.util** interfaces or extend any of these abstract classes but they are greatly optimized for garbage collection. <br>
LibKTX adds a lot of the missing utility and also enhances it even further with Kotlin's language possibilities. <br>
For our game we will use the `Array.iterate { ... }` extension to iterate over our raindrop array in a nicer way to directly access each element and the iterator itself. Also, we will use the `Array.forEach {... }` loop instead of the `for(raindrop in raindrops) { ... }` syntax since it is faster to write and has less characters. Believe it or not in your entire life those seconds will add up ;)

***

* Our **MainMenuScreen** is almost unchanged. It only uses the new `batch.use` extension. Since we do not rename the lambda element we can access the batch within `use` via the default **it**.
    ```Kotlin
    class MainMenuScreen(val game: Game) : KtxScreen {
        private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 480f) }

        override fun render(delta: Float) {
            camera.update()
            game.batch.projectionMatrix = camera.combined

            game.batch.use {
                game.font.draw(it, "Welcome to Drop!!! ", 100f, 150f)
                game.font.draw(it, "Tap anywhere to begin!", 100f, 100f)
            }

            if (Gdx.input.isTouched) {
                game.addScreen(GameScreen(game))
                game.setScreen<GameScreen>()
                game.removeScreen<MainMenuScreen>()
                dispose()
            }
        }
    }
    ```

* In our **GameScreen** class we can update the `render` method with the new extensions. Similar to our **MainMenuScreen** we use the `batch.use` extension but this time we rename the lambda element to batch to demonstrate this possibility and to make it clearer since we also have **it** in our **forEach** loop. We use Kotlin's `forEach` instead of the normal `for` loop and rename the lambda element to raindrop instead of **it**. <br>
As a last step we use `iterate { ... }` to iterate over our raindrops array and to have the possibility to access the element itself and also the iterator. This way we combine following three lines

    ```Kotlin
    val iter = raindrops.iterator()
    while (iter.hasNext()) {
        val raindrop = iter.next()
    ```

    into one

    ```Kotlin
    raindrops.iterate { raindrop, iterator ->
    ```

    Here is the final code for our **GameScreen**:


    ```Kotlin
    override fun render(delta: Float) {
            // generally good practice to update the camera's matrices once per frame
            camera.update()

            // tell the SpriteBatch to render in the coordinate system specified by the camera.
            game.batch.projectionMatrix = camera.combined

            // begin a new batch and draw the bucket and all drops
            game.batch.use { batch ->
                game.font.draw(batch, "Drops Collected: $dropsGathered", 0f, 480f)
                batch.draw(bucketImage, bucket.x, bucket.y, bucket.width, bucket.height)
                raindrops.forEach { raindrop -> batch.draw(dropImage, raindrop.x, raindrop.y) }
            }

            // process user input
            // ...

            // move the raindrops, remove any that are beneath the bottom edge of the
            //    screen or that hit the bucket.  In the latter case, play back a sound
            //    effect also
            raindrops.iterate { raindrop, iterator ->
                raindrop.y -= 200 * delta
                if (raindrop.y + 64 < 0) {
                    iterator.remove()
                }
                if (raindrop.overlaps(bucket)) {
                    dropsGathered++
                    dropSound.play()
                    iterator.remove()
                }
            }
        }
    ```

***

This ends the section for **graphics** and **collections** extensions. Those are minor things but they add up during your project and save a lot of lines and also increase the readability of your code once you are getting used to **lambdas**. The final code can be checked out using the [02-graphics-collections branch](https://github.com/Quillraven/SimpleKtxGame/tree/02-graphics-collections). <br>
The [next section](https://github.com/Quillraven/SimpleKtxGame/wiki/Log) will be a very short one which introduces the **logging** extensions of LibKTX. Since every game needs to log in some way it is an essential part and should not be skipped.