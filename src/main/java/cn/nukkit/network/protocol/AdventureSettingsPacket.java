package cn.nukkit.network.protocol;

import cn.nukkit.Player;
import cn.nukkit.api.API;
import lombok.ToString;

import static cn.nukkit.api.API.Definition.UNIVERSAL;
import static cn.nukkit.api.API.Usage.DEPRECATED;

/**
 * @author Nukkit Project Team
 */
@ToString
@API(usage = DEPRECATED, definition = UNIVERSAL) //在1.19.30弃用
public class AdventureSettingsPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.ADVENTURE_SETTINGS_PACKET;

    public static final int PERMISSION_NORMAL = 0;
    public static final int PERMISSION_OPERATOR = 1;
    public static final int PERMISSION_HOST = 2;
    public static final int PERMISSION_AUTOMATION = 3;
    public static final int PERMISSION_ADMIN = 4;

    /**
     * This constant is used to identify flags that should be set on the second field. In a sensible world, these
     * flags would all be set on the same packet field, but as of MCPE 1.2, the new abilities flags have for some
     * reason been assigned a separate field.
     */
    public static final int BITFLAG_SECOND_SET = 65536;

    public static final int WORLD_IMMUTABLE = 0x01;
    public static final int NO_PVM = 0x02;
    public static final int NO_MVP = 0x04;
    public static final int SHOW_NAME_TAGS = 0x10;
    public static final int AUTO_JUMP = 0x20;
    public static final int ALLOW_FLIGHT = 0x40;
    public static final int NO_CLIP = 0x80;
    public static final int WORLD_BUILDER = 0x100;
    public static final int FLYING = 0x200;
    public static final int MUTED = 0x400;
    public static final int MINE = 0x01 | BITFLAG_SECOND_SET;
    public static final int DOORS_AND_SWITCHES = 65538;
    public static final int OPEN_CONTAINERS = 65540;
    public static final int ATTACK_PLAYERS = 65544;
    public static final int ATTACK_MOBS = 65552;
    public static final int OPERATOR = 65568;
    public static final int TELEPORT = 65664;
    public static final int BUILD = 0x100 | BITFLAG_SECOND_SET;
    public static final int DEFAULT_LEVEL_PERMISSIONS = 0x200 | BITFLAG_SECOND_SET;

    public long flags = 0;

    public long commandPermission = PERMISSION_NORMAL;

    public long flags2 = -1; // This may be incorrect but DON'T TOUCH IT!

    public long playerPermission = Player.PERMISSION_MEMBER;

    public long customFlags;

    public long entityUniqueId; //This is a little-endian long, NOT a var-long. (WTF Mojang)

    public void decode() {
        this.flags = getUnsignedVarInt();
        this.commandPermission = getUnsignedVarInt();
        this.flags2 = getUnsignedVarInt();
        this.playerPermission = getUnsignedVarInt();
        this.customFlags = getUnsignedVarInt();
        this.entityUniqueId = getLLong();
    }

    public void encode() {
        this.reset();
        this.putUnsignedVarInt(this.flags);
        this.putUnsignedVarInt(this.commandPermission);
        this.putUnsignedVarInt(this.flags2);
        this.putUnsignedVarInt(this.playerPermission);
        this.putUnsignedVarInt(this.customFlags);
        this.putLLong(this.entityUniqueId);
    }

    public boolean getFlag(int flag) {
        if ((flag & BITFLAG_SECOND_SET) != 0) {
            return (this.flags2 & flag) != 0;
        }
        return (this.flags & flag) != 0;
    }

    public void setFlag(int flag, boolean value) {
        boolean flags = (flag & BITFLAG_SECOND_SET) != 0;

        if (value) {
            if (flags) {
                this.flags2 |= flag;
            } else {
                this.flags |= flag;
            }
        } else {
            if (flags) {
                this.flags2 &= ~flag;
            } else {
                this.flags &= ~flag;
            }
        }
    }

    @Override
    public byte pid() {
        return NETWORK_ID;
    }
}
