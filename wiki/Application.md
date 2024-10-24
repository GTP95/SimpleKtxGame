Setup is done? Initial LibKTX game window is starting up? Great job! :)

Now let's focus on the fun part. First we will look into the [app](https://github.com/libktx/ktx/blob/master/app/README.md) extension from LibKTX.

The starting point of the tutorial will be the code generated by GDX-Liftoff.

In this part we will explain some of the code generated by GDX-Liftoff. In addition, we will improve the code a little bit with some general coding and Kotlin best practices.

***

If you open the `core/src/main/kotlin/com/demoktx/game/DemoGame.kt` file, you will see the following code:
```Kotlin
package com.demoktx.game

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile
import ktx.async.KtxAsync
import ktx.graphics.use

class DemoGame : KtxGame<KtxScreen>() {
    override fun create() {
        KtxAsync.initiate()

        addScreen(FirstScreen())
        setScreen<FirstScreen>()
    }
}

class FirstScreen : KtxScreen {
    private val image = Texture("logo.png".toInternalFile(), true).apply { setFilter(Linear, Linear) }
    private val batch = SpriteBatch()

    override fun render(delta: Float) {
        clearScreen(red = 0.7f, green = 0.7f, blue = 0.7f)
        batch.use {
            it.draw(image, 100f, 160f)
        }
    }

    override fun dispose() {
        image.disposeSafely()
        batch.disposeSafely()
    }
}
```

Note that there are two classes inside this file: DemoGame, which is the entry point of our game, and FirstScreen, that takes care of rendering the LibKTX logo.  
As mentioned in the [LibGDX Extending the Simple Game](https://libgdx.com/wiki/start/simple-game-extended) wiki page it is a good practice to use the **Game** and **Screen** classes.
I think everyone who used these classes before knows that you will need to write your own _ScreenCache_ or something similar to avoid creating new screens all the time and to remember the old status of your screen when switching between them. <br>
The second thing is that you always had to manually take care of **disposing** screens and usually you anyway want to do that when your game's `dispose()` method is called.

What a coincidence: LibKTX will take care of that for us with its [KtxGame](https://github.com/libktx/ktx/blob/master/app/src/main/kotlin/ktx/app/game.kt) implementation! It will have an internal _ScreenCache_ and it will automatically dispose all screens when dispose is called.

Therefore, our main game class extends **KtxGame<KtxScreen>**, which has two optional parameters:

1. **firstScreen**: This will define the initial screen when starting up your game. Since our first screen will require some additional information we cannot pass it immediatly and therefore we don't specify it.<br>
In this case it will use the **emptyScreen()** implementation from LibKTX which is a **KtxScreen** implementation which overrides all **Screen** methods with an empty body.
2. **clearScreen**: default is **true** which means that at the beginning of the **render** method KtxGame will automatically clear the screen before calling the screen's render method. In my opinion this is a desired behavior for almost every game and it is nice that LibKTX takes care of that for us automatically.<br>

***

Besides **KtxGame** there is also **KtxScreen** and our screens (MainMenuScreen and GameScreen) will implement KtxScreen instead of the normal LibGDX Screen interface. <br>
This is also a convenient way to only implement those methods that you need since there is no _ScreenAdapter_ in LibGDX available.

You can add screens to your game via **addScreen** and change to a specific screen via **setScreen**.<br>
Note that you cannot add the same screen multiple times to the game. If you want to do that you first need to call **removeScreen** before adding the screen again.
```Kotlin
override fun create() {
    // ...
    addScreen(MainMenuScreen(this))
    setScreen<MainMenuScreen>()
    super.create()
}
```

***

Let's now modify the autogenerated code to serve our purposes. <br>
First of all, we're adding some resources to the DemoGame class. We will add code to load sprites and a font.




We define `batch` and `font` as **val by lazy** because a batch should not get reassigned and with lazy initialization these resources are really only allocated the first time they are used, which is a nicer solution in my opinion.
We then dispose of those in the `dispose` method. <br>
```Kotlin
class DemoGame : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val font by lazy { BitmapFont() }

    override fun create() {
        KtxAsync.initiate()
        addScreen(FirstScreen())
        setScreen<FirstScreen>()
    }

    override fun dispose() {
        batch.disposeSafely()
        font.disposeSafely()
    }
}
```
You may need to add the following two import statements at the top of the file:
```Kotlin
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
```

Now we write the `MainMenuScreen` class. For now, this will just display some text on the screen.

* For the **MainMenuScreen** class, we don't use an `init` block since the camera can be directly assigned. Using `apply` also allows us to directly call `setToOrtho` within a single line. <br>
Since we are implementing **KtxScreen** we do not need to override `hide`, `show`, `pause`, `resume`, `resize` and `dispose` anymore. <br>
With Kotlin we can also use the **property access** syntax instead of calling the _setter_ methods. E.g. `game.batch.setProjectionMatrix(camera.combined)` can be simplified to `game.batch.projectionMatrix = camera.combined`. <br>
We no longer need to call `Gdx.gl.glClearColor` and `Gdx.gl.glClear` because this is already done within **KtxGame**. <br>
Finally, we are _adding_ and _setting_ our **GameScreen** and we __remove__ and __dispose__ our MainMenuScreen since it will no longer be needed. <br>
We use `val camera` instead of `var camera` since the camera won't be reassigned.

    ```Kotlin
    class MainMenuScreen(val game: DemoGame) : KtxScreen {
        private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 480f) }

        override fun render(delta: Float) {
            camera.update()
            game.batch.projectionMatrix = camera.combined

            game.batch.begin()
            game.font.draw(game.batch, "Welcome to Drop!!! ", 100f, 150f)
            game.font.draw(game.batch, "Tap anywhere to begin!", 100f, 100f)
            game.batch.end()
        }
    }
    ```
    You may need to add the following import statement at the top of the file:
    ```Kotlin
  import com.badlogic.gdx.graphics.OrthographicCamera
    ```
  
Now that we have defined a new screen, let's add it to our game. Going back to the `DemoGame` class, we will add the `MainMenuScreen` to the game and set it as the current screen.

```Kotlin
class DemoGame : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val font by lazy { BitmapFont() }

    override fun create() {
        KtxAsync.initiate()
        addScreen(MainMenuScreen(this))
        setScreen<MainMenuScreen>()
        super.create()
    }

    override fun dispose() {
        batch.disposeSafely()
        font.disposeSafely()
        super.dispose()
    }
}
```
    * We are using **KtxGame** and by adding the MainMenuScreen via `addScreen` and setting it via `setScreen`, our game will start with the MainMenu. <br>
      Also, a call to `super.dispose()` will automatically call the dispose method of all screens added via _addScreen_. <br>

As a sanity check, try to run the game again. You should see the text "Welcome to Drop!!! Tap anywhere to begin!" displayed on the screen.

![MainMenuScreen](images/drops-main-menu.png)

* As a last step we now implement our **GameScreen** class. Similar to the **MainMenuScreen** we can do all the assignments of our variables directly and by having the `spawnRainDrop()` call inside the `show` method we can avoid the `init` block. We again use the **property access** syntax, override only the necessary methods and don't call the _glClear_ methods. <br>
We implement our `spawnRainDrop` method efficiently by using the **Rectangle** constructor with four parameters and directly adding it to our **raindrops** array. <br>
Thanks to Kotlin we can also avoid the string concatenation of **"Drops Collected: " + dropsGathered** and use a string template instead: **"Drops Collected: $dropsGathered"**. <br>
There is also an easier way to keep the bucket's x value within its boundaries. We use the `MathUtils.clamp(bucket.x, 0f, 800f - 64f)` method for it which as a first parameter takes the value to clamp. Second parameter is the minimum value and the third parameter is the maximum value. <br>
We can also change `Gdx.graphics.getDeltaTime()` to `delta` which is passed to the Screen's render method and represents the same value. <br>
Again, we use `val` instead of `var` where possible, and we can also simplify `lastDropTime: Long = 0L` and `dropsGathered: Int = 0` to `lastDropTime = 0L` and `dropsGathered = 0`. 

    ```Kotlin
    class GameScreen(val game: DemoGame) : KtxScreen {
        // load the images for the droplet & bucket, 64x64 pixels each
        private val dropImage = Texture(Gdx.files.internal("images/drop.png"))
        private val bucketImage = Texture(Gdx.files.internal("images/bucket.png"))
        // load the drop sound effect and the rain background music
        private val dropSound = Gdx.audio.newSound(Gdx.files.internal("sounds/drop.wav"))
        private val rainMusic = Gdx.audio.newMusic(Gdx.files.internal("music/rain.mp3")).apply { isLooping = true }
        // The camera ensures we can render using our target resolution of 800x480
        //    pixels no matter what the screen resolution is.
        private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 480f) }
        // create a Rectangle to logically represent the bucket
        // center the bucket horizontally
        // bottom left bucket corner is 20px above
        private val bucket = Rectangle(800f / 2f - 64f / 2f, 20f, 64f, 64f)
        // create the touchPos to store mouse click position
        private val touchPos = Vector3()
        // create the raindrops array and spawn the first raindrop
        private val raindrops = Array<Rectangle>() // gdx, not Kotlin Array
        private var lastDropTime = 0L
        private var dropsGathered = 0

        private fun spawnRaindrop() {
            raindrops.add(Rectangle(MathUtils.random(0f, 800f - 64f), 480f, 64f, 64f))
            lastDropTime = TimeUtils.nanoTime()
        }

        override fun render(delta: Float) {
            // generally good practice to update the camera's matrices once per frame
            camera.update()

            // tell the SpriteBatch to render in the coordinate system specified by the camera.
            game.batch.projectionMatrix = camera.combined

            // begin a new batch and draw the bucket and all drops
            game.batch.begin()
            game.font.draw(game.batch, "Drops Collected: $dropsGathered", 0f, 480f)
            game.batch.draw(bucketImage, bucket.x, bucket.y, bucket.width, bucket.height)
            for (raindrop in raindrops) {
                game.batch.draw(dropImage, raindrop.x, raindrop.y)
            }
            game.batch.end()

            // process user input
            if (Gdx.input.isTouched) {
                touchPos.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
                camera.unproject(touchPos)
                bucket.x = touchPos.x - 64f / 2f
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                bucket.x -= 200 * delta
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                bucket.x += 200 * delta
            }

            // make sure the bucket stays within the screen bounds
            bucket.x = MathUtils.clamp(bucket.x, 0f, 800f - 64f)

            // check if we need to create a new raindrop
            if (TimeUtils.nanoTime() - lastDropTime > 1_000_000_000L) {
                spawnRaindrop()
            }

            // move the raindrops, remove any that are beneath the bottom edge of the
            //    screen or that hit the bucket.  In the latter case, play back a sound
            //    effect also
            val iter = raindrops.iterator()
            while (iter.hasNext()) {
                val raindrop = iter.next()
                raindrop.y -= 200 * delta
                if (raindrop.y + 64 < 0)
                    iter.remove()
    
                if (raindrop.overlaps(bucket)) {
                    dropsGathered++
                    dropSound.play()
                    iter.remove()
                }
            }
        }

        override fun show() {
            // start the playback of the background music when the screen is shown
            rainMusic.play()
            spawnRaindrop()
        }

        override fun dispose() {
            dropImage.dispose()
            bucketImage.dispose()
            dropSound.dispose()
            rainMusic.dispose()
        }
    }
    ```
  
You may need to add the following import statements at the top of the file:
```Kotlin
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.Array
```

***

As before, we need to add the `GameScreen` to our game. But this time, we will add it inside
the `MainMenuScreen` and set it as the current screen. This is because we want to transition form the `MainMenuScreen` to the `GameScreen`
So, we will add the following code inside the `render` method of the `MainMenuScreen` class:

```Kotlin
if (Gdx.input.isTouched) {
            game.addScreen(GameScreen(game))
            game.setScreen<GameScreen>()
            game.removeScreen<MainMenuScreen>()
            dispose()
        }
```
When we click somewhere on the `MainMenu` screen, this code will add the `GameScreen` to the game, set it as the current screen, remove the `MainMenuScreen` and dispose of it. <br>

The `MainMenuScreen` class should now look like this:

```Kotlin
class MainMenuScreen(val game: DemoGame) : KtxScreen {
    private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 480f) }

    override fun render(delta: Float) {
        camera.update()
        game.batch.projectionMatrix = camera.combined

        game.batch.begin()
        game.font.draw(game.batch, "Welcome to Drop!!! ", 100f, 150f)
        game.font.draw(game.batch, "Tap anywhere to begin!", 100f, 100f)
        game.batch.end()

        if (Gdx.input.isTouched) {
            game.addScreen(GameScreen(game))
            game.setScreen<GameScreen>()
            game.removeScreen<MainMenuScreen>()
            dispose()
        }
    }
}
```

One last thing: we need to add to the project the assets we're using in our code.
We already have an `assets [main]` directory. Create the following subdirectories:
- `images` (for the `backround.png`, `bucket.png` and `drop.png` images)
- `sounds` (for the `drop.mp3` sound effect)
- `music` (for the `music.mp3` background music)

You can download these resources from the [official LibGDX tutorial](https://libgdx.com/wiki/start/a-simple-game#loading-assets) or use your own. <br>
Now, run the application again and play with your newly created game!

The game works, but we can make it prettier by having a background image instead of a black screen. As an assignment,
try to use what you've learned so far to add a background image to the game. If you downloaded the assets from the LibGDX wiki, 
you can use the `background.png` image provided there. Otherwise, use an image you like. <br>

This ends the basic LibKTX application section. The final code can be checked out via the [01-app branch](https://github.com/Quillraven/SimpleKtxGame/tree/01-app). <br>
In the [next section](https://github.com/Quillraven/SimpleKtxGame/wiki/Graphics-and-Collections) we will look into the **graphics** and **collections** extensions provided by **LibKTX** to also make our life easier when working with **SpriteBatch** and **Collections**.
