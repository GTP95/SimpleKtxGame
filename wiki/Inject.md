You are getting almost to the end of things that I can teach you with this tutorial. I hope you had fun reading until now and that you learned a few things from me! One of the last things I want to teach you is the [LibKTX inject](https://github.com/libktx/ktx/blob/master/inject/README.md) concept. <br>
In any game there are components that need to work together somehow like assets, physics, input listeners, etc.. One way is to create a lot of **manager** classes and pass them to different other classes which results in big constructors. I think we can agree that it is not a comfortable and modular solution and thinking a step further it gets trickier in case you need device specific code segments or instances of your classes. <br>
But do not worry! **LibKTX inject** comes to the rescue! It is a very **lightweight** and **simple** solution with little to no garbage at runtime. While our game is very simple and we would not need that at all I still want to showcase it so that you can use it in bigger projects and grasp the concept.

***

This time we are back to our **project's build.gradle** file. Didn't you miss it? Let's add the **inject** extensions to our project:

```Diff
    // ...
    api "io.github.libktx:ktx-assets:$ktxVersion"
    api "io.github.libktx:ktx-collections:$ktxVersion"
    api "io.github.libktx:ktx-graphics:$ktxVersion"
+   api "io.github.libktx:ktx-inject:$ktxVersion"
    api "io.github.libktx:ktx-log:$ktxVersion"
    // ...
```

Next we create a so called **context** which will contain all the different _providers_ that we need for our different components which are relevant for the entire game. Thanks to Kotlin's **inline** functionality we omit reflection and annotation usage and can access our component instances in a very convenient and readable way which gets already resolved at **compile-time**. Additionally, `context.dispose()` will call the dispose method of any `Disposable` object in the context which reduces the amount of dispose calls and also adds some safety in case you even forget to call dispose (_since nobody is perfect_). So, let's add a context to our game and dispose it:

```Diff
class Game : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    // use LibGDX's default Arial font
    val font by lazy { BitmapFont() }
    val assets = AssetManager()
+   private val context = Context()
    // ...

    override fun dispose() {
-       batch.dispose()
-       font.dispose()
-       assets.dispose()
+       context.dispose()
        super.dispose()
    }
```

Next we need to `register` our _providers_ for the different classes. Since we only need **singletons** in our game we will call `bindSingleton`. But there are also other possibilities. Refer to the [LibKTX documentation](https://github.com/libktx/ktx/blob/master/inject/README.md) for more details. <br>

Example: a call to `context.bindSingleton<Batch>(SpriteBatch())` will register **SpriteBatch** as a provider in case a **Batch** is needed. This means that if a method requires a **Batch** then a call to `context.inject()` will provide the registered SpriteBatch instance. If you omit `<Batch>` then SpriteBatch will only be used as a provider for SpriteBatch parameters/variables. Below is the entire **register** part of our **Game** class. Note the multiple calls to `inject` when creating the **LoadingScreen** which I will explain later (the important part here is to understand that the compiler will already know which type of provider to use for each specific type of parameter in the LoadingScreen constructor).

```Kotlin
override fun create() {
    addScreen(LoadingScreen(this))
    context.register {
        bindSingleton<Batch>(SpriteBatch())
        bindSingleton(BitmapFont())
        bindSingleton(AssetManager())
        // The camera ensures we can render using our target resolution of 800x480
        //    pixels no matter what the screen resolution is.
        bindSingleton(OrthographicCamera().apply { setToOrtho(false, 800f, 480f) })

        addScreen(LoadingScreen(this@Game, inject(), inject(), inject(), inject()))
    }
    setScreen<LoadingScreen>()
    super.create()
}
```

The second thing that we change in this part is the parameters of our screen constructors. As a best practice you should always specify what kind of things your class expects in order to work properly. By passing only **game** or **context** to our screens we do not really know what kind of things are accessed and therefore we also do not know what should already be initialized at that stage. This also makes it more complicated when writing **unit tests**. Also, imagine that you remove something from your context or replace it. The compiler will not warn you because `inject()` will work from its point of view. At runtime though you will get an exception since the provider is missing or delivers a different instance. To avoid all of that we specify now exactly what our screens are expecting:

```Kotlin
class LoadingScreen(private val game: Game,
                    private val batch: Batch,
                    private val font: BitmapFont,
                    private val assets: AssetManager,
                    private val camera: OrthographicCamera) : KtxScreen {
    // ...
}

class GameScreen(private val batch: Batch,
                 private val font: BitmapFont,
                 assets: AssetManager,
                 private val camera: OrthographicCamera) : KtxScreen {
    // ...
}
```

***

The final code can be checked out with the [06-inject branch](https://github.com/Quillraven/SimpleKtxGame/tree/06-inject). Again, using a context is by no means mandatory but in my opinion it adds a nice way of dealing with _global_ game components and it also helps with **disposing** stuff in a way you cannot miss the `dispose` call anymore. <br>
With all of that there is nothing more I can teach you in this tutorial. In case you are interested there is an [additional part](https://github.com/Quillraven/SimpleKtxGame/wiki/Ashley) to show how to use **ashley entity component system** with **LibKTX extensions** for our game. <br>
Thanks for reading! I hope you enjoyed it!