package cafe.yamds.ui;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键绑定。默认：R 开关自动钓鱼，O 打开配置。
 *
 * <p>1.21.11 的 Fabric API 模块仍叫 key-binding，helper 类为 {@link KeyBindingHelper}。
 */
public final class KeyBindings {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autofishing", "autofishing"));

    public static KeyMapping toggleAutoFishing;
    public static KeyMapping openConfig;

    private KeyBindings() {
    }

    public static void register() {
        toggleAutoFishing = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autofishing.toggle",
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));

        openConfig = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autofishing.config",
                GLFW.GLFW_KEY_O,
                CATEGORY
        ));
    }
}
