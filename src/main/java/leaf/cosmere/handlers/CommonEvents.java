/*
 * File created ~ 24 - 4 - 2021 ~ Leaf
 */

package leaf.cosmere.handlers;


import leaf.cosmere.Cosmere;
import leaf.cosmere.blocks.MetalOreBlock;
import leaf.cosmere.commands.CosmereCommand;
import leaf.cosmere.commands.permissions.PermissionManager;
import leaf.cosmere.constants.Metals;
import leaf.cosmere.registry.BlocksRegistry;
import leaf.cosmere.registry.FeatureRegistry;
import leaf.cosmere.registry.ItemsRegistry;
import leaf.cosmere.registry.VillagerProfessionRegistry;
import leaf.cosmere.utils.helpers.LogHelper;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraftforge.common.BasicTrade;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Cosmere.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents
{
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event)
    {
        CosmereCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event)
    {
        PermissionManager.init();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBiomeLoad(BiomeLoadingEvent event)
    {
        Biome.Category biomeCategory = event.getCategory();

        //whitelist only vanilla biomes.
        //todo may have compatibility issues with mining dimension type mods
        //don't really want to deprive people of easier access to ores if they want to play that way. something to look into.
        List<Biome.Category> whitelist =
                Arrays.stream(Biome.Category.values())
                        .filter(biomeType -> biomeType != Biome.Category.NONE && biomeType != Biome.Category.NETHER && biomeType != Biome.Category.THEEND)
                        .collect(Collectors.toList());


        if (whitelist.contains(biomeCategory))
        {
            for (Metals.MetalType metalType : Metals.MetalType.values())
            {
                if (metalType.hasOre())
                {
                    event.getGeneration().addFeature(GenerationStage.Decoration.UNDERGROUND_ORES, FeatureRegistry.ConfiguredFeatures.ORE_FEATURES.get(metalType));
                    LogHelper.debug(String.format("Added %s to: %s", metalType.name().toLowerCase(Locale.ROOT), event.getName()));
                }
            }

        }
    }

    @SubscribeEvent
    public static void registerTrades(VillagerTradesEvent event)
    {
        if (event.getType() == VillagerProfessionRegistry.METAL_TRADER.get())
        {
            for (int i = 1; i <= 5 ; i++)
            {
                final List<VillagerTrades.ITrade> tradesForLevel = event.getTrades().get(i);
                switch (i)
                {
                    case 1:
                        for (RegistryObject<Item> item : ItemsRegistry.METAL_NUGGETS.values())
                        {
                            tradesForLevel.add(new BasicTrade(1, new ItemStack(item.get(), 16),2,1));
                        }
                        break;
                    case 2:
                        for (RegistryObject<Item> item : ItemsRegistry.METAL_RAW_BLEND.values())
                        {
                            tradesForLevel.add(new BasicTrade(10, new ItemStack(item.get()),3,2));
                        }
                        break;
                    case 3:
                        tradesForLevel.add(new BasicTrade(10, new ItemStack(ItemsRegistry.METAL_VIAL.get(),16),2,3));
                        break;
                    case 4:
                        for (RegistryObject<Item> item : ItemsRegistry.METAL_RAW_ORE.values())
                        {
                            tradesForLevel.add(new BasicTrade(23, new ItemStack(item.get()),6,4));
                        }
                        break;
                    case 5:
                        for (RegistryObject<MetalOreBlock> item : BlocksRegistry.METAL_ORE.values())
                        {
                            tradesForLevel.add(new BasicTrade(23, new ItemStack(item.get()),1,5));
                        }
                        break;
                }
            }
        }
    }
}