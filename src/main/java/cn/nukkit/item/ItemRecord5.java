package cn.nukkit.item;

public class ItemRecord5 extends ItemRecord {

    public ItemRecord5() {
        this(0, 1);
    }

    public ItemRecord5(Integer meta) {
        this(meta, 1);
    }

    public ItemRecord5(Integer meta, int count) {
        super(RECORD_5, meta, count);
        name = "Music Disc 5";
    }

    @Override
    public String getSoundId() {
        return "record.5";
    }
}