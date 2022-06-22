/*
 * File created ~ 24 - 4 - 2021 ~ Leaf
 */

package leaf.cosmere.effects.feruchemy.store;

import leaf.cosmere.constants.Metals;
import leaf.cosmere.effects.feruchemy.FeruchemyEffectBase;
import leaf.cosmere.registry.AttributesRegistry;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;


public class ZincStoreEffect extends FeruchemyEffectBase
{
	public ZincStoreEffect(Metals.MetalType type, MobEffectCategory effectType)
	{
		super(type, effectType);
		addAttributeModifier(
				AttributesRegistry.XP_RATE_ATTRIBUTE.get(),
				"9d50ed05-06f4-4bc2-83bc-d870f93696bf",
				-0.1D,
				AttributeModifier.Operation.ADDITION);
	}

}
