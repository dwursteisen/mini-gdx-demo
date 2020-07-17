package com.github.dwursteisen.minigdx.demo

import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.camera.PerspectiveCamera
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.createFrom
import com.github.dwursteisen.minigdx.game.GameSystem
import com.github.dwursteisen.minigdx.game.Screen
import com.github.dwursteisen.minigdx.log

@ExperimentalStdlibApi
class AnimationScreen(override val gameContext: GameContext) : Screen {

    private val bird: Scene by gameContext.fileHandler.get("bird.protobuf")

    override fun createEntities(engine: Engine) {
        bird.models.values.forEach { model ->
            log.info("DEMO") { "Create animated model '${model.name}'" }
            engine.createFrom(model, bird, gameContext)
        }

        bird.perspectiveCameras.values.forEach { camera ->
            camera as PerspectiveCamera
            log.info("DEMO") { "Create Camera model '${camera.name}'" }
            engine.createFrom(camera, gameContext)
        }
    }
}

@ExperimentalStdlibApi
class AnimationDemo(gameContext: GameContext) : GameSystem(gameContext, AnimationScreen(gameContext))
