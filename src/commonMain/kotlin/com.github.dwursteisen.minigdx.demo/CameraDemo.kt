package com.github.dwursteisen.minigdx.demo

import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.camera.PerspectiveCamera
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.createFrom
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.game.GameSystem
import com.github.dwursteisen.minigdx.game.Screen

@ExperimentalStdlibApi
class CameraScreen(override val gameContext: GameContext) : Screen {

    private val cube: Scene by gameContext.fileHandler.get("cameras.protobuf")

    override fun createEntities(engine: Engine) {
        // Create the player model
        cube.models.values.forEach { player ->
            engine.createFrom(player, cube, gameContext)
        }

        // Create the camera
        val (_, _, camera) = cube.perspectiveCameras.values.toList()
        camera as PerspectiveCamera
        engine.createFrom(camera, gameContext)
    }

    override fun createSystems(engine: Engine): List<System> {
        return emptyList()
    }
}

@ExperimentalStdlibApi
class CameraDemo(gameContext: GameContext) : GameSystem(gameContext, CameraScreen(gameContext))
