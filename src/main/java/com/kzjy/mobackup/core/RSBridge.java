package com.kzjy.mobackup.core;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.apiimpl.API;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

/**
 * Refined Storage (RS) 模组的桥接工具类
 * 负责处理与 RS 网络的交互、坐标保存、跨维度获取网络实例等
 */
public class RSBridge {
    // 存储绑定坐标和维度的 NBT 键名
    private static final String NBT_RECEIVER_X = "ReceiverX";
    private static final String NBT_RECEIVER_Y = "ReceiverY";
    private static final String NBT_RECEIVER_Z = "ReceiverZ";
    private static final String NBT_DIMENSION = "Dimension";

    /**
     * 将目标方块坐标和维度保存到物品 NBT 中
     */
    public static void saveCoordinate(CompoundTag tag, BlockPos pos, ResourceKey<Level> dimension) {
        tag.putInt(NBT_RECEIVER_X, pos.getX());
        tag.putInt(NBT_RECEIVER_Y, pos.getY());
        tag.putInt(NBT_RECEIVER_Z, pos.getZ());
        tag.putString(NBT_DIMENSION, dimension.location().toString());
    }

    /**
     * 从物品 NBT 读取绑定的方块坐标
     * @return 如果 NBT 数据不完整则返回 null
     */
    @Nullable
    public static BlockPos getCoordinate(ItemStack stack) {
        if (stack.hasTag() &&
                stack.getTag().contains(NBT_RECEIVER_X) &&
                stack.getTag().contains(NBT_RECEIVER_Y) &&
                stack.getTag().contains(NBT_RECEIVER_Z)) {
            return new BlockPos(
                    stack.getTag().getInt(NBT_RECEIVER_X),
                    stack.getTag().getInt(NBT_RECEIVER_Y),
                    stack.getTag().getInt(NBT_RECEIVER_Z));
        }
        return null;
    }

    /**
     * 从物品 NBT 读取绑定的维度
     */
    @Nullable
    public static ResourceKey<Level> getDimension(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(NBT_DIMENSION)) {
            ResourceLocation name = ResourceLocation.tryParse(stack.getTag().getString(NBT_DIMENSION));
            if (name == null) {
                return null;
            }
            return ResourceKey.create(Registries.DIMENSION, name);
        }
        return null;
    }

    /**
     * 获取物品绑定的 RS 网络实例
     * 会检查维度、区块加载状态和网络有效性
     */
    @Nullable
    public static INetwork getNetwork(Level level, ItemStack stack) {
        if (level.isClientSide) {
            return null;
        }

        BlockPos pos = getCoordinate(stack);
        ResourceKey<Level> dim = getDimension(stack);

        if (pos == null || dim == null) {
            return null;
        }

        ServerLevel serverLevel = level.getServer().getLevel(dim);
        if (serverLevel == null) {
            return null;
        }

        // 确保目标区块已加载，否则不加载网络以免造成卡顿
        if (!serverLevel.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }

        return API.instance().getNetworkManager(serverLevel).getNetwork(pos);
    }

    /**
     * 标记物品插入操作是由特定玩家执行的
     * 用于 RS 的安全系统和审计日志
     * 使用反射调用 RS 内部 API，因为该功能未在公共接口中暴露
     */
    public static void markItemInsertedByPlayer(INetwork network, net.minecraft.world.entity.player.Player player,
            ItemStack stack) {
        try {
            // 获取物品存储追踪器
            Object tracker = network.getClass().getMethod("getItemStorageTracker").invoke(network);
            if (tracker == null) {
                return;
            }
            Class<?> tc = tracker.getClass();
            
            // 尝试从缓存获取 'changed' 方法
            java.lang.reflect.Method m = CHANGED_METHOD_CACHE.get(tc);
            Boolean playerFirst = CHANGED_METHOD_ORDER_PLAYER_FIRST.get(tc);
            
            // 如果未缓存，则扫描方法
            if (m == null || playerFirst == null) {
                for (var mm : tc.getMethods()) {
                    if (mm.getName().equals("changed")) {
                        Class<?>[] params = mm.getParameterTypes();
                        if (params.length == 2) {
                            // 匹配 (Player, ItemStack) 签名
                            if (params[0].isAssignableFrom(net.minecraft.world.entity.player.Player.class)
                                    && params[1].isAssignableFrom(ItemStack.class)) {
                                m = mm;
                                playerFirst = true;
                                break;
                            }
                            // 匹配 (ItemStack, Player) 签名（兼容不同版本）
                            if (params[0].isAssignableFrom(ItemStack.class)
                                    && params[1].isAssignableFrom(net.minecraft.world.entity.player.Player.class)) {
                                m = mm;
                                playerFirst = false;
                                break;
                            }
                        }
                    }
                }
                // 存入缓存
                if (m != null) {
                    m.setAccessible(true);
                    CHANGED_METHOD_CACHE.put(tc, m);
                    CHANGED_METHOD_ORDER_PLAYER_FIRST.put(tc, playerFirst);
                }
            }
            
            // 执行调用
            if (m != null) {
                if (playerFirst) {
                    m.invoke(tracker, player, stack);
                } else {
                    m.invoke(tracker, stack, player);
                }
            }
        } catch (Throwable ignored) {
            // 忽略反射错误，保证核心功能不崩溃
        }
    }

    // 反射方法缓存，避免重复查找带来的性能开销
    private static final java.util.Map<Class<?>, java.lang.reflect.Method> CHANGED_METHOD_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Class<?>, Boolean> CHANGED_METHOD_ORDER_PLAYER_FIRST = new java.util.concurrent.ConcurrentHashMap<>();
}
