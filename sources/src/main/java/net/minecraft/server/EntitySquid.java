package net.minecraft.server;

import javax.annotation.Nullable;

public class EntitySquid extends EntityWaterAnimal {

    public float a;
    public float b;
    public float c;
    public float bu;
    public float bv;
    public float bw;
    public float bx;
    public float by;
    private float bz;
    private float bA;
    private float bB;
    private float bC;
    private float bD;
    private float bE;

    public EntitySquid(World world) {
        super(world);
        this.setSize(0.8F, 0.8F);
        this.random.setSeed(1 + this.getId());
        this.bA = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
    }

    public static void a(DataConverterManager dataconvertermanager) {
        EntityInsentient.a(dataconvertermanager, EntitySquid.class);
    }

    @Override
	protected void r() {
        this.goalSelector.a(0, new EntitySquid.PathfinderGoalSquid(this));
    }

    @Override
	protected void initAttributes() {
        super.initAttributes();
        this.getAttributeInstance(GenericAttributes.maxHealth).setValue(10.0D);
    }

    @Override
	public float getHeadHeight() {
        return this.length * 0.5F;
    }

    @Override
	protected SoundEffect G() {
        return SoundEffects.gE;
    }

    @Override
	protected SoundEffect bW() {
        return SoundEffects.gG;
    }

    @Override
	protected SoundEffect bX() {
        return SoundEffects.gF;
    }

    @Override
	protected float ci() {
        return 0.4F;
    }

    @Override
	protected boolean playStepSound() {
        return false;
    }

    @Override
	@Nullable
    protected MinecraftKey J() {
        return LootTables.ak;
    }

    @Override
	public void n() {
        super.n();
        this.b = this.a;
        this.bu = this.c;
        this.bw = this.bv;
        this.by = this.bx;
        this.bv += this.bA;
        if (this.bv > 6.283185307179586D) {
            this.bv = (float) (this.bv - 6.283185307179586D);
            if (this.random.nextInt(10) == 0) {
                this.bA = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
            }

            this.world.broadcastEntityEffect(this, (byte) 19);
        }

        if (this.inWater) {
            float f;

            if (this.bv < 3.1415927F) {
                f = this.bv / 3.1415927F;
                this.bx = MathHelper.sin(f * f * 3.1415927F) * 3.1415927F * 0.25F;
                if (f > 0.75D) {
                    this.bz = 1.0F;
                    this.bB = 1.0F;
                } else {
                    this.bB *= 0.8F;
                }
            } else {
                this.bx = 0.0F;
                this.bz *= 0.9F;
                this.bB *= 0.99F;
            }

            this.motX = this.bC * this.bz;
            this.motY = this.bD * this.bz;
            this.motZ = this.bE * this.bz;

            f = MathHelper.sqrt(this.motX * this.motX + this.motZ * this.motZ);
            this.aN += (-((float) MathHelper.c(this.motX, this.motZ)) * 57.295776F - this.aN) * 0.1F;
            this.yaw = this.aN;
            this.c = (float) (this.c + 3.141592653589793D * this.bB * 1.5D);
            this.a += (-((float) MathHelper.c(f, this.motY)) * 57.295776F - this.a) * 0.1F;
        } else {
            this.bx = MathHelper.e(MathHelper.sin(this.bv)) * 3.1415927F * 0.25F;
            this.motX = 0.0D;
            this.motZ = 0.0D;
            if (this.hasEffect(MobEffects.LEVITATION)) {
                this.motY += 0.05D * (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - this.motY;
            } else if (!this.isNoGravity()) {
                this.motY -= 0.08D;
            }

            this.motY *= 0.9800000190734863D;

            this.a = (float) (this.a + (-90.0F - this.a) * 0.02D);
        }

    }

    @Override
	public void g(float f, float f1) {
        this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
    }

    @Override
	public boolean cM() {
        return this.locY > world.paperConfig.squidMinSpawnHeight && this.locY < world.paperConfig.squidMaxSpawnHeight && super.cM(); // Paper - Configurable squid spawn height range
    }

    public void b(float f, float f1, float f2) {
        this.bC = f;
        this.bD = f1;
        this.bE = f2;
    }

    public boolean o() {
        return this.bC != 0.0F || this.bD != 0.0F || this.bE != 0.0F;
    }

    static class PathfinderGoalSquid extends PathfinderGoal {

        private final EntitySquid a;

        public PathfinderGoalSquid(EntitySquid entitysquid) {
            this.a = entitysquid;
        }

        @Override
		public boolean a() {
            return true;
        }

        @Override
		public void e() {
            int i = this.a.bO();

            if (i > 100) {
                this.a.b(0.0F, 0.0F, 0.0F);
            } else if (this.a.getRandom().nextInt(50) == 0 || !this.a.inWater || !this.a.o()) {
                float f = this.a.getRandom().nextFloat() * 6.2831855F;
                float f1 = MathHelper.cos(f) * 0.2F;
                float f2 = -0.1F + this.a.getRandom().nextFloat() * 0.2F;
                float f3 = MathHelper.sin(f) * 0.2F;

                this.a.b(f1, f2, f3);
            }

        }
    }
}
