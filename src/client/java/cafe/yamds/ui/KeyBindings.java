package cafe.yamds.ui;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键绑定。默认：R 开关自动钓鱼，O 打开配置。
 */
public final class KeyBindings {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("autofishing", "autofishing"));

    public static KeyMapping toggleAutoFishing;
    public static KeyMapping openConfig;

    private KeyBindings() {
    }

    public static void register() {
        toggleAutoFishing = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autofishing.toggle",
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));

        openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autofishing.config",
                GLFW.GLFW_KEY_O,
                CATEGORY
        ));
    }
}
