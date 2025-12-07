package com.kzjy.mobackup.mixin;

import com.kzjy.mobackup.rs.RSInsertOnlyHandler;
import com.kzjy.mobackup.wrapper.DimensionalDepositUpgradeWrapper;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.apiimpl.API;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IItemHandlerInteractionUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = BackpackItem.class, remap = false)
/**
 * 背包物品的交互拦截
 * 潜行右键 RS 控制器时，将卸货升级直接作用于 RS 网络
 */
public class BackpackItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void mobackup$preventPlaceIfDeposit(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }
        context.getItemInHand().getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).ifPresent(wrapper -> {
            IStorageWrapper storageWrapper = wrapper;
            List<IItemHandlerInteractionUpgrade> upgrades = storageWrapper.getUpgradeHandler()
                    .getWrappersThatImplement(IItemHandlerInteractionUpgrade.class);
            boolean hasDimensionalDeposit = upgrades.stream()
                    .anyMatch(u -> u instanceof DimensionalDepositUpgradeWrapper);
            if (!hasDimensionalDeposit) {
                return;
            }
            BlockPos pos = context.getClickedPos();
            var state = world.getBlockState(pos);
            var key = state.getBlock().builtInRegistryHolder().key();
            boolean isRSBlock = key != null && "refinedstorage".equals(key.location().getNamespace());
            if (isRSBlock && world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                INetwork rsNetwork = API.instance().getNetworkManager(serverLevel).getNetwork(pos);
                if (rsNetwork == null || !rsNetwork.canRun()) {
                    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                        rsNetwork = API.instance().getNetworkManager(serverLevel).getNetwork(pos.relative(dir));
                        if (rsNetwork != null && rsNetwork.canRun()) {
                            break;
                        }
                    }
                    if (rsNetwork == null || !rsNetwork.canRun()) {
                        outer: for (net.minecraft.core.Direction dir1 : net.minecraft.core.Direction.values()) {
                            for (net.minecraft.core.Direction dir2 : net.minecraft.core.Direction.values()) {
                                rsNetwork = API.instance().getNetworkManager(serverLevel)
                                        .getNetwork(pos.relative(dir1).relative(dir2));
                                if (rsNetwork != null && rsNetwork.canRun()) {
                                    break outer;
                                }
                            }
                        }
                    }
                }
                if (rsNetwork != null && rsNetwork.canRun()) {
                    final INetwork targetNet = rsNetwork;
                    upgrades.stream().filter(u -> u instanceof DimensionalDepositUpgradeWrapper).forEach(u -> {
                        u.onHandlerInteract(new RSInsertOnlyHandler(targetNet, player), player);
                    });
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    cir.cancel();
                    return;
                }
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("misc.refinedstorage.network_card.not_found"),
                        true);
                cir.setReturnValue(InteractionResult.SUCCESS);
                cir.cancel();
                return;
            }
            boolean interacted = net.p3pp3rf1y.sophisticatedbackpacks.util.InventoryInteractionHelper
                    .tryInventoryInteraction(context);
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        });
    }
}