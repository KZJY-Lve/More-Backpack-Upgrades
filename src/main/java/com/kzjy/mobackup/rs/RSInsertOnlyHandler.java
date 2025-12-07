package com.kzjy.mobackup.rs;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * 仅插入的物品处理器
 * 将物品写入 RS 网络，并标记为玩家操作
 */
public class RSInsertOnlyHandler implements IItemHandler {
    private final INetwork network;
    private final Player player;

    public RSInsertOnlyHandler(INetwork network, Player player) {
        this.network = network;
        this.player = player;
    }

    @Override
    public int getSlots() { return 1; }

    @Override
    public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack remaining = network.insertItem(stack, stack.getCount(), simulate ? Action.SIMULATE : Action.PERFORM);
        if (!simulate && remaining.getCount() < stack.getCount()) {
            com.kzjy.mobackup.core.RSBridge.markItemInsertedByPlayer(network, player, stack);
        }
        return remaining;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }

    @Override
    public int getSlotLimit(int slot) { return 64; }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) { return true; }
}