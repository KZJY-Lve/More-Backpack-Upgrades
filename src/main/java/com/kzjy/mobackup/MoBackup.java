package com.kzjy.mobackup;

import com.kzjy.mobackup.registry.ModCreativeModeTabs;
import com.kzjy.mobackup.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import java.util.Objects;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilteredUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeTab;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod(MoBackup.MOD_ID)
public class MoBackup {
    public static final String MOD_ID = "mobackup";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MoBackup(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // 注册配置
        registerConfig();

        // 注册物品和创造模式标签
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        // 注册生命周期事件监听
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // 注册 Forge 事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 通用初始化阶段
     * 用于注册升级容器类型
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_MAGNET_UPGRADE.getId()),
                    DIMENSIONAL_MAGNET_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_PICKUP_UPGRADE.getId()),
                    DIMENSIONAL_PICKUP_TYPE);
            UpgradeContainerRegistry.register(Objects.requireNonNull(ModItems.DIMENSIONAL_DEPOSIT_UPGRADE.getId()),
                    DIMENSIONAL_DEPOSIT_TYPE);
        });
    }

    /**
     * 客户端初始化阶段
     * 用于注册 GUI 界面标签（如过滤设置页）
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册次元拾取升级的高级过滤界面
            UpgradeGuiManager.registerTab(DIMENSIONAL_PICKUP_TYPE, (
                    ContentsFilteredUpgradeContainer<PickupUpgradeWrapper> uc, Position p,
                    StorageScreenBase<?> s) -> new PickupUpgradeTab.Advanced(uc, p, s,
                            net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedPickupUpgrade.slotsInRow.get(),
                            SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));
            
            // 注册次元磁吸升级的高级过滤界面
            UpgradeGuiManager.registerTab(DIMENSIONAL_MAGNET_TYPE,
                    (MagnetUpgradeContainer uc, Position p, StorageScreenBase<?> s) -> new MagnetUpgradeTab.Advanced(uc,
                            p, s,
                            net.p3pp3rf1y.sophisticatedbackpacks.Config.SERVER.advancedMagnetUpgrade.slotsInRow.get(),
                            SBPButtonDefinitions.BACKPACK_CONTENTS_FILTER_TYPE));
            
            // 注册次元卸货升级的高级过滤界面
            UpgradeGuiManager.registerTab(DIMENSIONAL_DEPOSIT_TYPE, (
                    DepositUpgradeContainer uc, Position p,
                    StorageScreenBase<?> s) -> new DepositUpgradeTab.Advanced(uc, p, s));
        });
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.tryBuild(MOD_ID, path);
    }

    // 定义升级容器类型
    public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> DIMENSIONAL_PICKUP_TYPE = new UpgradeContainerType<>(
            ContentsFilteredUpgradeContainer::new);
    public static final UpgradeContainerType<net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeWrapper, MagnetUpgradeContainer> DIMENSIONAL_MAGNET_TYPE = new UpgradeContainerType<>(
            MagnetUpgradeContainer::new);
    public static final UpgradeContainerType<DepositUpgradeWrapper, DepositUpgradeContainer> DIMENSIONAL_DEPOSIT_TYPE = new UpgradeContainerType<>(
            DepositUpgradeContainer::new);

    @SuppressWarnings("removal")
    private void registerConfig() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_SPEC,
                "MoreBackpackUpgrades-common.toml");
    }

    /**
     * 玩家拾取物品前触发
     * 将玩家实例压入上下文，供升级逻辑使用
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPickupPre(EntityItemPickupEvent event) {
        com.kzjy.mobackup.core.PickupContext.push(event.getEntity());
    }

    /**
     * 玩家拾取物品后触发
     * 清理上下文，防止内存泄漏或逻辑污染
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPickupPost(EntityItemPickupEvent event) {
        com.kzjy.mobackup.core.PickupContext.pop();
    }
}
