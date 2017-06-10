package net.minecraft.server;

import javax.annotation.concurrent.Immutable;

@Immutable
public enum EnumCreatureType {

    MONSTER(IMonster.class, 70, Material.AIR, false, false), CREATURE(EntityAnimal.class, 10, Material.AIR, true, true), AMBIENT(EntityAmbient.class, 15, Material.AIR, true, false), WATER_CREATURE(EntityWaterAnimal.class, 5, Material.WATER, true, false);

    private final Class<? extends IAnimal> e;
    private final int f;
    private final Material g;
    private final boolean h;
    private final boolean i;

    private EnumCreatureType(Class<? extends IAnimal> oclass, int i, Material material, boolean flag, boolean flag1) {
        this.e = oclass;
        this.f = i;
        this.g = material;
        this.h = flag;
        this.i = flag1;
    }

    public Class<? extends IAnimal> a() {
        return this.e;
    }

    public int getMaxNumberOfCreature() { return this.b(); } // OBFHELPER
    public int b() {
        return this.f;
    }

    public boolean isPeaceful() { return this.d(); } // OBFHELPER
    public boolean d() {
        return this.h;
    }

    public boolean isAnimal() { return this.e(); } // OBFHELPER
    public boolean e() {
        return this.i;
    }
    
    public Material getCreatureMaterial() {
    	return this.g;
    }
}
