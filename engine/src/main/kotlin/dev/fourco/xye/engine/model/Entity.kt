package dev.fourco.xye.engine.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class EntityId(val value: Int)

@Serializable
data class Entity(
    val id: EntityId,
    val kind: EntityKind,
    val pos: Position,
    val props: EntityProps = EntityProps(),
)

@Serializable
enum class EntityKind {
    Player, Wall, SoftBlock, BlackHole,
    Gem, StarGem,
    PushBlock, RoundBlock, PullBlock,
    MagnetH, MagnetV,
    Door, Key,
    Teleporter, Trigger,
    SliderUp, SliderDown, SliderLeft, SliderRight,
    RockyUp, RockyDown, RockyLeft, RockyRight,
    Shooter, TimerBlock, Monster, Hazard,
    ConveyorUp, ConveyorDown, ConveyorLeft, ConveyorRight,
}

@Serializable
data class EntityProps(
    val color: Int? = null,
    val direction: Direction? = null,
    val pairId: Int? = null,
    val triggerId: Int? = null,
    val timerTicks: Int? = null,
    val shootInterval: Int? = null,
    val shootKind: EntityKind? = null,
    val isRound: Boolean = false,
)
