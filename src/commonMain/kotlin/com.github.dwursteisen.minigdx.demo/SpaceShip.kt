package com.github.dwursteisen.minigdx.demo

import com.curiouscreature.kotlin.math.Float3
import com.curiouscreature.kotlin.math.Mat4
import com.curiouscreature.kotlin.math.perspective
import com.curiouscreature.kotlin.math.translation
import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.camera.PerspectiveCamera
import com.github.dwursteisen.minigdx.*
import com.github.dwursteisen.minigdx.ecs.*
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.Position
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.game.GameSystem
import com.github.dwursteisen.minigdx.game.Screen
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.render.Camera
import com.github.dwursteisen.minigdx.render.MeshPrimitive
import com.github.dwursteisen.minigdx.render.MeshPrimitiveRenderStage
import com.github.dwursteisen.minigdx.render.RenderStage


class Player : Component
class Terrain : Component
class Bullet(var fired: Boolean = false) : Component

@ExperimentalStdlibApi
class RotationSystem : System(EntityQuery(MeshPrimitive::class)) {
    override fun update(delta: Seconds, entity: Entity) {
        entity[Position::class].forEach {
            it.rotateY(10f * delta)
        }
    }
}

class PlayerControl : System(EntityQuery(Player::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        if (inputs.isKeyPressed(Key.ARROW_LEFT)) {
            entity[Position::class].forEach {
                it.transformation *= translation(Float3(50f * delta, 0f, 0f))
            }
        } else if (inputs.isKeyPressed(Key.ARROW_RIGHT)) {
            entity[Position::class].forEach {
                it.transformation *= translation(Float3(-50f * delta, 0f, 0f))
            }
        }
    }
}

class TerrainMove : System(EntityQuery(Terrain::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        entity[Position::class].forEach {
            it.transformation *= translation(Float3(0f, 0f, delta * -20f))

            if (it.transformation.position.z < -20f) {
                it.transformation *= translation(Float3(0f, 0f, 200f))
            }
        }
    }
}

class BulletMove : System(EntityQuery(Bullet::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        if(inputs.isKeyJustPressed(Key.SPACE)) {
            entities.filter { it[Bullet::class].any { !it.fired } }.firstOrNull()?.run {
                this[Position::class].forEach {
                    it.transformation *= translation(Float3(it.transformation.translation))
                }
                this[Bullet::class].forEach {
                    it.fired = true
                }
            }
        }

        entity[Position::class].forEach {
            if (it.transformation.position.z < 200f) {
                it.transformation *= translation(Float3(0f, 0f, delta * 20f))
                entity[Bullet::class].forEach {
                    it.fired = false
                }
            }
        }
    }
}

@ExperimentalStdlibApi
class SpaceshipScreen : Screen {

    private val spaceship: Scene by fileHandler.get("spaceship.protobuf")

    override fun createEntities(engine: Engine) {
        // Create the player model
        spaceship.models["Player"]?.let { player ->
            engine.create {
                player.mesh.primitives.forEach { primitive ->
                    add(
                        MeshPrimitive(
                            primitive = primitive,
                            material = spaceship.materials.values.first { it.id == primitive.materialId }
                        )
                    )
                }
                add(Player())
                add(Position(Mat4.fromColumnMajor(*player.transformation.matrix)))
            }
        }

        spaceship.models["Bullet"]?.let { bullet ->
            engine.create {
                bullet.mesh.primitives.forEach { primitive ->
                    add(
                        MeshPrimitive(
                            primitive = primitive,
                            material = spaceship.materials.values.first { it.id == primitive.materialId }
                        )
                    )
                }
                add(Bullet())
                add(Position(Mat4.fromColumnMajor(*bullet.transformation.matrix)))
            }
        }

        // Create terrains
        spaceship.models["Terrain"]?.let { terrain ->
            val model = terrain.mesh.primitives.map { primitive ->
                MeshPrimitive(
                    primitive = primitive,
                    material = spaceship.materials.values.first { it.id == primitive.materialId }
                )

            }
            (0..10).forEach { index ->
                engine.create {
                    model.forEach { add(it) }
                    add(Terrain())
                    add(
                        Position(
                            Mat4.fromColumnMajor(*terrain.transformation.matrix) * translation(
                                Float3(
                                    0f,
                                    0f,
                                    index * 20f
                                )
                            )
                        )
                    )
                }
            }
        }

        // Create the camera
        spaceship.perspectiveCameras.values.forEach { camera ->
            camera as PerspectiveCamera
            engine.create {
                add(
                    Camera(
                        projection = perspective(
                            fov = camera.fov,
                            aspect = 1f, // FIXME,
                            near = camera.near,
                            far = camera.far
                        )
                    )
                )
                add(
                    Position(
                        transformation = Mat4.fromColumnMajor(
                            *camera.transformation.matrix
                        ), way = -1f
                    )
                )
            }
        }
    }

    override fun createSystems(): List<System> {
        return listOf(PlayerControl(), TerrainMove(), BulletMove())
    }
}

@ExperimentalStdlibApi
class SpaceShip(gl: GL) : GameSystem(gl, SpaceshipScreen())
