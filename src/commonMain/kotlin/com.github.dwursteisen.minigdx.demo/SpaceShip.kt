package com.github.dwursteisen.minigdx.demo

import com.curiouscreature.kotlin.math.Float3
import com.curiouscreature.kotlin.math.Mat4
import com.curiouscreature.kotlin.math.translation
import com.dwursteisen.minigdx.scene.api.Scene
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.MeshPrimitive
import com.github.dwursteisen.minigdx.ecs.components.Position
import com.github.dwursteisen.minigdx.ecs.createFrom
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.ecs.systems.TemporalSystem
import com.github.dwursteisen.minigdx.file.Content
import com.github.dwursteisen.minigdx.game.GameSystem
import com.github.dwursteisen.minigdx.game.Screen
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

class PlayerControl(private val inputs: InputHandler) : System(EntityQuery(Player::class)) {

    private fun lerp(a: Float, b: Float, f: Float): Float {
        return a + f * (b - a)
    }

    override fun update(delta: Seconds, entity: Entity) {
        val position = entity.get(Position::class)

        position.setRotationZ(0f)
        if (inputs.isKeyPressed(Key.ARROW_LEFT)) {
            position.translate(50f * delta)
            val player = entity.get(Player::class)
            player.rotation = max(-1f, player.rotation - delta)
        } else if (inputs.isKeyPressed(Key.ARROW_RIGHT)) {
            position.translate(-50f * delta)
            val player = entity.get(Player::class)
            player.rotation = min(1f, player.rotation + delta)
        }
        position.setTranslate(x = max(min(20f, position.translation.x), -20f))

        val player = entity.get(Player::class)
        position.setRotationZ(player.rotation * 180f)
        player.rotation = lerp(0f, player.rotation, 0.9f)
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

    private val players by interested(EntityQuery(Player::class))

    override fun update(delta: Seconds) {
        if (inputs.isKeyJustPressed(Key.SPACE)) {
            entities.firstOrNull { !it.get(Bullet::class).fired }?.run {
                val playerPosition = players.first().get(Position::class)
                val translation = Vector3(
                    playerPosition.translation.x * playerPosition.scale.x,
                    playerPosition.translation.y * playerPosition.scale.y,
                    playerPosition.translation.z * playerPosition.scale.z
                )
                this.get(Position::class).setTranslate(translation)
                this.get(Bullet::class).fired = true
            }
        }
        super.update(delta)
    }

    override fun update(delta: Seconds, entity: Entity) {
        val position = entity.get(Position::class)
        if (position.transformation.position.z < 100f) {
            position.translate(z = delta * 120f)
        } else {
            entity.get(Bullet::class).fired = false
        }
    }
}

@ExperimentalStdlibApi
class MonsterSpawnSystem(
    private val model: Content<MeshPrimitive>,
    private val engine: Engine
) : TemporalSystem(2f) {

    override fun timeElapsed() {
        engine.create {
            val aa by model
            add(aa)
            add(Position().setTranslate(0f, 0f, 40f))
            add(Monster())
        }
    }

    override fun update(delta: Seconds, entity: Entity) = Unit
}

class MonsterSystem : System(EntityQuery(Monster::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        val monster = entity.get(Monster::class)
        monster.time += delta
        entity.get(Position::class).translate(z = -10f * delta, x = 0.1f * cos(monster.time * 2f))

        if(monster.time > 10f) {
            remove(entity) // hum.
        }
    }
}

@ExperimentalStdlibApi
class SpaceshipScreen(override val gameContext: GameContext) : Screen {

    private val spaceship: Scene by gameContext.fileHandler.get("spaceship.protobuf")

    private val monsterReference: Content<MeshPrimitive> = Content("fake")

    override fun createEntities(engine: Engine) {
        // Create the player model
        spaceship.models["Player"]?.let { player ->
            val playerEntity = engine.createFrom(player, spaceship)
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
            (0..100).forEach { _ ->
                engine.create {
                    models.forEach { add(it) }
                    add(Bullet())
                    add(Position(Mat4.fromColumnMajor(*bullet.transformation.matrix)))
                }
            }
        }

        spaceship.models["Monster"]?.let { model ->
            val monster = engine.createFrom(model, spaceship)
            monsterReference.load(monster.get(MeshPrimitive::class))
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
        spaceship.orthographicCameras.values.forEach { camera ->
            engine.createFrom(camera, gameContext)
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(
            PlayerControl(gameContext.input),
            TerrainMove(),
            BulletMove(gameContext.input),
            MonsterSpawnSystem(monsterReference, engine),
            MonsterSystem()
        )
    }
}

@ExperimentalStdlibApi
class SpaceShip(gameContext: GameContext) : GameSystem(gameContext, SpaceshipScreen(gameContext))
