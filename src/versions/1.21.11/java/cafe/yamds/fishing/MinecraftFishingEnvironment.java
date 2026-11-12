package cafe.yamds.fishing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * {@link FishingEnvironment} 的 Minecraft 实现（薄胶水层，无业务逻辑）。
 *
 * <p>1.21.11 使用 {@link LocalPlayer#displayClientMessage(Component, boolean)} 发送动作栏消息。
 */
public final class MinecraftFishingEnvironment implements FishingEnvironment {

    private final Minecraft minecraft;

    public MinecraftFishingEnvironment(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public boolean holdingRod() {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }
        return isFishingRod(player.getItemInHand(InteractionHand.MAIN_HAND))
                || isFishingRod(player.getItemInHand(InteractionHand.OFF_HAND));
    }

    @Override
    public boolean hasHook() {
        LocalPlayer player = minecraft.player;
        return player != null && player.fishing != null;
    }

    @Override
    public void castRod() {
        useItem();
    }

    @Override
    public void reelIn() {
        useItem();
    }

    @Override
    public void actionBar(String message) {
        LocalPlayer player = minecraft.player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), true);
        }
    }

    private void useItem() {
        if (minecraft.gameMode != null && minecraft.player != null) {
            minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
        }
    }

    private static boolean isFishingRod(ItemStack stack) {
        return stack.getItem() == Items.FISHING_ROD;
    }
}
