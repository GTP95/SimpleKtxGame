package com.demoktx.game

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.pool
import ktx.async.KtxAsync
import ktx.collections.iterate
import ktx.log.logger
import ktx.graphics.use
import ktx.assets.invoke
import ktx.inject.Context
import ktx.inject.register

private val log = logger<GameScreen>()

class DemoGame : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val font by lazy { BitmapFont() }
    val assets = AssetManager()
    private val context = Context()

    override fun create() {
        KtxAsync.initiate()
        Gdx.app.setLogLevel(Application.LOG_DEBUG)
        context.register {
            bindSingleton<Batch>(SpriteBatch())
            bindSingleton(BitmapFont())
            bindSingleton(AssetManager())
            // The camera ensures we can render using our target resolution of 800x480
            //    pixels no matter what the screen resolution is.
            bindSingleton(OrthographicCamera().apply { setToOrtho(false, 800f, 480f) })

            addScreen(LoadingScreen(this@DemoGame, inject(), inject(), inject(), inject()))
        }
        setScreen<LoadingScreen>()
        super.create()
    }

    override fun dispose() {
        context.dispose()
        super.dispose()
    }
}

class LoadingScreen(private val game: DemoGame,
                    private val batch: Batch,
                    private val font: BitmapFont,
                    private val assets: AssetManager,
                    private val camera: OrthographicCamera) : KtxScreen {

    override fun render(delta: Float) {
        game.assets.update()    // continue loading our assets
        camera.update()
        game.batch.projectionMatrix = camera.combined

        game.batch.use {
            game.font.draw(it, "Welcome to Drop!!! ", 100f, 150f)
            if(assets.isFinished) game.font.draw(it, "Tap anywhere to begin!", 100f, 100f)
            else game.font.draw(it, "Loading assets...", 100f, 100f)
        }

        if (Gdx.input.isTouched && assets.isFinished) {    //transition to game screen
            game.addScreen(GameScreen(batch, font, assets, camera))
            game.setScreen<GameScreen>()
            game.removeScreen<LoadingScreen>()
            dispose()
        }
    }

    override fun show() {
        MusicAssets.entries.forEach { game.assets.load(it) }
        SoundAssets.entries.forEach { game.assets.load(it) }
        TextureAtlasAssets.entries.forEach { game.assets.load(it) }
    }
}

class GameScreen(private val batch: Batch,
                 private val font: BitmapFont,
                 assets: AssetManager,
                 private val camera: OrthographicCamera) : KtxScreen {
    // load the images for the droplet & bucket, 64x64 pixels each
    private val dropImage = assets[TextureAtlasAssets.Game].findRegion("drop")
    private val bucketImage = assets[TextureAtlasAssets.Game].findRegion("bucket")
    private val background = assets[TextureAtlasAssets.Game].findRegion("background")
    // load the drop sound effect and the rain background music
    private val dropSound = assets[SoundAssets.Drop]
    private val rainMusic = assets[MusicAssets.Rain].apply { isLooping = true }
    // The camera ensures we can render using our target resolution of 800x480
    //    pixels no matter what the screen resolution is.

    // create a Rectangle to logically represent the bucket
    // center the bucket horizontally
    // bottom left bucket corner is 20px above
    private val bucket = Rectangle(800f / 2f - 64f / 2f, 20f, 64f, 64f)
    // create the touchPos to store mouse click position
    private val touchPos = Vector3()
    // create the raindrops pool
    private val raindropsPool = pool { Rectangle() } // pool to reuse raindrop rectangles
    private val activeRaindrops = Array<Rectangle>() // gdx, not Kotlin Array
    private var lastDropTime = 0L
    private var dropsGathered = 0

    private fun spawnRaindrop() {
        activeRaindrops.add(raindropsPool().set(MathUtils.random(0f, 800f - 64f), 480f, 64f, 64f))
        lastDropTime = TimeUtils.nanoTime()
    }

    override fun render(delta: Float) {
        // generally good practice to update the camera's matrices once per frame
        camera.update()

        // tell the SpriteBatch to render in the coordinate system specified by the camera.
        batch.projectionMatrix = camera.combined

        // begin a new batch and draw the bucket and all drops
        batch.use { batch ->
            batch.draw(background, 0f, 0f)
            font.draw(batch, "Drops Collected: $dropsGathered", 0f, 480f)
            batch.draw(bucketImage, bucket.x, bucket.y, bucket.width, bucket.height)
            for (raindrop in activeRaindrops) {
                batch.draw(dropImage, raindrop.x, raindrop.y)
            }
        }

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
        activeRaindrops.iterate { raindrop, iterator ->
            raindrop.y -= 200 * delta

            if (raindrop.y + 64 < 0) {
                iterator.remove()
                raindropsPool(raindrop)
                log.debug { "Missed a raindrop! Pool free objects: ${raindropsPool.free}" }
            }

            if (raindrop.overlaps(bucket)) {
                dropsGathered++
                dropSound.play()
                iterator.remove()
                raindropsPool(raindrop)
                log.debug { "Gathered a raindrop! Pool free objects: ${raindropsPool.free}" }
            }
        }
    }

    override fun show() {
        // start the playback of the background music when the screen is shown
        rainMusic.play()
        spawnRaindrop()
    }

    override fun dispose() {
        log.debug { "Disposing ${this.javaClass.simpleName}" }
        dropImage.texture.dispose()
        bucketImage.texture.dispose()
        dropSound.dispose()
        rainMusic.dispose()
    }
}
