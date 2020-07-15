package com.github.dwursteisen.minigdx.demo

import com.curiouscreature.kotlin.math.Mat4
import com.curiouscreature.kotlin.math.perspective
import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.camera.PerspectiveCamera
import com.github.dwursteisen.minigdx.GL
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.MeshPrimitive
import com.github.dwursteisen.minigdx.ecs.components.Position
import com.github.dwursteisen.minigdx.ecs.createFrom
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.game.GameSystem
import com.github.dwursteisen.minigdx.game.Screen

@ExperimentalStdlibApi
class CameraScreen(override val gameContext: GameContext) : Screen {

    private val spaceship: Scene by gameContext.fileHandler.get("cameras.protobuf")

    override fun createEntities(engine: Engine) {
        // Create the player model
        spaceship.models.values.forEach { player ->
            engine.create {
                player.mesh.primitives.forEach { primitive ->
                    add(
                        MeshPrimitive(
                            primitive = primitive,
                            material = spaceship.materials.values.first { it.id == primitive.materialId }
                        )
                    )
                }
                val transformation = Mat4.fromColumnMajor(*player.transformation.matrix)
                add(Position(transformation))
            }
        }

        // Create the camera
        val (_, _, camera) = spaceship.perspectiveCameras.values.toList()
        camera as PerspectiveCamera
        engine.createFrom(camera, gameContext)
    }

    override fun createSystems(): List<System> {
        return emptyList()
    }
}

@ExperimentalStdlibApi
class CameraDemo(gameContext: GameContext) : GameSystem(gameContext, CameraScreen(gameContext))
