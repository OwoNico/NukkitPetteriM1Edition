package cn.nukkit.entity.passive;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityWalking;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

public abstract class EntityWalkingAnimal extends EntityWalking implements EntityAnimal {

    public EntityWalkingAnimal(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    private int panicTicks = 0;

    /**
     * todo 当玩家靠近友好生物时，看向玩家
     */
    @Getter
    @Setter
    private int stayLookAt = 0;
    @Getter
    @Setter
    private int startLookAt = 0;

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        if (!this.isAlive()) {
            if (++this.deadTicks >= 23) {
                this.close();
                return false;
            }
            return true;
        }

        if (this.panicTicks > 0) {
            this.panicTicks--;
            if (panicTicks == 0) {
                doPanic(false);
            }
        }

        int tickDiff = currentTick - this.lastUpdate;
        this.lastUpdate = currentTick;
        this.entityBaseTick(tickDiff);

        // fix look at
        Optional.ofNullable(this.updateMove(tickDiff))
                .ifPresent(this::lookAt);
        return true;
    }

    public void doPanic(boolean panic) {
        if (panic) {
            int time = Utils.rand(60, 100);
            this.panicTicks = time;
            this.stayTime = 0;
            this.moveTime = time;
            this.moveMultiplier = 1.8f;
        } else {
            this.moveMultiplier = 1.0f;
        }
    }

    @Override
    public boolean attack(EntityDamageEvent ev) {
        super.attack(ev);

        if (!ev.isCancelled() && ev instanceof EntityDamageByEntityEvent) {
            if (((EntityDamageByEntityEvent) ev).getDamager() instanceof Player) {
                this.doPanic(true);
            }
        }

        return true;
    }

    @Override
    public boolean canTarget(Entity entity) {
        return ((this.isInLove() || entity instanceof Player) && entity.canBeFollowed());
    }

}
