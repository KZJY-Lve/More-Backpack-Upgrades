package com.kzjy.mobackup.registry;

import com.kzjy.mobackup.Config;
import com.kzjy.mobackup.MoBackup;
import com.kzjy.mobackup.item.DimensionalDepositUpgradeItem;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
import com.kzjy.mobackup.item.DimensionalPickupUpgradeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品注册表
 * 集中注册模组所有的自定义物品（升级卡）
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            MoBackup.MOD_ID);

    // 注册次元磁吸升级
    public static final RegistryObject<DimensionalMagnetUpgradeItem> DIMENSIONAL_MAGNET_UPGRADE = ITEMS.register(
            "dimensional_magnet_upgrade",
            () -> new DimensionalMagnetUpgradeItem(Config.COMMON.dimensionalMagnetRange::get,
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedMagnetUpgrade.filterSlots::get,
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.maxUpgradesPerStorage));

    // 注册次元拾取升级
    public static final RegistryObject<DimensionalPickupUpgradeItem> DIMENSIONAL_PICKUP_UPGRADE = ITEMS.register(
            "dimensional_pickup_upgrade",
            () -> new DimensionalPickupUpgradeItem(
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedPickupUpgrade.filterSlots::get,
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.maxUpgradesPerStorage));

    // 注册次元卸货升级
    public static final RegistryObject<DimensionalDepositUpgradeItem> DIMENSIONAL_DEPOSIT_UPGRADE = ITEMS.register(
            "dimensional_deposit_upgrade",
            () -> new DimensionalDepositUpgradeItem(
                    net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedDepositUpgrade.filterSlots::get));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
