package com.kzjy.mobackup.wrapper;

import com.kzjy.mobackup.core.RSBridge;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import com.kzjy.mobackup.item.DimensionalMagnetUpgradeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;

import java.util.function.Consumer;

/**
 * 次元拾取升级的逻辑实现
 * 拾取到的物品优先推送到 RS 网络，必要时回退到背包
 */
public class DimensionalPickupUpgradeWrapper extends PickupUpgradeWrapper {

    public DimensionalPickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
            Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public ItemStack pickup(Level world, ItemStack stack, boolean simulate) {
        if (!getFilterLogic().matchesFilter(stack)) {
            return stack;
        }
        // 背包已装磁吸升级时，避免在拾取升级重复路由到 RS
        if (storageWrapper.getUpgradeHandler().hasUpgrade(DimensionalMagnetUpgradeItem.TYPE)) {
            return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
        }

        INetwork network = getCachedNetwork(world);
        if (network != null && network.canRun()) {
            ItemStack original = stack.copy();
            ItemStack remaining = network.insertItem(original, original.getCount(),
                    simulate ? Action.SIMULATE : Action.PERFORM);
            if (!simulate) {
                Player ctx = com.kzjy.mobackup.core.PickupContext.current();
                if (ctx != null && remaining.getCount() < original.getCount()) {
                    com.kzjy.mobackup.core.RSBridge.markItemInsertedByPlayer(network, ctx, original);
                }
            }
            // 若已部分/全部插入 RS，返回剩余
            if (remaining.getCount() < stack.getCount()) {
                return remaining;
            }
        }

        return storageWrapper.getInventoryForUpgradeProcessing().insertItem(stack, simulate);
    }

    private INetwork cachedNetwork;
    private long lastNetworkCheckTime = -1;
    private static final int NETWORK_CHECK_INTERVAL = 20;

    private INetwork getCachedNetwork(Level level) {
        long gameTime = level.getGameTime();
        if (cachedNetwork == null || !cachedNetwork.canRun() || lastNetworkCheckTime < 0
                || gameTime - lastNetworkCheckTime >= NETWORK_CHECK_INTERVAL) {
            lastNetworkCheckTime = gameTime;
            cachedNetwork = RSBridge.getNetwork(level, upgrade);
        }
        return cachedNetwork;
    }
}