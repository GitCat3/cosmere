/*
 * File created ~ 24 - 4 - 2021 ~ Leaf
 */

package leaf.cosmere.items;

import leaf.cosmere.constants.Metals;
import leaf.cosmere.registry.ItemsRegistry;
import leaf.cosmere.utils.helpers.CompoundNBTHelper;
import leaf.cosmere.utils.helpers.TextHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static leaf.cosmere.constants.Constants.Strings.CONTAINED_METALS;

public class MetalVialItem extends BaseItem implements IHasMetalType
{
	private final String metal_ids = "metalIDs";
	private final String metal_amounts = "metalAmounts";
	private final int MAX_METALS_COUNT = 16;

	@Override
	public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems)
	{
		super.fillItemCategory(pCategory, pItems);

		if (allowdedIn(pCategory))
		{
			final ItemStack filled = new ItemStack(this);
			for (int i = 0; i < 16; i++)
			{
				addMetals(filled, i, 1);
			}
			pItems.add(filled);
		}
	}

	private CompoundTag getContainedMetalsTag(ItemStack stack)
	{
		return stack.getOrCreateTagElement("metals_contained");
	}

	public boolean isFull(ItemStack stack)
	{
		return containedMetalCount(stack) >= getMaxFillCount(stack);
	}

	public int getMaxFillCount(ItemStack stack)
	{
		final CompoundTag stackTags = stack.getOrCreateTag();
		final String max_count = "max_count";
		return stackTags.contains(max_count) ? stackTags.getInt(max_count) : MAX_METALS_COUNT;
	}

	@Nonnull
	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack)
	{
		//same drink time as normal potions
		return 16;
	}


	@Nonnull
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, @Nonnull InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if (player.canEat(true) && containedMetalCount(player.getItemInHand(hand)) > 0)
		{
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		return InteractionResultHolder.pass(stack);
	}


	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving)
	{
		Player playerentity = entityLiving instanceof Player ? (Player) entityLiving : null;

		if (!worldIn.isClientSide)
		{
			//for each metal in the vial
			Map<Integer, Integer> metalsInVial = getStoredMetalsMap(getContainedMetalsTag(stack));

			metalsInVial.entrySet().forEach(metalInfo ->
			{
				// MetalName x Value
				Metals.MetalType metalType = Metals.MetalType.valueOf(metalInfo.getKey()).get();
				MetalNuggetItem.consumeNugget(entityLiving, metalType, null);
			});
		}


		if (playerentity == null || !playerentity.getAbilities().instabuild)
		{
			if (stack.isEmpty())
			{
				return new ItemStack(ItemsRegistry.METAL_VIAL.get());
			}

			if (playerentity != null)
			{
				if (stack.getCount() > 1)
				{
					//split 1 off the stack, if more than one
					//drain that new stack
					ItemStack splitStack = stack.split(1);
					emptyMetals(splitStack);

					if (!playerentity.addItem(splitStack) && !playerentity.level.isClientSide)
					{
						ItemEntity entity = new ItemEntity(playerentity.getCommandSenderWorld(), playerentity.position().x(), playerentity.position().y(), playerentity.position().z(), splitStack);
						playerentity.getCommandSenderWorld().addFreshEntity(entity);
					}
				}
				else
				{
					emptyMetals(stack);
				}
			}
		}

		return stack;
	}

	public int containedMetalCount(ItemStack stack)
	{
		int count = 0;
		int[] metalAmounts = CompoundNBTHelper.getIntArray(getContainedMetalsTag(stack), metal_amounts);

		for (int metalCount : metalAmounts)
			count += metalCount;

		return count;
	}

	public void addMetals(ItemStack stack, int metalID, int count)
	{
		//todo refactor this? seems so convoluted compared to what I'm used to

		//get and add
		CompoundTag nbt = getContainedMetalsTag(stack);

		Map<Integer, Integer> sorted = getStoredMetalsMap(nbt);

		if (sorted.containsKey(metalID))
		{
			count += sorted.get(metalID);
		}

		sorted.put(metalID, count);
		List<Integer> keys = new ArrayList<>(sorted.keySet());
		List<Integer> values = new ArrayList<>(sorted.values());

		CompoundNBTHelper.setIntArray(nbt, metal_ids, keys);
		CompoundNBTHelper.setIntArray(nbt, metal_amounts, values);
	}

	private Map<Integer, Integer> getStoredMetalsMap(CompoundTag nbt)
	{
		int[] metalIDs = CompoundNBTHelper.getIntArray(nbt, metal_ids);
		int[] metalAmounts = CompoundNBTHelper.getIntArray(nbt, metal_amounts);

		Map<Integer, Integer> sorted = IntStream.range(0, metalIDs.length).boxed()
				.collect(Collectors.toMap(
						i -> metalIDs[i], i -> metalAmounts[i],
						(i, j) -> i, LinkedHashMap::new));
		return sorted;
	}

	public void emptyMetals(ItemStack stack)
	{
		CompoundTag nbt = getContainedMetalsTag(stack);
		nbt.remove(metal_ids);
		nbt.remove(metal_amounts);
	}

	@Override
	public boolean isBarVisible(ItemStack stack)
	{
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack)
	{
		return getBarWidth(containedMetalCount(stack), MAX_METALS_COUNT);
	}

	@Override
	public int getBarColor(ItemStack stack)
	{
		return getBarColour(containedMetalCount(stack), MAX_METALS_COUNT);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn)
	{
		Map<Integer, Integer> sorted = getStoredMetalsMap(getContainedMetalsTag(stack));
		tooltip.add(TextHelper.createTranslatedText(CONTAINED_METALS));
		sorted.entrySet().stream().forEach(metalInfo ->
		{
			// MetalName x Value
			String metalName = Metals.MetalType.valueOf(metalInfo.getKey()).get().getName();

			String metalTranslation = String.format("item.cosmere.%s_nugget", metalName);
			tooltip.add(TextHelper.createTranslatedText(metalTranslation).append(TextHelper.createText(String.format(": x%s", metalInfo.getValue()))));

		});

		tooltip.add(TextHelper.createText(String.format("%s / %s", containedMetalCount(stack), MAX_METALS_COUNT)));

	}

	@Override
	public Metals.MetalType getMetalType()
	{
		return Metals.MetalType.IRON;
	}

	public void addMetals(ItemStack newMetalVialStack, ItemStack oldMetalVialStack)
	{
		Map<Integer, Integer> sorted = getStoredMetalsMap(getContainedMetalsTag(oldMetalVialStack));
		for (Integer metalID : sorted.keySet())
		{
			addMetals(newMetalVialStack, metalID, sorted.get(metalID));
		}
	}
}
