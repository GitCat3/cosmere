/*
 * File created ~ 24 - 4 - 2021 ~ Leaf
 */

package leaf.cosmere.manifestation.feruchemy;

import leaf.cosmere.cap.entity.ISpiritweb;
import leaf.cosmere.charge.MetalmindChargeHelper;
import leaf.cosmere.constants.Manifestations;
import leaf.cosmere.constants.Metals;
import leaf.cosmere.items.IHasMetalType;
import leaf.cosmere.manifestation.ManifestationBase;
import leaf.cosmere.registry.AttributesRegistry;
import leaf.cosmere.registry.EffectsRegistry;
import leaf.cosmere.utils.helpers.EffectsHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

public class FeruchemyBase extends ManifestationBase implements IHasMetalType
{
	protected final Metals.MetalType metalType;

	public FeruchemyBase(Metals.MetalType metalType)
	{
		super(Manifestations.ManifestationTypes.FERUCHEMY, metalType.getColorValue());
		this.metalType = metalType;
	}

	@Override
	public int getPowerID()
	{
		return metalType.getID();
	}

	@Override
	public Metals.MetalType getMetalType()
	{
		return this.metalType;
	}

	@Override
	public boolean modeWraps(ISpiritweb data)
	{
		return false;
	}

	//storing is positive, eg adding to store
	@Override
	public int modeMax(ISpiritweb data)
	{
		final double strength = getStrength(data, false);
		return Mth.fastFloor(strength / 3);
	}

	//tapping is negative, eg taking from store
	@Override
	public int modeMin(ISpiritweb data)
	{
		final double strength = getStrength(data, false);
		return -(Mth.fastFloor(strength));
	}

	@Override
	public void onModeChange(ISpiritweb data)
	{
		super.onModeChange(data);

		if (getMode(data) == 0)
		{
			//todo check if removing effects on mode change is wise. May be better to let them run out as they have already "paid" for them.
			data.getLiving().removeEffect(EffectsRegistry.STORING_EFFECTS.get(this.metalType).get());
			data.getLiving().removeEffect(EffectsRegistry.TAPPING_EFFECTS.get(this.metalType).get());
		}
	}

	public boolean isStoring(ISpiritweb data)
	{
		return getMode(data) > 0;
	}

	public boolean isTapping(ISpiritweb data)
	{
		return getMode(data) < 0;
	}

	public boolean canAfford(ISpiritweb data, boolean simulate)
	{
		int cost = getCost(data);
		final ItemStack metalmind = MetalmindChargeHelper.adjustMetalmindChargeExact(data, metalType, -cost, !simulate, true);

		if (metalmind != null)
		{
			return true;
		}

		if (!simulate)
		{
			final int mode = getMode(data);
			if (mode < 0)
			{
				//move towards turning off feruchemy.
				data.setMode(this, mode + 1);
			}
		}

		return false;
	}

	public int getCost(ISpiritweb data)
	{
		int mode = data.getMode(this);

		// if we are tapping
		//check if there is charges to tap
		if (mode < 0)
		{
			//wanting to tap
			//get cost
			return mode >= -modeMax(data) ? mode : -(Mth.absFloor(Math.pow(Mth.abs(mode), 1.5d)));
		}
		//if we are storing
		//check if there is space to store
		else if (mode > 0)
		{
			return mode;
		}
		return 0;
	}


	@Override
	public void tick(ISpiritweb data)
	{
		//don't check every tick.
		LivingEntity livingEntity = data.getLiving();

		int mode = getMode(data);

		if ((livingEntity.tickCount % 20 != 0) || mode == 0)
		{
			//if not active tick, or mode is off
			return;
		}

		if (canAfford(data, false))//success
		{
			applyEffectTick(data);
		}
	}

	@Override
	public void applyEffectTick(ISpiritweb data)
	{
		int mode = getMode(data);
		MobEffect effect = getEffect(mode);
		MobEffectInstance currentEffect = EffectsHelper.getNewEffect(effect, Math.abs(mode) - 1);

		if (effect == null)
		{
			return;
		}
		data.getLiving().removeEffect(effect);
		data.getLiving().addEffect(currentEffect);
	}

	protected MobEffect getEffect(int mode)
	{
		if (mode == 0)
		{
			return null;
		}
		else if (mode < 0)
		{
			return metalType.getTappingEffect();
		}
		else
		{
			return metalType.getStoringEffect();
		}

	}

	public double getStrength(ISpiritweb cap, boolean getBaseStrength)
	{
		RegistryObject<Attribute> attributeRegistryObject = AttributesRegistry.FERUCHEMY_ATTRIBUTES.get(metalType);
		AttributeInstance attribute = cap.getLiving().getAttribute(attributeRegistryObject.get());
		if (attribute != null)
		{
			return getBaseStrength ? attribute.getBaseValue() : attribute.getValue();
		}
		return 0;
	}

	@Override
	public RegistryObject<Attribute> getAttribute()
	{
		return AttributesRegistry.FERUCHEMY_ATTRIBUTES.get(metalType);
	}
}
