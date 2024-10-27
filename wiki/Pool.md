Did you take your small break? For me it was an energy drink as I don't drink coffee (_very_ interesting). But let's get back to business: **pooling**! <br>
As mentioned in the [last](https://github.com/Quillraven/SimpleKtxGame/wiki/Assets-and-TextureAtlas) part we are currently creating endless **Rectangle** objects whenever we spawn a raindrop. Of course this is not ideal and there is a solution for it called [Object pooling](https://libgdx.com/wiki/articles/memory-management). Basically, whenever you don't need an object anymore (e.g. our raindrop leaves the screen) you will put it back to a pool of that object. Instead of creating a new object all the time you can then reuse existing objects in the pool first. **LibKTX** offers some [pool extensions](https://github.com/libktx/ktx/blob/master/assets/README.md) as well to simplify `pool.obtain()` to `pool()` and `pool.free(instance)` to `pool(instance)`.

***

First things first - **build.gradle** upd... WRONG! Got you there ;) **Pooling** is part of the [LibKTX assets](https://github.com/libktx/ktx/blob/master/assets/README.md) extensions which we already added in the previous part.

The only thing left is then to update our **GameScreen**. First we create a new rectangle pool via `pool { Rectangle() } `. We also rename our **raindrops** array to **activeRaindrops** since this array will now only contain the active drops on the screen. _Dead_ raindrops will be part of our pool and can be reused (can raindrops really die?). 

```Diff
-  private val raindrops = Array<Rectangle>() // gdx, not Kotlin Array
+  private val raindropsPool = pool { Rectangle() } // pool to reuse raindrop rectangles
+  private val activeRaindrops = Array<Rectangle>() // gdx, not Kotlin Array
```

In our `spawnRaindrop` method we will now use our pool to obtain either an existing rectangle or a new one. A call to `raindropsPool()` will do that for us.

```Diff
private fun spawnRaindrop() {
-   raindrops.add(Rectangle(MathUtils.random(0f, 800f - 64f), 480f, 64f, 64f))
+   activeRaindrops.add(raindropsPool().set(MathUtils.random(0f, 800f - 64f), 480f, 64f, 64f))
    lastDropTime = TimeUtils.nanoTime()
}
```

Also, when our raindrops fall into the bucket or whenever they leave the screen we want to put them back into our pool. To visualize that our pool is working we will also add some debug [log](https://github.com/Quillraven/SimpleKtxGame/wiki/Log) messages. With `raindropsPool(raindrop)` we put the rectangle back to the pool.

```Diff
activeRaindrops.iterate { raindrop, iterator ->
    raindrop.y -= 200 * delta
    if (raindrop.y + 64 < 0) {
        iterator.remove()
-       log.debug { "Missed a raindrop!" }
+       raindropsPool(raindrop)
+       log.debug { "Missed a raindrop! Pool free objects: ${raindropsPool.free}" }
    }
    if (raindrop.overlaps(bucket)) {
        dropsGathered++
        dropSound.play()
        iterator.remove()
+       raindropsPool(raindrop)
+       log.debug { "Pool free objects: ${raindropsPool.free}" }
    }
}
```

***

And with that we are done optimizing our game! It is now close to perfect. Although there are still things that you can do to improve your coding skills that we will cover in the next parts, there is nothing left to improve the game itself. We added pooling and a TextureAtlas and we used Kotlin best practices and LibKTX extensions to make our code as clean as possible. <br>
The code until now can be checked out with the [05-pool branch](https://github.com/Quillraven/SimpleKtxGame/tree/05-pool). The [next](https://github.com/Quillraven/SimpleKtxGame/wiki/Inject) section will cover the **context** concept that was mentioned at the end of the [previous](https://github.com/Quillraven/SimpleKtxGame/wiki/Assets-and-TextureAtlas) part which will further simplify our **Game** class.
