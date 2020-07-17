package com.github.dwursteisen.minigdx.demo

import com.curiouscreature.kotlin.math.Float3
import com.curiouscreature.kotlin.math.Mat4
import com.curiouscreature.kotlin.math.translation
import com.dwursteisen.minigdx.scene.api.Scene
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.Position
import com.github.dwursteisen.minigdx.ecs.components.gl.MeshPrimitive
import com.github.dwursteisen.minigdx.ecs.createFrom
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.ecs.systems.TemporalSystem
import com.github.dwursteisen.minigdx.game.GameSystem
import com.github.dwursteisen.minigdx.game.Screen
import com.github.dwursteisen.minigdx.graphics.GLResourceClient
import com.github.dwursteisen.minigdx.input.InputHandler
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.math.Vector3
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min


class Player(var rotation: Float = 0f) : Component
class Terrain : Component
class Bullet(var fired: Boolean = false) : Component
class Monster(var time: Float = 0f) : Component

@ExperimentalStdlibApi
class RotationSystem : System(EntityQuery(MeshPrimitive::class)) {
    override fun update(delta: Seconds, entity: Entity) {
        entity.findAll(Position::class).forEach {
            it.rotateY(10f * delta)
        }
    }
}

class PlayerControl(private val engine: Engine, private val resources: GLResourceClient, private val inputs: InputHandler) : System(EntityQuery(Player::class)) {

    private fun lerp(a: Float, b: Float, f: Float): Float {
        return a + f * (b - a)
    }

    override fun update(delta: Seconds, entity: Entity) {
        val position = entity.get(Position::class)

        position.setRotationZ(0f)
        if (inputs.isKeyPressed(Key.ARROW_LEFT)) {
            position.translate(100f * delta)
            val player = entity.get(Player::class)
            player.rotation = max(-1f, player.rotation - delta)
        } else if (inputs.isKeyPressed(Key.ARROW_RIGHT)) {
            position.translate(-100f * delta)
            val player = entity.get(Player::class)
            player.rotation = min(1f, player.rotation + delta)
        }
        position.setTranslate(x = max(min(50f, position.translation.x), -50f))

        val player = entity.get(Player::class)
        position.setRotationZ(player.rotation * 180f)
        player.rotation = lerp(0f, player.rotation, 0.95f)


        if (inputs.isKeyJustPressed(Key.SPACE)) {
            engine.create {
                val translation = Vector3(
                    position.translation.x * position.scale.x,
                    position.translation.y * position.scale.y,
                    position.translation.z * position.scale.z
                )

                add(Bullet())
                add(Position().setTranslate(translation))
                add(resources.get("Bullet"))
            }
        }
    }
}

class TerrainMove : System(EntityQuery(Terrain::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        entity.findAll(Position::class).forEach {
            it.transformation *= translation(Float3(0f, 0f, delta * -20f))

            if (it.transformation.position.z < -20f) {
                it.transformation *= translation(Float3(0f, 0f, 200f))
            }
        }
    }
}

class BulletMove(private val inputs: InputHandler) : System(EntityQuery(Bullet::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        val position = entity.get(Position::class)
        if (position.transformation.position.z < 100f) {
            position.translate(z = delta * 120f)
        } else {
            remove(entity)
        }
    }
}

@ExperimentalStdlibApi
class MonsterSpawnSystem(
    private val resource: GLResourceClient,
    private val engine: Engine
) : TemporalSystem(2f) {

    override fun timeElapsed() {
        engine.create {
            add(resource.get("Monster"))
            add(Position().setTranslate(0f, 0f, 60f))
            add(Monster())
        }
    }

    override fun update(delta: Seconds, entity: Entity) = Unit
}

class MonsterSystem : System(EntityQuery(Monster::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        val monster = entity.get(Monster::class)
        monster.time += delta
        entity.get(Position::class).translate(z = -10f * delta, x = 0.2f * cos(monster.time * 2f))

        if (monster.time > 10f) {
            remove(entity) // hum.
        }
    }
}

@ExperimentalStdlibApi
class SpaceshipScreen(override val gameContext: GameContext) : Screen {

    private val spaceship: Scene by gameContext.fileHandler.get("spaceship.protobuf")

    override fun createEntities(engine: Engine) {
        // Create the player model
        spaceship.models["Player"]?.let { player ->
            val playerEntity = engine.createFrom(player, spaceship, gameContext)
            playerEntity.add(Player())
        }

        // Create the bullet model
        spaceship.models["Bullet"]?.let { bullet ->
            val models = bullet.mesh.primitives.map { primitive ->
                MeshPrimitive(
                    primitive = primitive,
                    material = spaceship.materials.values.first { it.id == primitive.materialId }
                )
            }
            gameContext.glResourceClient.compile("Bullet", models)
        }

        spaceship.models["Monster"]?.let { model ->
            val models = model.mesh.primitives.map { primitive ->
                MeshPrimitive(
                    primitive = primitive,
                    material = spaceship.materials.values.first { it.id == primitive.materialId }
                )
            }
            gameContext.glResourceClient.compile("Monster", models)
        }

        // Create terrains
        spaceship.models["Terrain"]?.let { terrain ->
            val models = terrain.mesh.primitives.map { primitive ->
                MeshPrimitive(
                    primitive = primitive,
                    material = spaceship.materials.values.first { it.id == primitive.materialId }
                )
            }
            gameContext.glResourceClient.compile("Terrain", models)

            (0..10).forEach { index ->
                engine.create {
                    add(models)
                    add(Terrain())
                    add(Position(transformation = Mat4.fromColumnMajor(*terrain.transformation.matrix)).translate(z = index * 20f))
                }
            }
        }

        // Create the camera
        spaceship.orthographicCameras.values.forEach { camera ->
            engine.createFrom(camera, gameContext)
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(
            PlayerControl(engine, gameContext.glResourceClient, gameContext.input),
            TerrainMove(),
            BulletMove(gameContext.input),
            MonsterSpawnSystem(gameContext.glResourceClient, engine),
            MonsterSystem()
        )
    }

    override fun render(engine: Engine, delta: Seconds) {

        super.render(engine, delta)
    }
}

@ExperimentalStdlibApi
class SpaceShip(gameContext: GameContext) : GameSystem(gameContext, SpaceshipScreen(gameContext))
