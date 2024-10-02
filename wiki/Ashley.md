You made it to the last part of this tutorial - congratulations! In this part we will focus on introducing an **entity component system** into our game. **LibGDX** uses [ashley](https://github.com/libgdx/ashley/wiki) as its entity component system. I will not explain in detail what an entity component system is as this is covered already in the link above and in the [About Entity Systems article](https://www.gamedev.net/articles/programming/general-and-gameplay-programming/understanding-component-entity-systems-r3013). The code for our game can be checked out with the [07-ashley branch](https://github.com/Quillraven/SimpleKtxGame/tree/07-ashley). It uses the [LibKTX ashley extensions](https://github.com/libktx/ktx/tree/master/ashley). <br>
Let's go!

***

Who would have thought but we need to update our **project's build.gradle** file. This time we need to add the **ktx** part and also the **badlogicgames** part to be able to use ashley.

```Diff
dependencies {
+   api "com.badlogicgames.ashley:ashley:$ashleyVersion"
    api "com.badlogicgames.gdx:gdx:$gdxVersion"
    api "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    api "io.github.libktx:ktx-app:$ktxVersion"
+   api "io.github.libktx:ktx-ashley:$ktxVersion"
    api "io.github.libktx:ktx-assets:$ktxVersion"
    api "io.github.libktx:ktx-collections:$ktxVersion"
    // ...
```

Next we are going to create a `PooledEngine` for our game to get the advantages of [pooling](https://github.com/Quillraven/SimpleKtxGame/wiki/Pool). The **engine** is responsible to create our **entities**, **components** and to update them via **systems**. We will also add a debug [log](https://github.com/Quillraven/SimpleKtxGame/wiki/Log) message in our **Game's** `dispose` method to visualize how many entities were added to our engine. We also no longer access the assets in our **GameScreen's** constructor and therefore we can already create the screen in our `create` method. Here are the changes to our **Game** class:

```Diff
+   private val log = logger<Game>()

    class Game : KtxGame<KtxScreen>() {
        private val context = Context()
        override fun create() {
            context.register {
                bindSingleton<Batch>(SpriteBatch())
                bindSingleton(BitmapFont())
                bindSingleton(AssetManager())
                // The camera ensures we can render using our target resolution of 800x480
                //    pixels no matter what the screen resolution is.
                bindSingleton(OrthographicCamera().apply { setToOrtho(false, 800f, 480f) })
+               bindSingleton(PooledEngine())

                addScreen(LoadingScreen(this@Game, inject(), inject(), inject(), inject()))
+               addScreen(GameScreen(inject(), inject(), inject(), inject(), inject()))
            }
            setScreen<LoadingScreen>()
            super.create()
        }

        override fun dispose() {
+           log.debug { "Entities in engine: ${context.inject<PooledEngine>().entities.size()}" }
            context.dispose()
            super.dispose()
        }
    }
```

With that we are able to use now our engine and adjust the existing code according to an **entity component system** approach. I will only summarize the idea behind it as explaining everything here in detail would be too much. I will mainly focus on the **Kotlin** parts and **LibKTX** extensions that are used.

***

**Components** <br>
We have following components for our entities:
* **BucketComponent**: this component is added to our bucket to keep track of how many raindrops were already gathered
* **CollisionComponent**: this component is added to our raindrops in order for them to collide with our bucket
* **MoveComponent**: this component is used for both bucket and raindrop in order for them to move. Raindrops move downwards and the bucket can move left and right when pressing the arrow keys
* **RenderComponent**: this component is also used for both bucket and raindrop. It contains the sprite and **z-index** for rendering. The z-index is used to render our entities in a specific order. The bucket is drawn in the background while raindrops are drawn in the foreground.
* **TransformComponent**: this component is also used for bucket and raindrops and contains the boundaries of those entities (=position and size).

To have a high performance when accessing components of an entity we use [ComponentMappers](https://github.com/libgdx/ashley/wiki/How-to-use-Ashley). **LibKTX's** [ashley](https://github.com/libktx/ktx/tree/master/ashley) extensions recommend the usage of **companion objects** for that. Let's look at our **TransformComponent** as an example:

```Kotlin
class TransformComponent : Component {
    companion object {
        val mapper = mapperFor<TransformComponent>()
    }

    val bounds = Rectangle()
} 
```

The `mapperFor` extension is an easy way to create a ComponentMapper for a specific type. We can then simply retrieve the transform component of an entity via `entity[TransformComponent.mapper]`.

***

**Systems** <br>
We will have following systems for our engine:
* **CollisionSystem**: detects whenever a raindrop is leaving the screen or collides with the bucket
* **MoveSystem**: moves our entities according to their **MoveComponent** and **TransformComponent**
* **RenderSystem**: responsible for rendering our bucket, raindrops and gather raindrops information
* **SpawnSystem**: responsible to spawn our raindrops

Let's go through system by system to cover the important parts.

In our **CollisionSystem** we need to periodically check if a raindrop collides with our bucket. Instead of retrieving the boundaries of the bucket each frame we are going to store it when the system is created. I used the `!!` operator when accessing the components of the bucket because the game should crash if these components are missing for our bucket. It means that we screwed up something and did not correctly initialize our bucket entity. While in general you should try to avoid the `!!` operator as it looks ugly (for a reason!) and most likely there is a better solution, it makes sense in this case and is used on purpose. <br>
The next thing is that every **IteratingSystem** requires a **family** to call `processEntity` each frame for all entities which are part of the family. **LibKTX** simplifies the creation of families with its `allOf`, `oneOf` and `exclude` extensions. <br>
Unfortunately, there is one _ugly_ part when working with ashley in my opinion. Our CollisionSystem only calls `processEntity` for entities which have a **TransformComponent** and a **CollisionComponent** (=our raindrops). When accessing these components via `entity[TransformComponent.mapper]` the compiler does not know that this can **never** be null and therefore it will complain when we write something like `entity[TransformComponent.mapper].bounds.y`. The nicest way to deal with this issue is in my opinion **Kotlin's let and ?** functionality which makes the code still readable and short. Example:

```Kotlin
override fun processEntity(entity: Entity, deltaTime: Float) {
    entity[TransformComponent.mapper]?.let { transform ->
        if (transform.bounds.y < 0) {
            engine.removeEntity(entity)
        } else if (transform.bounds.overlaps(bucketBounds)) {
            bucketCmp.dropsGathered++
            dropSound.play()
            engine.removeEntity(entity)
        }
    }
}
```

The last thing I want to mention for the CollisionSystem is that it should be added as **last** system to our engine because it is removing entities. As a best practice always remove entities as a last step within your engine's `update` method or do it outside your `update` call. Otherwise you will get very strange bugs which are hard to find!

The **MoveSystem** has nothing special compared to our **CollisionSystem**. It also uses **Kotlin's ?.let** to improve the readability of our component access logic.

**RenderSystem** is a **SortedIteratingSystem** which automatically sorts our entities by their **RenderComponent's z** value. We use **Kotlin's compareBy** functionality to create the comparator:

```Kotlin
SortedIteratingSystem(
        allOf(TransformComponent::class, RenderComponent::class).get(),
        // compareBy is used to render entities by their z-index (=bucket is drawn in the background; raindrops are drawn in the foreground)
        compareBy { entity: Entity -> entity[RenderComponent.mapper]?.z })
```

**SortedIteratingSystem** does not sort entities every frame. It is up to the user when he wants to sort. In our case we want to do it every frame. That's why we call `forceSort()` within its `update` method. Here is the entire code as a good example for a system:

```Kotlin
class RenderSystem(bucket: Entity,
                   private val batch: Batch,
                   private val font: BitmapFont,
                   private val camera: OrthographicCamera) : SortedIteratingSystem(
        allOf(TransformComponent::class, RenderComponent::class).get(),
        // compareBy is used to render entities by their z-index (=bucket is drawn in the background; raindrops are drawn in the foreground)
        compareBy { entity: Entity -> entity[RenderComponent.mapper]?.z }) {
    private val bucketCmp = bucket[BucketComponent.mapper]!!

    override fun update(deltaTime: Float) {
        forceSort()
        // generally good practice to update the camera's matrices once per frame
        camera.update()
        // tell the SpriteBatch to render in the coordinate system specified by the camera.
        batch.projectionMatrix = camera.combined
        // draw all entities in one batch
        batch.use {
            super.update(deltaTime)
            font.draw(batch, "Drops Collected: ${bucketCmp.dropsGathered}", 0f, 480f)
        }
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        entity[TransformComponent.mapper]?.let { transform ->
            entity[RenderComponent.mapper]?.let { render ->
                batch.draw(render.sprite, transform.bounds.x, transform.bounds.y)
            }
        }
    }
} 
```

In our **SpawnSystem** we use several **LibKTX extensions**  to ease the process of creating new raindrop entities. `engine.entity { ... }` creates a new entity and adds it to the engine. Within the `{ ... }` block you can specify the configuration of the entity. We need all components for our raindrop except the **BucketComponent**. We use the `with<ComponentType> { ... }` extension to add a component of a specific type to our entity. Again, within the `{ ... }` block you can configure the created component. E.g. we set the correct **region** for our **sprite** in the **RenderComponent** and also set the **z** value to 1 to render the raindrops in the foreground.

```Kotlin
engine.entity {
    with<RenderComponent> {
        sprite.setRegion(dropRegion)
        z = 1
    }
    with<TransformComponent> { bounds.set(MathUtils.random(0f, 800 - 64f), 480f, 64f, 64f) }
    with<MoveComponent> { speed.set(0f, -200f) }
    with<CollisionComponent>()
}
```

***

Check out the [07-ashley branch](https://github.com/Quillraven/SimpleKtxGame/tree/07-ashley) for the final code. <br>

I hope you enjoyed this _small_ tutorial series and learned a lot about how to use **Kotlin** and **LibKTX** when developing games with **LibGDX**. 

Thank you for reading through all sections and good luck with your future projects!