Seems like you are very interested to learn something if you got that far - very good! This section will be a little longer but it is also a very crucial part of **2D-game development**. <br>
We are going to introduce an [AssetManager](https://github.com/libgdx/libgdx/wiki/Managing-your-assets) which takes care of our assets like textures, music and sounds. Also, we will **pack** our textures and create a [TextureAtlas](https://github.com/libgdx/libgdx/wiki/Texture-packer) to avoid **binding textures** which is a relatively expensive process in OpenGL. To make our life easier we will use the [LibKTX assets](https://github.com/libktx/ktx/blob/master/assets/README.md) extensions. <br>
I will not explain the details of all these concepts in this tutorial as they are already covered in the linked wikis. Please, if you have no experience yet with asset management then check out the different links first. Otherwise you will be overwhelmed by this section.

***

First thing to do? Yeah ... I know you guessed it already ... well, here is our updated **project's build gradle**.

```Diff
    // ...
    api "com.badlogicgames.gdx:gdx:$gdxVersion"
    api "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    api "io.github.libktx:ktx-app:$ktxVersion"
+   api "io.github.libktx:ktx-assets:$ktxVersion"
    api "io.github.libktx:ktx-collections:$ktxVersion"
    api "io.github.libktx:ktx-graphics:$ktxVersion"
    api "io.github.libktx:ktx-log:$ktxVersion"
    // ...
```

Next we will create our **AssetManager** which takes care of loading, unloading and accessing our assets. Let's update our **Game** class accordingly. Note that we are also going to rename our **MainMenuScreen** to **LoadingScreen** as it fits the purpose of that screen better with the upcoming changes to it.

```Diff
class Game : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    // use LibGDX's default Arial font
    val font by lazy { BitmapFont() }
+   val assets = AssetManager()

    override fun create() {
-       addScreen(MainMenuScreen(this))
-       setScreen<MainMenuScreen>()
+       addScreen(LoadingScreen(this))
+       setScreen<LoadingScreen>()
        super.create()
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
+       assets.dispose()
        super.dispose()
    }
}
```

So, now that we have got our **AssetManager** we also want to do something with it so it does not get bored! Let's define our **assets** for the game. We will follow a similar approach as mentioned in the [LibKTX assets documentation](https://github.com/libktx/ktx/blob/master/assets/README.md) which means we are creating **enums** for our assets. We will do that for each _type_ of asset. <br>
We will also add our own **extension methods** to conveniently load and access our assets. We use the **LibKTX load** and **getAsset** extensions for that. Here is the code of our **EAssets** file:

```Kotlin
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import ktx.assets.getAsset
import ktx.assets.load

// sounds
enum class SoundAssets(val path: String) {
    Drop("sounds/drop.wav")
}

inline fun AssetManager.load(asset: SoundAssets) = load<Sound>(asset.path)
inline operator fun AssetManager.get(asset: SoundAssets) = getAsset<Sound>(asset.path)

// music
enum class MusicAssets(val path: String) {
    Rain("music/rain.mp3")
}

inline fun AssetManager.load(asset: MusicAssets) = load<Music>(asset.path)
inline operator fun AssetManager.get(asset: MusicAssets) = getAsset<Music>(asset.path)

// texture atlas
enum class TextureAtlasAssets(val path: String) {
    Game("images/game.atlas")
}

inline fun AssetManager.load(asset: TextureAtlasAssets) = load<TextureAtlas>(asset.path)
inline operator fun AssetManager.get(asset: TextureAtlasAssets) = getAsset<TextureAtlas>(asset.path) 
```

Let's move on to our **MainMenuScreen** and rename it to **LoadingScreen**. This screen is now responsible to load our game assets. We override the `show` method to do that. It uses our `load` extension that we defined for each of our asset _types_.

```Kotlin
override fun show() {
    MusicAssets.values().forEach { game.assets.load(it) }
    SoundAssets.values().forEach { game.assets.load(it) }
    TextureAtlasAssets.values().forEach { game.assets.load(it) }
}
```

With these lines we told our assetmanager to queue our assets for loading! Now we need to tell the manager to really load it. For that we are using the `update` method which could later on be used to show a progressbar for the loading. Alternatively you can call `finishLoading` which blocks the game until all queued assets are loaded. As you can imagine this is not perfect once your assets get bigger and bigger as it might freeze your game for a couple of seconds. With our approach we need to call `update` periodically and that's why we put it into the `render` method. Additionally, we do not change to our **GameScreen** as long as there are assets still loading. Here are the changes to the render method:

```Diff
override fun render(delta: Float) {
+   // continue loading our assets
+   game.assets.update()

    camera.update()
    game.batch.projectionMatrix = camera.combined

    game.batch.use {
        game.font.draw(it, "Welcome to Drop!!! ", 100f, 150f)
+       if (game.assets.isFinished) {
            game.font.draw(it, "Tap anywhere to begin!", 100f, 100f)
+       } else {
+           game.font.draw(it, "Loading assets...", 100f, 100f)
+       }
    }

-   if (Gdx.input.isTouched) {
+   if (Gdx.input.isTouched && game.assets.isFinished) {
        game.addScreen(GameScreen(game))
        game.setScreen<GameScreen>()
        game.removeScreen<LoadingScreen>()
        dispose()
    }
}
```

Almost done! We have our assets loaded in our **LoadingScreen** and we are ready to use them in our **GameScreen**. We will use our **get** extension which is based on **LibKTX's getAsset** to easily access our assets. Also, we can remove the `dispose` method as our assetmanager will now take care of that and we added `assets.dispose()` already to our **Game** class. These are the changes for our GameScreen:

```Diff
class GameScreen(val game: Game) : KtxScreen {
-   // load the images for the droplet & bucket, 64x64 pixels each
-   private val dropImage = Texture(Gdx.files.internal("images/drop.png"))
-   private val bucketImage = Texture(Gdx.files.internal("images/bucket.png"))
-   // load the drop sound effect and the rain background music
-   private val dropSound = Gdx.audio.newSound(Gdx.files.internal("sounds/drop.wav"))
-   private val rainMusic = Gdx.audio.newMusic(Gdx.files.internal("music/rain.mp3")).apply { isLooping = true }
+   private val dropImage = game.assets[TextureAtlasAssets.Game].findRegion("drop")
+   private val bucketImage = game.assets[TextureAtlasAssets.Game].findRegion("bucket")
+   private val dropSound = game.assets[SoundAssets.Drop]
+   private val rainMusic = game.assets[MusicAssets.Rain].apply { isLooping = true }
    // ...

-    override fun dispose() {
-       log.debug { "Disposing ${this.javaClass.simpleName}" }
-       dropImage.dispose()
-       bucketImage.dispose()
-       dropSound.dispose()
-       rainMusic.dispose()
-    }
    // ...
```

And with that we are done for asset management! I hope it wasn't too overwhelming. But I am positive that you are a quick learner! <br>

***

There is one final step missing for this section. You most likely noticed already the **TextureAtlasAssets enum**.I used the [TexturePacker GUI](https://github.com/crashinvaders/gdx-texture-packer-gui/blob/master/README.md) to create a **TextureAtlas**. You can download it [here](https://github.com/crashinvaders/gdx-texture-packer-gui/releases). You can also check out the **gameTexPacker.tpproj** file of the repository which contains the settings I used. It is very important to use **padding** otherwise you will get **artifacts** in your game. From my experience a padding of 4, enabling **Duplicate padding**, **Edge padding** and **Bleeding** removed any artifacts/texture bleedings that I ever had. <br>
Also, note the **File name** which is the name of our atlas that contains all of our images. Here is a screenshot of the settings:
![TexturePacker Settings](https://www.dropbox.com/s/orom5qd4lzmlffx/2019-05-04%2017_22_07-GDX%20Texture%20Packer.png?raw=1)

With our **game.atlas** we no longer **bind** different textures in our GameScreen's `render` method since our bucket and raindrop image are within one big texture instead of two different textures.

***

The final code can be checked out with the [04-assets branch](https://github.com/Quillraven/SimpleKtxGame/tree/04-assets). And with that we are almost done for this tutorial series. <br>
There are three sections outstanding:
* One to avoid the _endless_ **Rectangle** creation for our raindrops by using a **pool**
* One to simplify our **Game** class by using a **context** which takes care of disposing and which makes it more convenient to handle objects that are used in the entire game
* One to showcase the **LibKTX** ashley extensions in case you need an entity component system in your game

Let's take a coffee break or whatever you prefer before moving on to the [next](https://github.com/Quillraven/SimpleKtxGame/wiki/Pool) section.