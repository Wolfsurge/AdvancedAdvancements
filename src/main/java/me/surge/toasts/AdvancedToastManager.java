package me.surge.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import me.surge.config.Config;
import me.surge.config.EntryAnimation;
import me.surge.mixins.IDrawContext;
import me.surge.duck.DDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author surge
 * @since 02/07/2023
 */
public class AdvancedToastManager {

    private static final Queue<AdvancedToast> toastQueue = new ConcurrentLinkedQueue<>();
    private static final Queue<Recipe<?>> recipesQueue = new ConcurrentLinkedQueue<>();

    public static void draw(DrawContext context, int width, int height) {
        AdvancedToast toast = toastQueue.peek();

        if (toast != null) {
            AdvancedToast.Data data = toast.draw(context, width, height);

            MatrixStack stack = context.getMatrices();

            ((IDrawContext) context).setMatrices(new MatrixStack());

            data.setFactor(MathHelper.clamp(data.getFactor(), 0.0001f, 1f));

            float mcScale = MinecraftClient.getInstance().options.getGuiScale().getValue();

            // scale animation
            {
                if (Config.ENTRY_ANIMATION.getValue().equals(EntryAnimation.SCALE)) {
                    context.getMatrices()
                           .translate(
                                   (data.getToastX() + (data.getToastWidth() / 2)) / mcScale,
                                   (data.getToastY() + (data.getToastHeight() / 2)) / mcScale,
                                   200.0
                           );

                    context.getMatrices().scale(data.getFactor(), data.getFactor(), data.getFactor());

                    context.getMatrices()
                           .translate(
                                   -((data.getToastX() + (data.getToastWidth() / 2)) / mcScale),
                                   -((data.getToastY() + (data.getToastHeight() / 2)) / mcScale),
                                   -200.0
                           );
                }
            }

            float scale = 1f / mcScale;
            context.getMatrices().scale(scale, scale, scale);

            float configScale = Config.SCALE.getValue();

            float offset = 16;

            if (toast.getBackground().equals(AdvancedToast.Background.GOAL)) {
                offset = 14;
            }

            offset *= configScale;

            // scale to normal size
            {
                context.getMatrices().translate(
                        ((int) (data.getToastX() + offset)),
                        ((data.getToastY() + (16 * configScale))),
                        200.0
                );

                context.getMatrices().scale(2, 2, 2);

                context.getMatrices().translate(
                        -(((data.getToastX() + offset))),
                        -(((data.getToastY() + (16 * configScale)))),
                        -200.0
                );
            }

            // scale to config size
            {
                context.getMatrices().translate(
                        ((int) (data.getToastX() + offset)),
                        ((data.getToastY() + (16 * configScale))),
                        200.0
                );

                context.getMatrices().scale(configScale, configScale, configScale);

                context.getMatrices().translate(
                        -(((data.getToastX() + offset))),
                        -(((data.getToastY() + (16 * configScale)))),
                        -200.0
                );
            }

            RenderSystem.setShaderColor(1f, 1f, 1f, Config.ENTRY_ANIMATION.getValue().equals(EntryAnimation.SCALE) ? data.getFactor() : 1);

            ((DDrawContext) context).drawItemWithoutEntityF(toast.getIcon(), (int) (data.getToastX() + offset), (int) (data.getToastY() + (16 * (Config.ENTRY_ANIMATION.getValue().equals(EntryAnimation.SCALE) ? configScale : 1))));

            ((IDrawContext) context).setMatrices(stack);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();

            if (toast.finished()) {
                toastQueue.poll();
            }
        }

        if (!recipesQueue.isEmpty()) {
            AdvancedRecipeToast recipeToast = new AdvancedRecipeToast();

            for (Recipe<?> recipe = recipesQueue.poll(); recipe != null; recipe = recipesQueue.poll()) {
                recipeToast.add(recipe);
            }

            toastQueue.add(recipeToast);
        }
    }

    public static void add(AdvancedToast toast) {
        toastQueue.add(toast);
    }

    public static void queueRecipe(List<Recipe<?>> recipes) {
        recipesQueue.addAll(recipes);
    }

}
